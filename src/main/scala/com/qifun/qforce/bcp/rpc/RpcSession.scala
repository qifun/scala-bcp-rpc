package com.qifun.qforce.bcp.rpc

import haxe.lang.Function
import com.qifun.qforce.bcp.BcpServer
import com.qifun.jsonStream.rpc.OutgoingProxy
import com.qifun.jsonStream.rpc.IncomingProxy
import scala.reflect.classTag
import scala.reflect.ClassTag
import com.qifun.jsonStream.JsonStream
import com.qifun.jsonStream.AsynchronousJsonStream

object RpcSession {

  trait IncomingProxyImpl[ServiceInterface] extends IncomingProxy[ServiceInterface] { _: ServiceInterface =>

    override final def get_service: this.type = this

    // 由json-stream实现
    def incomingRpc(request: AsynchronousJsonStream, handler: Function): Unit

  }

  private object IncomingProxyEntry {

    import language.implicitConversions

    implicit def fromFunction[ServiceInterface: ClassTag, S <: RpcSession](
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
}

trait RpcSession extends BcpServer.Session {
  protected def incomingProxyFactory: RpcSession.IncomingProxyFactory[_ >: this.type]

} 