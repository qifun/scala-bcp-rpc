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

import com.google.protobuf.GeneratedMessageLite
import RpcCallback._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object RpcService {

  object IncomingMessageRegistration {
    final def apply(incomingEntries: IncomingEntry*) = {
      val map = (for {
        entry <- incomingEntries
      } yield {
        entry.messageType.tpe.toString -> entry
      })(collection.breakOut(Map.canBuildFrom))
      new IncomingMessageRegistration(map)
    }
  }

  final class IncomingMessageRegistration private (val incomingMessagesMap: Map[String, IncomingEntry])
    extends AnyVal

  abstract class IncomingEntry(val messageType: TypeTag[GeneratedMessageLite]) {

    def executeRequest(message: GeneratedMessageLite, service: RpcService): GeneratedMessageLite

    def executeMessage(message: GeneratedMessageLite, service: RpcService): Unit

  }

  final case class IncomingRequestEntry[TRequest <: GeneratedMessageLite, TService <: RpcService](
    val requestCallback: RequestCallback[TRequest, TService, GeneratedMessageLite])(implicit requestTag: TypeTag[TRequest])
    extends IncomingEntry(requestTag.asInstanceOf[TypeTag[GeneratedMessageLite]]) {
    
    override def executeRequest(message: GeneratedMessageLite, service: RpcService): GeneratedMessageLite = {
      requestCallback(message.asInstanceOf[TRequest], service.asInstanceOf[TService])
    }

    override def executeMessage(message: GeneratedMessageLite, service: RpcService): Unit = ???

  }

  final case class IncomingMessageEntry[TMessage <: GeneratedMessageLite, TService <: RpcService](
    val messageCallback: MessageCallback[TMessage, TService])(implicit messageTag: TypeTag[TMessage])
    extends IncomingEntry(messageTag.asInstanceOf[TypeTag[GeneratedMessageLite]]) {

    def executeRequest(message: GeneratedMessageLite, service: RpcService): GeneratedMessageLite = ???

    override def executeMessage(message: GeneratedMessageLite, service: RpcService): Unit = {
      messageCallback(message.asInstanceOf[TMessage], service.asInstanceOf[TService])
    }
  }

}

trait RpcService {

  import RpcService._

  def incomingMessages: RpcService.IncomingMessageRegistration

}
