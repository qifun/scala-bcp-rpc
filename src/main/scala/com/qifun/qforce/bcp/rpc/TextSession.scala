package com.qifun.qforce.bcp.rpc

import java.nio.ByteBuffer
import com.qifun.jsonStream.JsonStream
import com.qifun.jsonStream.io.PrettyTextPrinter
import com.qifun.jsonStream.io.TextParser
import com.qifun.qforce.bcp.BcpSession
import com.dongxiguo.continuation.utils.{ Generator => HaxeGenerator }
import com.qifun.jsonStream.rpc.IJsonService
import com.qifun.jsonStream.JsonStreamPair
import com.qifun.jsonStream.rpc.IJsonResponseHandler

/**
 * 收到的包都是JSON格式的数据，有以下三种格式的包：
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
 */
trait TextSession extends RpcSession { _: BcpSession[_, _] =>

  import RpcSession.generator1

  override protected final def toByteBuffer(js: JsonStream): Seq[ByteBuffer] = {
    val output = new ByteBufferOutput
    PrettyTextPrinter.print(output, js, 0)
    output.result()
  }

  override protected final def toJsonStream(buffers: java.nio.ByteBuffer*): JsonStream = {
    TextParser.parseInput(new ByteBufferInput(buffers.iterator))
  }

  final def outgoingService[ServiceInterface](
    implicit entry: RpcSession.OutgoingProxyEntry[ServiceInterface]): ServiceInterface = {

    val serviceClassName = entry.serviceTag.toString

    entry.outgoingView(new IJsonService {
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
                              throw new RpcException.UnknownServiceName
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
            case "failure" => {
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
                        handler.onFailure(idPair.value)
                      }
                    }
                  }
                }
                case _ => {
                  throw new RpcException.IllegalRpcData
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
                        throw new RpcException.IllegalRpcData(cause = e)
                      }
                    }
                    outgoingRpcResponseHandlers.remove(id) match {
                      case None => {
                        throw new RpcException.IllegalRpcData
                      }
                      case Some(handler) => {
                        handler.onSuccess(idPair.value)
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

