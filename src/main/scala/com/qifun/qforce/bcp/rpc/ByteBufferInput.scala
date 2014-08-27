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

import haxe.io.Input
import java.nio.ByteBuffer
import haxe.io.Eof
import haxe.lang.HaxeException
import scala.util.control.Exception
import scala.annotation.tailrec

private[rpc] final class ByteBufferInput(buffers: Iterator[ByteBuffer]) extends Input {

  var current: ByteBuffer = if (buffers.hasNext) {
    buffers.next()
  } else {
    null
  }

  @tailrec
  override final def readByte(): Int = {
    if (current == null) {
      throw HaxeException.wrap(new Eof)
    } else {
      if (current.remaining() == 0) {
        if (buffers.hasNext) {
          current = buffers.next
        } else {
          current = null
        }
        readByte()
      } else {
        val result = current.get().toInt & 0xFF
        if (current.remaining == 0) {
          current = if (buffers.hasNext) {
            buffers.next()
          } else {
            null
          }
        }
        result
      }
    }
  }

  override final def readBytes(s: haxe.io.Bytes, pos: Int, len: Int) = {
    if (current == null) {
      throw HaxeException.wrap(new Eof)
    } else {
      if (len < current.remaining) {
        current.get(s.getData, pos, len)
        len
      } else {
        val result = current.remaining
        current.get(s.getData, pos, result)
        if (current.remaining == 0) {
          current = if (buffers.hasNext) {
            buffers.next()
          } else {
            null
          }
        }
        result
      }
    }
  }
}