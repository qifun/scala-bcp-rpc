/*
 * scala-bcp-rpc
 * Copyright 2014 深圳岂凡网络有限公司 (Shenzhen QiFun Network Corp., LTD)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qifun.bcp.rpc

import com.qifun.bcp.BcpServer
import scala.reflect.ClassTag
import com.qifun.statelessFuture.util.Generator
import java.nio.ByteBuffer
import scala.util.control.Exception.Catcher
import com.qifun.bcp.BcpSession
import scala.runtime.BoxedUnit
import scala.reflect.macros.Context
import net.sandrogrzicic.scalabuff.Message
import com.qifun.statelessFuture.Future
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import com.google.protobuf.GeneratedMessageLite
import java.lang.reflect.InvocationTargetException

object RpcSession {

  private implicit val (logger, formatter, appender) = ZeroLoggerFactory.newLogger(this)

  final case class IncomingProxyEntry[Service <: IService](module: String, incomingService: Service)

  object IncomingProxyRegistration {
    final def apply[Service <: IService](incomingEntries: IncomingProxyEntry[Service]*) = {
      val map = (for {
        entry <- incomingEntries
      } yield {
        entry.module -> entry.incomingService
      })(collection.breakOut(Map.canBuildFrom))
      new IncomingProxyRegistration(map)
    }
  }

  final class IncomingProxyRegistration private (val incomingProxyMap: Map[String, IService])
    extends AnyVal // Do not extends AnyVal because of https://issues.scala-lang.org/browse/SI-8702

  private[rpc] val REQUEST = 0
  private[rpc] val SUCCESS = 1
  private[rpc] val FAIL = 2
  private[rpc] val EVENT = 3
  private[rpc] val INFO = 4
  private[rpc] val CASTREQUEST = 5

}

/**
 * data format:
 * 32bit(Inc) + 8bit(Type) + 8bit(NameSize) + 32bit(MessageSize) + raw_byte(MessageName) + raw_byte(Protobuf)
 * Inc: Incremental id
 * Type: 0: REQUEST; 1: SUCCESS; 2: FAIL; 3: EVENT; 4: INFO;
 * NameSize: The size of MessageName
 * MessageSize: The size of Protobuf
 * raw_byte(MessageName): The name of the Protobuf message
 * raw_byte(Protobuf): The Protobuf content
 * 
 * TODO Send the hash of the name, which can save network flow.
 * 
 */
trait RpcSession { _: BcpSession[_, _] =>

  import RpcSession._

  protected def incomingServices: RpcSession.IncomingProxyRegistration

  private val nextMessageId = new AtomicInteger(0)

  private val outgoingRpcResponseHandlers = TrieMap.empty[Int, IResponseHandler]

  final val outgoingProxy = new OutgoingProxy

  final class OutgoingProxy {

    final def handleReques[M <: GeneratedMessageLite](request: GeneratedMessageLite)(
      successCallback: M => Unit,
      failCallback: GeneratedMessageLite => Unit) = {
      val messageId = nextMessageId.getAndIncrement()
      val responseHandler = new IResponseHandler {
        final override def onSuccess(message: GeneratedMessageLite): Unit = successCallback(message.asInstanceOf[M])

        final override def onFailure(message: GeneratedMessageLite): Unit = failCallback(message)
      }
      outgoingRpcResponseHandlers.putIfAbsent(messageId, responseHandler) match {
        case None => {
          sendMessage(RpcSession.REQUEST, messageId, request)
        }
        case Some(oldFunction) => {
          throw new IllegalStateException("")
        }
      }
    }

    final def handleEvent(event: GeneratedMessageLite): Unit = {
      val messageId = nextMessageId.getAndIncrement()
      sendMessage(RpcSession.EVENT, messageId, event)
    }
    
    final def handleInfo(info: GeneratedMessageLite): Unit = {
      val messageId = nextMessageId.getAndIncrement()
      sendMessage(RpcSession.INFO, messageId, info) 
    }
    
    final def handleCastRequest(castRequest: GeneratedMessageLite): Unit = {
      val messageId = nextMessageId.getAndIncrement()
      sendMessage(RpcSession.CASTREQUEST, messageId, castRequest)
    }
  }

  private final def sendMessage(messageType: Int, messageId: Int, message: GeneratedMessageLite): Unit = {
    val messageName = message.getClass.getName
    val nameSize = messageName.size
    val messageByteArray = message.toByteArray
    val messageSize = messageByteArray.length
    val byteBuffer = ByteBuffer.allocate(10 + nameSize + messageSize)
    // TODO Can use Generator ?
    byteBuffer.putInt(messageId)
    byteBuffer.put(messageType.toByte)
    byteBuffer.put(nameSize.toByte)
    byteBuffer.putInt(messageSize)
    byteBuffer.put(messageName.getBytes)
    byteBuffer.put(messageByteArray)
    byteBuffer.flip()
    send(byteBuffer)
  }

  override protected final def received(buffers: java.nio.ByteBuffer*): Unit = {
    // TODO Can use Generator ?
    val byteBufferInput = new ByteBufferInput(buffers.iterator)
    val messageId = byteBufferInput.readInt()
    val messageType = byteBufferInput.readByte()
    val messageNameSize = byteBufferInput.readByte()
    val messageSize = byteBufferInput.readInt()
    val messageNameBytes = ByteBuffer.allocate(messageNameSize)
    byteBufferInput.readBytes(messageNameBytes, 0, messageNameSize)
    val messageName = new String(messageNameBytes.array, "UTF-8")
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    val module = runtimeMirror.staticModule(messageName)
    val messageObject = runtimeMirror.reflectModule(module).instance
    val message = 
      if(messageSize > 0) {
        val messageByte = ByteBuffer.allocate(messageSize)
        byteBufferInput.readBytes(messageByte, 0, messageSize)
        // Parse message reflectively
        val parseFrom = messageObject.getClass.getMethod("parseFrom", classOf[Array[Byte]])
        parseFrom.invoke(messageObject, messageByte.array).asInstanceOf[GeneratedMessageLite]
      }
      else {
        val newBuilder = messageObject.getClass.getMethod("newBuilder")
        newBuilder.invoke(messageObject).asInstanceOf[GeneratedMessageLite]
      }
    val packageName = message.getClass.getPackage.getName
    messageType match {
      case RpcSession.REQUEST => {
        incomingServices.incomingProxyMap.get(packageName) match {
          case None => {
            logger.severe("Unknown service name: " + messageName)
            interrupt()
          }
          case Some(service) => {
            val handleRequest = service.getClass.getMethod("handleRequest", message.getClass)
            try {
              val responseMessage = handleRequest.invoke(service, message).asInstanceOf[GeneratedMessageLite]
              sendMessage(RpcSession.SUCCESS, messageId, responseMessage)
            } catch {
              case exception: InvocationTargetException =>
                exception.getCause match {
                  case errorCode: ErrorCode =>
                    sendMessage(RpcSession.FAIL, messageId, errorCode.errorMessage)
                  case e: Exception =>
                    logger.severe("Fail: " + exception)
                    interrupt()
                }
              case exception: Exception =>
                logger.severe("Fail: " + exception)
                interrupt()
            }
          }
        }
      }
      case RpcSession.CASTREQUEST => {
        incomingServices.incomingProxyMap.get(packageName) match {
          case None => {
            logger.severe("Unknown service name: " + messageName)
            interrupt()
          }
          case Some(service) => {
            val handleCastRequest = service.getClass.getMethod("handleCastRequest", message.getClass)
            handleCastRequest.invoke(service, message)
          }
        }
      }
      case RpcSession.EVENT => {
        incomingServices.incomingProxyMap.get(packageName) match {
          case None => {
            logger.severe("Unknown service name: " + messageName)
            interrupt()
          }
          case Some(service) => {
            val handleEvent = service.getClass.getMethod("handleEvent", message.getClass)
            handleEvent.invoke(service, message)
          }
        }
      }
      case RpcSession.INFO => {
        incomingServices.incomingProxyMap.get(packageName) match {
          case None => {
            logger.severe("Unknown service name: " + messageName)
            interrupt()
          }
          case Some(service) => {
            val handleInfo = service.getClass.getMethod("handleInfo", message.getClass)
            handleInfo.invoke(service, message)
          }
        }
      }
      case RpcSession.SUCCESS => {
        outgoingRpcResponseHandlers.remove(messageId) match {
          case None => {
            logger.severe(this + " Illegal rpc data: " + messageName)
            interrupt()
          }
          case Some(handler) => {
            handler.onSuccess(message)
          }
        }
      }
      case RpcSession.FAIL => {
        outgoingRpcResponseHandlers.remove(messageId) match {
          case None => {
            logger.severe(this + " Illegal rpc data: " + messageName)
            interrupt()
          }
          case Some(handler) => {
            handler.onFailure(message)
          }
        }
      }
    }
  }

} 
