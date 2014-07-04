package com.qifun.qforce.bcp.rpc

import haxe.io.Output
import scala.collection.mutable.Buffer
import java.nio.ByteBuffer
import haxe.io.Bytes

private[rpc] object ByteBufferOutput {
  final val PageSize = 128
}

private[rpc] final class ByteBufferOutput extends Output {

  val buffers = Buffer.empty[ByteBuffer]

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
      current.get(s.getData, pos, len)
      len
    } else {
      val result = current.remaining
      current.get(s.getData, pos, result)
      result
    }
  }
}