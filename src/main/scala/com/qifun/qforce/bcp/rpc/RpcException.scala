package com.qifun.qforce.bcp.rpc

import java.io.IOException

sealed abstract class RpcException(message: String = null, cause: Throwable = null)
  extends IOException(message, cause)

object RpcException {

  class IllegalRpcData(message: String = null, cause: Throwable = null)
    extends RpcException(message, cause)

  class UnknownServiceName(message: String = null, cause: Throwable = null)
    extends RpcException(message, cause)

}