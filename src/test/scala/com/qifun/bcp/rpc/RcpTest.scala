package com.qifun.bcp.rpc

import org.junit._
import Assert._
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.nio.ByteBuffer
import com.qifun.statelessFuture.Future
import com.qifun.statelessFuture.util.io.Nio2Future
import java.net.InetSocketAddress
import org.junit.Assert._
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import com.qifun.statelessFuture.util.Blocking
import java.util.concurrent.Executors
import scala.reflect.classTag
import com.qifun.statelessFuture.util.Promise
import java.nio.channels.CompletionHandler
import scala.util.Try
import scala.reflect.ClassTag
import scala.util.Success
import scala.util.Failure
import java.nio.channels.ShutdownChannelGroupException
import java.io.IOException
import scala.concurrent.stm.Ref
import scala.concurrent.stm._
import java.util.concurrent.TimeUnit
import com.qifun.bcp.BcpServer
import com.qifun.common.rpctest._
import scala.util.control.Exception.Catcher
import com.qifun.bcp._
import com.google.protobuf.GeneratedMessageLite

object RcpTest {

  private implicit val (logger, formatter, appender) = ZeroLoggerFactory.newLogger(this)
}
class RcpTest {
  import RcpTest.{ logger, formatter, appender }

  @Test
  def pingPong(): Unit = {
    val lock = new AnyRef
    @volatile var clientResult: Option[Int] = None
    @volatile var serverResult: Option[Int] = None
    @volatile var eventResult: Option[Int] = None
    val client = new RpcClient
    val server = new RpcServer {

      final object TestService {
        val messageCallbacks = RpcService.IncomingMessageRegistration(
          RpcService.IncomingRequestEntry[RpcTestRequest, RpcTestServerSession, RpcTestResponse](handleRpcRequest),
          RpcService.IncomingMessageEntry[RpcTestEvent, RpcTestServerSession](handleRpcEvent))

        final def handleRpcRequest(
          message: RpcTestRequest,
          session: RpcTestServerSession): RpcTestResponse = {
          lock.synchronized {
            serverResult = message.id
            lock.notify()
          }
          new RpcTestResponse(Some(123321))
        }

        final def handleRpcEvent(event: RpcTestEvent, service: RpcTestServerSession): Unit = {
          lock.synchronized {
            eventResult = event.id
            lock.notify()
          }
        }
      }

      final class TestService(val session: RpcTestServerSession) extends RpcService {

        def incomingMessages = TestService.messageCallbacks

      }

      trait RpcTestServerSession extends RpcSession { _: BcpServer#Session =>

        private def incomingServiceEntries = RpcSession.IncomingProxyRegistration(
          RpcSession.IncomingProxyEntry("com.qifun.common.rpctest", new TestService(this)))

        protected def errorCodes = RpcTestServerSession.Exceptions

        override protected final def incomingServices = incomingServiceEntries

        override protected final def available(): Unit = {}

        override protected final def accepted(): Unit = {}

        override protected final def shutedDown(): Unit = {}

        override protected final def interrupted(): Unit = {}

        override protected final def unavailable(): Unit = {}
      }

      override protected final def newSession(id: Array[Byte]) =
        new Session(id) with RpcTestServerSession with RpcSession
    }

    client.start()
    def ping(): Unit = {

      val sendMsg = new RpcTestRequest(Some(321123))
      val sendEvent = new RpcTestEvent(Some(1048576))
      client.outgoingProxy.pushMessage(sendEvent)
      client.outgoingProxy.sendRequest(sendMsg)((message: RpcTestResponse) => {
        lock.synchronized {
          clientResult = message.id
          lock.notify()
        }
      }, (message: GeneratedMessageLite) => {
        message match {
          case message: RpcTestException =>
            lock.synchronized {
              clientResult = Some(RpcTestException.CODE_FIELD_NUMBER)
              lock.notify()
            }
        }
      })
    }
    ping()

    lock.synchronized {
      while (serverResult == None || clientResult == None || eventResult == None) {
        lock.wait()
      }
    }

    assertEquals(clientResult, Some(123321))
    assertEquals(serverResult, Some(321123))
    assertEquals(eventResult, Some(1048576))
    
    client.shutedDown()
    server.clear()
  }
}


























