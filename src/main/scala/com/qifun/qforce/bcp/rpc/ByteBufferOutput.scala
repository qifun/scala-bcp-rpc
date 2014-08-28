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

import haxe.io.Output
import scala.collection.mutable.Buffer
import java.nio.ByteBuffer
import haxe.io.Bytes

private[rpc] object ByteBufferOutput {
  final val PageSize = 128
}

private[rpc] final class ByteBufferOutput extends Output {

  private val buffers = Buffer.empty[ByteBuffer]

  /**
   * Produces a collection from the added elements.
   *
   * The [[ByteBufferOutput]]'s contents are undefined after this operation.
   */
  def result(): Seq[ByteBuffer] = {
    for (buffer <- buffers) yield buffer.flip().asInstanceOf[ByteBuffer]
  }

  override def writeByte(b: Int) {
    val current = if (buffers.isEmpty) {
      val newBuffer = ByteBuffer.allocate(ByteBufferOutput.PageSize)
      buffers += newBuffer
      newBuffer
    } else {
      val last = buffers.last
      if (last.remaining > 0) {
        last
      } else {
        val newBuffer = ByteBuffer.allocate(ByteBufferOutput.PageSize)
        buffers += newBuffer
        newBuffer
      }
    }
    current.put(b.toByte)
  }

  override def writeBytes(s: Bytes, pos: Int, len: Int): Int = {
    val current = if (buffers.isEmpty) {
      val newBuffer = ByteBuffer.allocate(ByteBufferOutput.PageSize)
      buffers += newBuffer
      newBuffer
    } else {
      val last = buffers.last
      if (last.remaining > 0) {
        last
      } else {
        val newBuffer = ByteBuffer.allocate(ByteBufferOutput.PageSize)
        buffers += newBuffer
        newBuffer
      }
    }
    if (len < current.remaining) {
      current.put(s.getData, pos, len)
      len
    } else {
      val result = current.remaining
      current.put(s.getData, pos, result)
      result
    }
  }
}