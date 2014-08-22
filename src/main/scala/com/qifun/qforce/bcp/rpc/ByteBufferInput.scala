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