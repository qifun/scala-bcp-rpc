/*
 * scala-bcp-rpc
 * Copyright 2014 深圳岂凡网络有限公司 (Shenzhen QiFun Network Corp., LTD)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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