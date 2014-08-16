package com.qifun.qforce.bcp.rpc

import java.nio.ByteBuffer
import com.qifun.jsonStream.JsonStream
import com.qifun.jsonStream.io.PrettyTextPrinter
import com.qifun.jsonStream.io.TextParser
import com.qifun.qforce.bcp.BcpSession

trait TextSession extends RpcSession { _: BcpSession[_, _] =>

  override protected final def toByteBuffer(js: JsonStream): Seq[ByteBuffer] = {
    val output = new ByteBufferOutput
    PrettyTextPrinter.print(output, js, 0)
    output.result()
  }

  override protected final def toJsonStream(buffers: java.nio.ByteBuffer*): JsonStream = {
    TextParser.parseInput(new ByteBufferInput(buffers.iterator))
  }

}

