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
import com.google.protobuf.GeneratedMessageLite
import java.lang.reflect.InvocationTargetException
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.TypeTag

object RpcSession {

  private implicit val (logger, formatter, appender) = ZeroLoggerFactory.newLogger(this)

  final case class IncomingProxyEntry(module: String, incomingService: RpcService)
  
  private[rpc] trait InternalErrorCode {
    val errorMessage: GeneratedMessageLite
    val errorTag: TypeTag[GeneratedMessageLite]
  }
  
  final case class ErrorCodeEntry[TMessage <: GeneratedMessageLite](errorCode: TMessage)
    (implicit tag: TypeTag[TMessage]) extends InternalErrorCode {
    override final val errorMessage = errorCode
    override final val errorTag = tag.asInstanceOf[TypeTag[GeneratedMessageLite]]
  }
  
  object IncomingProxyRegistration {
    final def apply(incomingEntries: IncomingProxyEntry*) = {
      val map = (for {
        entry <- incomingEntries
      } yield {
        entry.module -> entry.incomingService
      })(collection.breakOut(Map.canBuildFrom))
      new IncomingProxyRegistration(map)
    }
  }

  final class IncomingProxyRegistration private (val incomingProxyMap: Map[String, RpcService])
    extends AnyVal // Do not extends AnyVal because of https://issues.scala-lang.org/browse/SI-8702
    
  object ErrorCodeRegistration {
    final def apply(errorCodes: InternalErrorCode*) = {
      val map = (for {
        errorCode <- errorCodes
      } yield {
        errorCode.errorMessage.getClass.getName -> errorCode
      })(collection.breakOut(Map.canBuildFrom))
      new ErrorCodeRegistration(map)
    }
  }
    
  final class ErrorCodeRegistration private (val errorCodesMap: Map[String, InternalErrorCode])
    extends AnyVal

}

/**
 * Data format:
 * 32bit(Inc) + 8bit(Type) + 8bit(NameSize) + 32bit(MessageSize) + raw_byte(MessageName) + raw_byte(Protobuf)
 * Inc: Incremental id
 * Type: 0: REQUEST; 1: SUCCESS; 2: FAIL; 3: PUSHMESSAGE;
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
  import RpcService._

  protected def incomingServices: RpcSession.IncomingProxyRegistration
  
  protected def errorCodes: RpcSession.ErrorCodeRegistration

  private val nextMessageId = new AtomicInteger(0)

  private val outgoingRpcResponseHandlers = TrieMap.empty[Int, IResponseHandler]

  final val outgoingProxy = new OutgoingProxy

  final class OutgoingProxy {

    final def sendRequest[M <: GeneratedMessageLite](request: GeneratedMessageLite)(
      successCallback: M => Unit,
      failCallback: GeneratedMessageLite => Unit)(implicit responseTag: TypeTag[M]): Unit = {
      val handleRequestFuture = Future {
        val messageId = nextMessageId.getAndIncrement()
        val responseHandler = new IResponseHandler {
          
          final override def responseType = responseTag.asInstanceOf[TypeTag[GeneratedMessageLite]]
          
          final override def onSuccess(message: GeneratedMessageLite): Unit = successCallback(message.asInstanceOf[M])

          final override def onFailure(message: GeneratedMessageLite): Unit = failCallback(message)
        }
        outgoingRpcResponseHandlers.putIfAbsent(messageId, responseHandler) match {
          case None => {
            sendMessage(BcpRpc.REQUEST, messageId, request.getClass.getName,request.toByteArray)
          }
          case Some(oldFunction) => {
            throw new IllegalStateException("")
          }
        }
      }
      implicit def catcher: Catcher[Unit] = {
        case exception: Exception => {
          logger.severe("Handle request failed: " + exception.printStackTrace)
          interrupt()
        }
      }
      for(_ <- handleRequestFuture) {}
    }
    
    final def blockingSendRequest[M <: GeneratedMessageLite](request: GeneratedMessageLite)(
      successCallback: M => Unit,
      failCallback: GeneratedMessageLite => Unit)(implicit responseTag: TypeTag[M]): Unit = {
      val lock = new AnyRef
      @volatile var isReceived: Option[Boolean] = Some(false)
      sendRequest(request)((message: M) => {
        lock.synchronized {
          isReceived = Some(true)
          successCallback(message)
          lock.notify()
        }
      }, (message: GeneratedMessageLite) => {
        lock.synchronized {
          isReceived = Some(true)
          failCallback(message)
          lock.notify()
        }
      })
      lock.synchronized {
        while(isReceived == Some(false)) {
          lock.wait()
        }
      }
    }

    final def pushMessage(event: GeneratedMessageLite): Unit = {
      val handleEventFuture = Future {
        val messageId = nextMessageId.getAndIncrement()
        sendMessage(BcpRpc.PUSHMESSAGE, messageId, event.getClass.getName,event.toByteArray)
      }
      implicit def catcher: Catcher[Unit] = {
        case exception: Exception => {
          logger.severe("Handle event failed: " + exception.printStackTrace)
          interrupt()
        }
      }
      for(_ <- handleEventFuture) {}
    }
    
    final def pushMessage(messageName: String,messageByteArray: Array[Byte]): Unit = {
      val handleEventFuture = Future {
        val messageId = nextMessageId.getAndIncrement()
        sendMessage(BcpRpc.PUSHMESSAGE, messageId, messageName,messageByteArray)
      }
      implicit def catcher: Catcher[Unit] = {
        case exception: Exception => {
          logger.severe("Handle event failed: " + exception.printStackTrace)
          interrupt()
        }
      }
      for(_ <- handleEventFuture) {}
    }
    
  }
  
  private final def sendMessage(messageType: Int, messageId: Int, messageName: String, messageByteArray: Array[Byte]):Unit = {
    val nameSize = messageName.size
    val messageSize = messageByteArray.length
    val byteBuffer = ByteBuffer.allocate(10 + nameSize + messageSize)
    byteBuffer.putInt(messageId)
    byteBuffer.put(messageType.toByte)
    byteBuffer.put(nameSize.toByte)
    byteBuffer.putInt(messageSize)
    byteBuffer.put(messageName.getBytes)
    byteBuffer.put(messageByteArray)
    byteBuffer.flip()
    send(byteBuffer)
  }
  
  private def bytesToMessage(
      byteBufferInput: ByteBufferInput,
      messageType: TypeTag[GeneratedMessageLite], 
      messageSize: Int) = {
      val universeMirror = universe.runtimeMirror(getClass.getClassLoader)
      val messageObject = universeMirror.reflectModule(messageType.tpe.typeSymbol.companionSymbol.asModule).instance
      if(messageSize > 0) {
        val messageByte = ByteBuffer.allocate(messageSize)
        byteBufferInput.readBytes(messageByte, 0, messageSize)
        val parseFrom = messageObject.getClass.getMethod("parseFrom", classOf[Array[Byte]])
        parseFrom.invoke(messageObject, messageByte.array).asInstanceOf[GeneratedMessageLite]
      } else {
        val newBuilder = messageObject.getClass.getMethod("newBuilder")
        newBuilder.invoke(messageObject).asInstanceOf[GeneratedMessageLite]
     }
  }

  override protected final def received(buffers: java.nio.ByteBuffer*): Unit = {
    // TODO Can use Generator ?
    val receivedFuture = Future {
      val byteBufferInput = new ByteBufferInput(buffers.iterator)
      val messageId = byteBufferInput.readInt()
      val messageType = byteBufferInput.readByte()
      val messageNameSize = byteBufferInput.readByte()
      val messageSize = byteBufferInput.readInt()
      val messageNameBytes = ByteBuffer.allocate(messageNameSize)
      byteBufferInput.readBytes(messageNameBytes, 0, messageNameSize)
      val messageName = new String(messageNameBytes.array, "UTF-8")
      val packageName = messageName.substring(0, messageName.lastIndexOf("."))
      messageType match {
        case BcpRpc.REQUEST => {
          incomingServices.incomingProxyMap.get(packageName) match {
            case None => {
              logger.severe("Unknown service name: " + messageName)
              interrupt()
            }
            case Some(service) => {
              service.incomingMessages.incomingMessagesMap.get(messageName) match {
                case None => {
                  logger.severe("Unknown message name: " + messageName)
                  interrupt()
                }
                case Some(messageEntry: IncomingEntry) => {
                  val message = bytesToMessage(byteBufferInput, messageEntry.messageType, messageSize)
                  try {
                    val responseMessage = messageEntry.executeRequest(message, this)
                    sendMessage(BcpRpc.SUCCESS, messageId, responseMessage.getClass.getName,responseMessage.toByteArray)
                  } catch {
                    case errorCode: ErrorCode[_] =>
                      sendMessage(BcpRpc.FAIL, messageId, errorCode.errorMessage.getClass.getName,errorCode.errorMessage.toByteArray)
                    case exception: Exception =>
                      logger.severe("Handle request exception: " + exception.printStackTrace)
                  }
                }
              }
            }
          }
        }
        case BcpRpc.PUSHMESSAGE => {
          incomingServices.incomingProxyMap.get(packageName) match {
            case None => {
              logger.severe("Unknown service name: " + messageName)
              interrupt()
            }
            case Some(service) => {
              service.incomingMessages.incomingMessagesMap.get(messageName) match {
                case None => {
                  logger.severe("Unknown message name: " + messageName)
                  interrupt()
                }
                case Some(messageEntry: IncomingEntry) => {
                  val message = bytesToMessage(byteBufferInput, messageEntry.messageType, messageSize)
                  messageEntry.executeMessage(message, this)
                }
              }
            }
          }
        }
        case BcpRpc.SUCCESS => {
          outgoingRpcResponseHandlers.remove(messageId) match {
            case None => {
              logger.severe(this + " Illegal rpc data: " + messageName)
              interrupt()
            }
            case Some(handler) => {
              val message = bytesToMessage(byteBufferInput, handler.responseType, messageSize)
              handler.onSuccess(message)
            }
          }
        }
        case BcpRpc.FAIL => {
          outgoingRpcResponseHandlers.remove(messageId) match {
            case None => {
              logger.severe(this + " Illegal rpc data: " + messageName)
              interrupt()
            }
            case Some(handler) => {
              errorCodes.errorCodesMap.get(messageName) match {
                case None => {
                  logger.severe("Unknown ErrorCode name: " + messageName)
                  interrupt()
                }
                case Some(errorCode) =>
                  val error = bytesToMessage(byteBufferInput, errorCode.errorTag, messageSize)
                  handler.onFailure(error)
              }
            }
          }
        }
      }
    }
    implicit def catcher: Catcher[Unit] = {
      case exception: Exception => {
        logger.severe("Handle received failed: " + exception.printStackTrace())
        interrupt()
      }
    }
    for(_ <- receivedFuture) {}
  }

} 
