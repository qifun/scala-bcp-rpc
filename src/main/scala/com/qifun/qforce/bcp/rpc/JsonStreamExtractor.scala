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
          Some(WrappedHaxeIterator(haxe.root.Type.enumParameters(0)).asInstanceOf[WrappedHaxeIterator[JsonStreamPair]])
        }
        case _ => None
      }
    }

  }
}