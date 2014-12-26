package com.qifun.bcp.rpc

import com.google.protobuf.GeneratedMessageLite

final object RpcCallback {

  type RequestCallback[TRequest <: GeneratedMessageLite, TService <: RpcService, TResponse <: GeneratedMessageLite] = Function2[TRequest, RpcService, TResponse]

  type MessageCallback[TMessage <: GeneratedMessageLite, TService <: RpcService] = Function2[TMessage, TService, Unit]

}