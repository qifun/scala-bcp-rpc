package com.qifun.qforce.bcp.rpc

import com.qifun.jsonStream.JsonStream
import scala.reflect.classTag
import scala.reflect.ClassTag
import haxe.lang.Function
import com.qifun.qforce.bcp.BcpServer
import java.nio.ByteBuffer
import com.qifun.statelessFuture.util.Generator
import scala.actors.threadpool.AtomicInteger
import scala.collection.concurrent.TrieMap
import com.qifun.jsonStream.AsynchronousJsonStream

private object TextSession {
  private val RequestHead0 = ByteBuffer.wrap("{\n\t\"request\": {\n\t\t\"".getBytes("UTF-8"))
  private val RequestHead1 = ByteBuffer.wrap("\": ".getBytes("UTF-8"))
  private val RequestTail = ByteBuffer.wrap("\t}\n}\n".getBytes("UTF-8"))

}

trait TextSession extends RpcSession {

  val nextRequestId = new AtomicInteger(0)

  val outgoingRpcResponseHandler = TrieMap.empty[Int, Function]

  private def toByteBuffer(js: JsonStream) = Generator[ByteBuffer].Future {
    ???
  }

  private def toAsynchronousJsonStream(buffers: java.nio.ByteBuffer*): AsynchronousJsonStream = {
    ???
  }

  final def outgoingRpc[ServiceInterface: ClassTag](request: JsonStream, handler: Function): Unit = {
    val requestId = nextRequestId.getAndIncrement()
    outgoingRpcResponseHandler.putIfAbsent(requestId, handler) match {
      case None => {
        implicit val gen = Generator[ByteBuffer]
        val buffers: gen.OutputSeq = gen.Future {
          TextSession.RequestHead0.await
          ByteBuffer.wrap(requestId.toString.getBytes("UTF-8")).await
          TextSession.RequestHead1.await
          toByteBuffer(request).await
          TextSession.RequestTail.await
        }
        send(buffers: _*)
      }
      case Some(oldFunction) => {
        throw new IllegalStateException("")
      }
    }
  }

  override protected final def received(buffers: java.nio.ByteBuffer*): Unit = {
//    toAsynchronousJsonStream(buffers: _*).index match {
////      case
//    }
    ???
  }

}

