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

import java.nio.ByteBuffer
import scala.util.control.Exception
import scala.annotation.tailrec
import java.io.EOFException

private[rpc] final class ByteBufferInput(buffers: Iterator[ByteBuffer]) {

  var current: ByteBuffer = if (buffers.hasNext) {
    buffers.next()
  } else {
    null
  }

  @tailrec
  final def readByte(): Int = {
    if (current == null) {
      throw new EOFException
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
  
  final def readInt(): Int = {
    (readByte() << 24) | (readByte() << 16) | (readByte() << 8) | readByte()
  }
  
  final def readShort(): Short = {
    ((readByte() << 8) | readByte()).toShort
  }

  final def readBytes(buffer: ByteBuffer, pos: Int, len: Int) = {
    if (current == null) {
      throw new EOFException
    } else {
      if (len < current.remaining) {
        current.get(buffer.array, pos, len)
        len
      } else {
        val result = current.remaining
        current.get(buffer.array, pos, result)
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