package com.qifun.bcp.rpc

import com.qifun.statelessFuture._
import com.qifun.statelessFuture.util.io.Nio2Future
import com.qifun.bcp._
import com.qifun.common.rpctest._
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.net.InetSocketAddress
import java.util.concurrent.ScheduledThreadPoolExecutor

abstract class RpcClient extends BcpClient with RpcSession {

  override final def available(): Unit = {}

  override final def executor = new ScheduledThreadPoolExecutor(1)

  override final def interrupted(): Unit = {}

  override final def shutedDown(): Unit = {}

  override final def unavailable(): Unit = {}

  private val internalIncomingServices = RpcSession.IncomingProxyRegistration()

  override protected final def incomingServices = internalIncomingServices

  protected def errorCodes = RpcSession.ErrorCodeRegistration(RpcSession.ErrorCodeEntry(new RpcTestException))

}