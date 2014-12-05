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

import java.nio.ByteBuffer
import com.qifun.jsonStream.JsonStream
import com.qifun.jsonStream.io.PrettyTextPrinter
import com.qifun.jsonStream.io.TextParser
import com.qifun.bcp.BcpSession
import com.dongxiguo.continuation.utils.{ Generator => HaxeGenerator }
import com.qifun.jsonStream.rpc.IJsonService
import com.qifun.jsonStream.JsonStreamPair
import com.qifun.jsonStream.rpc.IJsonResponseHandler
import scala.collection.concurrent.TrieMap
import java.util.concurrent.atomic.AtomicInteger

private object TextSession {

  private implicit val (logger, formatter, appender) = ZeroLoggerFactory.newLogger(this)

  private final def generator1[Element](element: Element) = {
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

/**
 * 收到的包都是JSON格式的数据，有以下四种格式的包：
 *   1. 请求
 *     {
 *         "request": {
 *             "123": { //请求Id
 *                 "myPacakge.IMyInterface": {
 *                     // 交给Haxe处理的数据
 *                 }
 *             }
 *         }
 *     }
 *
 *   2. 失败回应
 *     {
 *         "failure": {
 *             "123": { // 失败的请求Id
 *                 // 交给Haxe处理的数据
 *             }
 *         }
 *     }
 *
 *   3. 成功回应
 *     {
 *         "success": {
 *             "123": { // 失败的请求Id
 *                 // 交给Haxe处理的数据
 *             }
 *         }
 *     }
 *     
 *   4. 单向推送数据
 *     {
 *         "push": {
 *             "myPacakge.IMyInterface": {
 *                 // 交给Haxe处理的数据
 *             }
 *         }
 *     }
 *
 */
trait TextSession extends RpcSession { _: BcpSession[_, _] =>

  import TextSession._

  private val nextRequestId = new AtomicInteger(0)

  private val outgoingRpcResponseHandlers = TrieMap.empty[Int, IJsonResponseHandler]

  private def toByteBuffer(js: JsonStream): Seq[ByteBuffer] = {
    val output = new ByteBufferOutput
    PrettyTextPrinter.print(output, js, 0)
    output.result()
  }

  private def toJsonStream(buffers: java.nio.ByteBuffer*): JsonStream = {
    TextParser.parseInput(new ByteBufferInput(buffers.iterator))
  }

  final def outgoingService[ServiceInterface](
    implicit entry: RpcSession.OutgoingProxyEntry[ServiceInterface]): ServiceInterface = {

    val serviceClassName = entry.serviceTag.toString

    entry.outgoingView(new IJsonService {

      override final def push(data: JsonStream): Unit = {
        val pushStream = JsonStream.OBJECT(generator1(new JsonStreamPair(
          "push",
          JsonStream.OBJECT(generator1(new JsonStreamPair(serviceClassName, data))))))
        send(toByteBuffer(pushStream): _*)
      }

      override final def apply(request: JsonStream, handler: IJsonResponseHandler): Unit = {
        val requestId = nextRequestId.getAndIncrement()
        outgoingRpcResponseHandlers.putIfAbsent(requestId, handler) match {
          case None => {
            val requestStream = JsonStream.OBJECT(generator1(new JsonStreamPair(
              "request",
              JsonStream.OBJECT(generator1(new JsonStreamPair(
                requestId.toString,
                JsonStream.OBJECT(generator1(new JsonStreamPair(serviceClassName, request)))))))))
            send(toByteBuffer(requestStream): _*)
          }
          case Some(oldFunction) => {
            throw new IllegalStateException("")
          }
        }
      }
    })

  }

  override protected final def received(buffers: java.nio.ByteBuffer*): Unit = {
    toJsonStream(buffers: _*) match {
      case JsonStreamExtractor.Object(requestOrResponsePairs) => {
        for (requestOrResponsePair <- requestOrResponsePairs) {
          requestOrResponsePair.key match {
            case "push" => {
              requestOrResponsePair.value match {
                case JsonStreamExtractor.Object(servicePairs) => {
                  for (servicePair <- servicePairs) {
                    incomingServices.incomingProxyMap.get(servicePair.key) match {
                      case None => {
                        logger.severe("Unknown service name")
                        interrupt()
                      }
                      case Some(incomingRpc) => {
                        incomingRpc(this).push(servicePair.value)
                      }
                    }
                  }
                }
                case _ => {
                  logger.severe("Illegal rpc data!")
                  interrupt()
                }
              }
            }
            case "request" => {
              requestOrResponsePair.value match {
                case JsonStreamExtractor.Object(idPairs) => {
                  for (idPair <- idPairs) {
                    val id = idPair.key
                    idPair.value match {
                      case JsonStreamExtractor.Object(servicePairs) => {
                        for (servicePair <- servicePairs) {
                          incomingServices.incomingProxyMap.get(servicePair.key) match {
                            case None => {
                              logger.severe("Unknown service name")
                              interrupt()
                            }
                            case Some(incomingRpc) => {
                              incomingRpc(this).apply(
                                servicePair.value,
                                new IJsonResponseHandler {
                                  override final def onSuccess(responseBody: JsonStream): Unit = {
                                    val responseStream = JsonStream.OBJECT(generator1(new JsonStreamPair(
                                      "success",
                                      JsonStream.OBJECT(generator1(new JsonStreamPair(
                                        id,
                                        responseBody))))))
                                    send(toByteBuffer(responseStream): _*)
                                  }
                                  override final def onFailure(errorBody: JsonStream): Unit = {
                                    val responseStream = JsonStream.OBJECT(generator1(new JsonStreamPair(
                                      "failure",
                                      JsonStream.OBJECT(generator1(new JsonStreamPair(
                                        id,
                                        errorBody))))))
                                    send(toByteBuffer(responseStream): _*)
                                  }
                                })
                            }
                          }
                        }
                      }
                      case _ => {
                        logger.severe("Illegal rpc data!")
                        interrupt()
                      }
                    }
                  }
                }
                case _ => {
                  logger.severe("Illegal rpc data!")
                  interrupt()
                }
              }
            }
            case "failure" => {
              requestOrResponsePair.value match {
                case JsonStreamExtractor.Object(idPairs) => {
                  for (idPair <- idPairs) {
                    val id = try {
                      idPair.key.toInt
                    } catch {
                      case e: NumberFormatException => {
                        interrupt()
                        throw new RpcException.IllegalRpcData(cause = e)
                      }
                    }
                    outgoingRpcResponseHandlers.remove(id) match {
                      case None => {
                        logger.severe("Illegal rpc data!")
                        interrupt()
                      }
                      case Some(handler) => {
                        handler.onFailure(idPair.value)
                      }
                    }
                  }
                }
                case _ => {
                  logger.severe("Illegal rpc data!")
                  interrupt()
                }
              }

            }
            case "success" => {
              requestOrResponsePair.value match {
                case JsonStreamExtractor.Object(idPairs) => {
                  for (idPair <- idPairs) {
                    val id = try {
                      idPair.key.toInt
                    } catch {
                      case e: NumberFormatException => {
                        interrupt()
                        throw new RpcException.IllegalRpcData(cause = e)
                      }
                    }
                    outgoingRpcResponseHandlers.remove(id) match {
                      case None => {
                        logger.severe(this + " Illegal rpc data: " + id)
                        interrupt()
                      }
                      case Some(handler) => {
                        handler.onSuccess(idPair.value)
                      }
                    }
                  }
                }
                case _ => {
                  logger.severe("Illegal rpc data!")
                  interrupt()
                }
              }
            }
            case _ => {
              logger.severe("Illegal rpc data!")
              interrupt()
            }
          }
        }
      }
      case _ => {
        logger.severe("Illegal rpc data!")
        interrupt()
      }
    }
  }

}

