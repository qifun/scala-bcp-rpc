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
import scala.reflect.ClassTag

final class ErrorCode[TError <: GeneratedMessageLite](
    final val errorMessage: TError,
    final val errorType: ClassTag[TError]) extends Throwable {
}

object ErrorCode {
  def apply[TError <: GeneratedMessageLite](errorMessage: TError)(implicit errorType: ClassTag[TError]) = {
    new ErrorCode(errorMessage, errorType)
  }
}
