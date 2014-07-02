package com.qifun.qforce.bcp

package object rpc {
  type RpcServer[Session <: RpcSession] = BcpServer[Session]
}