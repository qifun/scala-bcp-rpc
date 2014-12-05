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

package com.qifun.bcp.rpc

import com.qifun.statelessFuture._
import scala.util.control.Exception.Catcher
import scala.util.control.TailCalls.TailRec
import com.qifun.jsonStream.rpc.IFuture0
import com.qifun.jsonStream.rpc.ICompleteHandler0
import com.qifun.jsonStream.rpc.IFuture1
import com.qifun.jsonStream.rpc.ICompleteHandler1
import scala.util.control.TailCalls
import haxe.lang.HaxeException

object FutureConverters {

  implicit final class StatelessFutureToHaxeFuture0(future: Future[Unit]) extends IFuture0 {
    override final def start(completeHandler: ICompleteHandler0) = {
      (future.onComplete { _: Unit =>
        completeHandler.onSuccess()
        TailCalls.done(())
      } {
        case haxeException: HaxeException =>
          completeHandler.onFailure(haxeException.getObject())
          TailCalls.done(())
        case notHaxeException: Exception =>
          completeHandler.onFailure(notHaxeException)
          TailCalls.done(())
      }).result
    }
  }

  implicit final class StatelessFutureToHaxeFuture1[AwaitResult <: AnyRef](future: Future[AwaitResult]) extends IFuture1[AwaitResult] {
    override final def start(completeHandler: ICompleteHandler1[AwaitResult]) = {
      (future.onComplete { result =>
        completeHandler.onSuccess(result)
        TailCalls.done(())
      } {
        case haxeException: HaxeException =>
          completeHandler.onFailure(haxeException.getObject())
          TailCalls.done(())
        case notHaxeException: Exception =>
          completeHandler.onFailure(notHaxeException)
          TailCalls.done(())
      }).result
    }
  }
  
  implicit final class HaxeFuture0ToStatelessFuture(haxeFuture: IFuture0) extends Future.Stateless[Unit] {
    override final def onComplete(handler: Unit => TailRec[Unit])(implicit catcher: Catcher[TailRec[Unit]]): TailRec[Unit] = {
      haxeFuture.start(new ICompleteHandler0 {
        override final def onSuccess() = {
          handler(()).result
        }
        override final def onFailure(error: AnyRef) = {
          error match {
            case throwable: Throwable => {
              catcher(throwable).result
            }
            case notThrowable => {
              catcher(HaxeException.wrap(notThrowable)).result
            }
          }
        }
      })
      TailCalls.done(())
    }
  }

  implicit final class HaxeFuture1ToStatelessFuture[AwaitResult <: AnyRef](haxeFuture: IFuture1[AwaitResult]) extends Future.Stateless[AwaitResult] {
    override final def onComplete(handler: AwaitResult => TailRec[Unit])(implicit catcher: Catcher[TailRec[Unit]]): TailRec[Unit] = {
      haxeFuture.start(new ICompleteHandler1[AwaitResult] {
        override final def onSuccess(awaitResult: AwaitResult) = {
          handler(awaitResult).result
        }
        override final def onFailure(error: AnyRef) = {
          error match {
            case throwable: Throwable => {
              catcher(throwable).result
            }
            case notThrowable => {
              catcher(HaxeException.wrap(notThrowable)).result
            }
          }
        }
      })
      TailCalls.done(())
    }
  }

}