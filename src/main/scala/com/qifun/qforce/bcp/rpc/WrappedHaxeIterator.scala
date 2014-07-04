package com.qifun.qforce.bcp.rpc
import com.dongxiguo.continuation.utils.{ Generator => HaxeGenerator }
import haxe.root.Reflect

private[rpc] trait WrappedHaxeIterator[+Element] extends Iterator[Element]

private[rpc] object WrappedHaxeIterator {

  private final class WrappedHaxeGenerator[+Element](haxeGenerator: HaxeGenerator[Element]) extends WrappedHaxeIterator[Element] {

    override final def hasNext = haxeGenerator.hasNext

    override final def next() = haxeGenerator.next()

  }

  private final class WrappedReflectiveIterator(haxeIterator: Any) extends WrappedHaxeIterator[Any] {

    override final def hasNext = {
      Reflect.callMethod(haxeIterator, Reflect.field(haxeIterator, "hasNext"), new haxe.root.Array()).asInstanceOf[Boolean]
    }

    override final def next() = {
      Reflect.callMethod(haxeIterator, Reflect.field(haxeIterator, "next"), new haxe.root.Array())
    }

  }

  final def apply(haxeIterator: AnyRef): WrappedHaxeIterator[Any] = {
    haxeIterator match {
      case generator: HaxeGenerator[_] => new WrappedHaxeGenerator(generator)
      case _ => new WrappedReflectiveIterator(haxeIterator)
    }
  }

}