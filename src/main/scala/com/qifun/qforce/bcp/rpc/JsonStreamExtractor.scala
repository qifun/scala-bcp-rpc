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

import com.qifun.jsonStream.JsonStream
import com.qifun.jsonStream.JsonStreamPair

private[rpc] object JsonStreamExtractor {

  private val JsonStreamObjectIndex = {
    haxe.root.Type.getEnumConstructs(classOf[JsonStream]).indexOf("OBJECT", 0)
  }

  private val JsonStreamArrayIndex = {
    haxe.root.Type.getEnumConstructs(classOf[JsonStream]).indexOf("ARRAY", 0)
  }

  object Array {

    final def unapply(jsonStream: JsonStream): Option[WrappedHaxeIterator[JsonStream]] = {
      haxe.root.Type.enumIndex(jsonStream) match {
        case JsonStreamArrayIndex => {
          Some(WrappedHaxeIterator(haxe.root.Type.enumParameters(0)).asInstanceOf[WrappedHaxeIterator[JsonStream]])
        }
        case _ => None
      }
    }

  }

  object Object {

    final def unapply(jsonStream: JsonStream): Option[WrappedHaxeIterator[JsonStreamPair]] = {
      haxe.root.Type.enumIndex(jsonStream) match {
        case JsonStreamObjectIndex => {
          Some(WrappedHaxeIterator(haxe.root.Type.enumParameters(jsonStream).__a(0)).asInstanceOf[WrappedHaxeIterator[JsonStreamPair]])
        }
        case _ => None
      }
    }

  }
}