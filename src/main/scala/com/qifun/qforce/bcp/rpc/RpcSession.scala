package com.qifun.qforce.bcp.rpc

import haxe.lang.Function
import com.qifun.qforce.bcp.BcpServer
import com.qifun.jsonStream.rpc.OutgoingProxy
import com.qifun.jsonStream.rpc.IncomingProxy
import scala.reflect.classTag
import scala.reflect.ClassTag
import com.qifun.jsonStream.JsonStream
import java.util.concurrent.atomic.AtomicInteger
import com.qifun.statelessFuture.util.Generator
import com.dongxiguo.continuation.utils.{ Generator => HaxeGenerator }
import java.nio.ByteBuffer
import scala.collection.concurrent.TrieMap
import com.qifun.jsonStream.JsonStreamPair
import com.qifun.qforce.bcp.BcpSession

object RpcSession {

  trait IncomingProxyImpl[ServiceInterface] extends IncomingProxy[ServiceInterface] { _: ServiceInterface =>

    override final def get_service: this.type = this

    // 由json-stream实现
    def incomingRpc(request: JsonStream, handler: Function): Unit

  }

  object IncomingProxyEntry {

    import language.implicitConversions

    implicit def apply[ServiceInterface: ClassTag, S <: RpcSession](
      newProxyFunction: S => IncomingProxyImpl[ServiceInterface]): IncomingProxyEntry[S] = {
      new IncomingProxyEntry(classTag[ServiceInterface].toString, newProxyFunction)
    }

  }

  final class IncomingProxyEntry[S <: RpcSession] private (
    val pair: (String, S => IncomingProxyImpl[_])) extends AnyVal

  object IncomingProxyFactory {

    final def apply[S <: RpcSession](incomingEntries: IncomingProxyEntry[S]*) = {
      new IncomingProxyFactory(
        (for (entry <- incomingEntries) yield entry.pair)(collection.breakOut(Map.canBuildFrom)))
    }
  }

  final class IncomingProxyFactory[S <: RpcSession] private (
    private[RpcSession] val incomingProxyMap: Map[String, S => IncomingProxyImpl[_]])
  // extends AnyVal // Do not extends AnyVal because of https://issues.scala-lang.org/browse/SI-8702

  private def generator1[Element](element: Element) = {
    new HaxeGenerator[Element](
      new haxe.lang.Function(2, 0) {
        override final def __hx_invoke2_o(
          argumentValue0: Double, argumentRef0: AnyRef,
          argumentValue1: Double, argumentRef1: AnyRef) = {
          val yieldFunction = argumentRef0.asInstanceOf[haxe.lang.Function]
          val returnFunction = argumentRef1.asInstanceOf[haxe.lang.Function]
          yieldFunction.__hx_invoke2_o(0, element, 0, returnFunction)
        }
      })
  }
}

trait RpcSession { _: BcpSession[_, _] =>

  import RpcSession.generator1

  protected def incomingProxyFactory: RpcSession.IncomingProxyFactory[_ >: this.type]

  private val nextRequestId = new AtomicInteger(0)

  private val outgoingRpcResponseHandlers = TrieMap.empty[Int, Function]

  protected def toByteBuffer(js: JsonStream): Seq[ByteBuffer]

  protected def toJsonStream(buffers: java.nio.ByteBuffer*): JsonStream

  final class OutgoingProxyEntry[ServiceInterface: ClassTag] private[RpcSession] {
    trait OutgoingProxyImpl { _: OutgoingProxy[ServiceInterface] =>
      override protected final def outgoingRpc(i: JsonStream, o: Function): Unit = {
        RpcSession.this.outgoingRpc[ServiceInterface](i, o)
      }
    }
  }

  final def send[ServiceInterface: ClassTag] = new OutgoingProxyEntry[ServiceInterface]

  private final def outgoingRpc[ServiceInterface: ClassTag](request: JsonStream, handler: Function): Unit = {
    val requestId = nextRequestId.getAndIncrement()
    outgoingRpcResponseHandlers.putIfAbsent(requestId, handler) match {
      case None => {
        val requestStream = JsonStream.OBJECT(generator1(new JsonStreamPair(
          "request",
          JsonStream.OBJECT(generator1(new JsonStreamPair(
            requestId.toString,
            JsonStream.OBJECT(generator1(new JsonStreamPair(
              classTag[ServiceInterface].toString,
              request)))))))))
        send(toByteBuffer(request): _*)
      }
      case Some(oldFunction) => {
        throw new IllegalStateException("")
      }
    }
  }

  private final class SendResponse(requestId: String) extends haxe.lang.Function(1, 0) {
    override final def __hx_invoke1_o(
      argumentValue0: Double, argumentRef0: AnyRef) = {
      val responseBody = argumentRef0.asInstanceOf[JsonStream]
      val responseStream = JsonStream.OBJECT(generator1(new JsonStreamPair(
        "response",
        JsonStream.OBJECT(generator1(new JsonStreamPair(
          requestId.toString,
          responseBody))))))
      send(toByteBuffer(responseStream): _*)
      null
    }
  }

  override protected final def received(buffers: java.nio.ByteBuffer*): Unit = {
    toJsonStream(buffers: _*) match {
      case JsonStreamExtractor.Object(requestOrResponsePairs) => {
        for (requestOrResponsePair <- requestOrResponsePairs) {
          requestOrResponsePair.key match {
            case "request" => {
              requestOrResponsePair.value match {
                case JsonStreamExtractor.Object(idPairs) => {
                  for (idPair <- idPairs) {
                    val id = idPair.key
                    idPair.value match {
                      case JsonStreamExtractor.Object(servicePairs) => {
                        for (servicePair <- servicePairs) {
                          incomingProxyFactory.incomingProxyMap.get(servicePair.key) match {
                            case None => {
                              throw new RpcException.UnknownServiceName
                            }
                            case Some(newProxyFunction) => {
                              newProxyFunction(this).incomingRpc(servicePair.value, new SendResponse(id))
                            }
                          }
                        }
                      }
                      case _ => {
                        throw new RpcException.IllegalRpcData
                      }
                    }
                  }
                }
                case _ => {
                  throw new RpcException.IllegalRpcData
                }
              }
            }
            case "response" => {
              requestOrResponsePair.value match {
                case JsonStreamExtractor.Object(idPairs) => {
                  for (idPair <- idPairs) {
                    val id = try {
                      idPair.key.toInt
                    } catch {
                      case e: NumberFormatException => {
                        throw new RpcException.IllegalRpcData(cause = e)
                      }
                    }
                    outgoingRpcResponseHandlers.remove(id) match {
                      case None => {
                        throw new RpcException.IllegalRpcData
                      }
                      case Some(handler) => {
                        handler.__hx_invoke1_o(0.0, idPair.value)
                      }
                    }
                  }
                }
                case _ => {
                  throw new RpcException.IllegalRpcData
                }
              }
            }
            case _ => {
              throw new RpcException.IllegalRpcData
            }
          }
        }
      }
      case _ => {
        throw new RpcException.IllegalRpcData
      }
    }
  }

} 