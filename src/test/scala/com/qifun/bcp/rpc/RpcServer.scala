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
import java.util.concurrent.Executors
import java.io.IOException
import scala.util.control.Exception.Catcher
import com.dongxiguo.zeroLog._

abstract class RpcServer extends BcpServer {
  
  final object RpcTestServerSession {
    val Exceptions = RpcSession.ErrorCodeRegistration(RpcSession.ErrorCodeEntry(new RpcTestException))
  }

  override protected final val executor = Executors.newScheduledThreadPool(1)
  executor.submit(new Runnable { override def run(): Unit = {} })

  val channelGroup = AsynchronousChannelGroup.withThreadPool(executor)
  val serverSocket = AsynchronousServerSocketChannel.open(channelGroup)

  private def startAccept(serverSocket: AsynchronousServerSocketChannel): Unit = {
    val acceptFuture = Future {
      val newSocket = Nio2Future.accept(serverSocket).await
      startAccept(serverSocket)
      addIncomingSocket(newSocket)
    }
    implicit def catcher: Catcher[Unit] = {
      case e: IOException => {
      }
      case otherException: Exception => {
      }
    }

    for (newSocket <- acceptFuture) {
    }
  }

  serverSocket.bind(new java.net.InetSocketAddress(3333))
  startAccept(serverSocket)
}