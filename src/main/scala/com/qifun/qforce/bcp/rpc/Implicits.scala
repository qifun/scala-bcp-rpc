package com.qifun.qforce.bcp.rpc

import com.qifun.statelessFuture._
import scala.runtime.BoxedUnit
import scala.util.control.TailCalls.TailRec

object Implicits {

  import scala.language.implicitConversions
  /**
   * 把`future`转换成其运行时类型。
   *
   * 由于Haxe生成的Java代码不支持Scala的[[Unit]]类型，所以只能使用运行时类型。
   * 当在Scala中实现Haxe生成的接口时，如果涉及上述运行时类型，就必须进行本转换，编译器才会放行。
   */
  implicit def boxedFuture[AwaitResult](future: Future[AwaitResult]): Awaitable[AwaitResult, BoxedUnit] = {
    future.asInstanceOf[Awaitable[AwaitResult, BoxedUnit]]
  }

  /**
   * 把`future`转换成其Scala类型。
   *
   * 由于Haxe生成的Java代码不支持Scala的[[Unit]]类型，所以只能使用运行时类型。
   * 当在Scala中实现Haxe生成的接口时，如果涉及上述运行时类型，就必须进行本转换，编译器才会放行。
   */
  implicit def unboxedFuture[AwaitResult](future: Awaitable[AwaitResult, BoxedUnit]): Future[AwaitResult] = {
    future.asInstanceOf[Future[AwaitResult]]
  }

}