// Generated by ScalaBuff, the Scala Protocol Buffers compiler. DO NOT EDIT!
// source: RpcProtobuf.proto

package com.qifun.bcp.rpc.test

final case class RpcTestRequest (
	`id`: Option[Int] = None
) extends com.google.protobuf.GeneratedMessageLite
	with com.google.protobuf.MessageLite.Builder
	with net.sandrogrzicic.scalabuff.Message[RpcTestRequest]
	with net.sandrogrzicic.scalabuff.Parser[RpcTestRequest] {

	def setId(_f: Int) = copy(`id` = Some(_f))

	def clearId = copy(`id` = None)

	def writeTo(output: com.google.protobuf.CodedOutputStream) {
		if (`id`.isDefined) output.writeInt32(1, `id`.get)
	}

	def getSerializedSize = {
		import com.google.protobuf.CodedOutputStream._
		var __size = 0
		if (`id`.isDefined) __size += computeInt32Size(1, `id`.get)

		__size
	}

	def mergeFrom(in: com.google.protobuf.CodedInputStream, extensionRegistry: com.google.protobuf.ExtensionRegistryLite): RpcTestRequest = {
		import com.google.protobuf.ExtensionRegistryLite.{getEmptyRegistry => _emptyRegistry}
		var __id: Option[Int] = `id`

		def __newMerged = RpcTestRequest(
			__id
		)
		while (true) in.readTag match {
			case 0 => return __newMerged
			case 8 => __id = Some(in.readInt32())
			case default => if (!in.skipField(default)) return __newMerged
		}
		null
	}

	def mergeFrom(m: RpcTestRequest) = {
		RpcTestRequest(
			m.`id`.orElse(`id`)
		)
	}

	def getDefaultInstanceForType = RpcTestRequest.defaultInstance
	def clear = getDefaultInstanceForType
	def isInitialized = true
	def build = this
	def buildPartial = this
	def parsePartialFrom(cis: com.google.protobuf.CodedInputStream, er: com.google.protobuf.ExtensionRegistryLite) = mergeFrom(cis, er)
	override def getParserForType = this
	def newBuilderForType = getDefaultInstanceForType
	def toBuilder = this
	def toJson(indent: Int = 0): String = "ScalaBuff JSON generation not enabled. Use --generate_json_method to enable."
}

object RpcTestRequest {
	@beans.BeanProperty val defaultInstance = new RpcTestRequest()

	def parseFrom(data: Array[Byte]): RpcTestRequest = defaultInstance.mergeFrom(data)
	def parseFrom(data: Array[Byte], offset: Int, length: Int): RpcTestRequest = defaultInstance.mergeFrom(data, offset, length)
	def parseFrom(byteString: com.google.protobuf.ByteString): RpcTestRequest = defaultInstance.mergeFrom(byteString)
	def parseFrom(stream: java.io.InputStream): RpcTestRequest = defaultInstance.mergeFrom(stream)
	def parseDelimitedFrom(stream: java.io.InputStream): Option[RpcTestRequest] = defaultInstance.mergeDelimitedFromStream(stream)

	val ID_FIELD_NUMBER = 1

	def newBuilder = defaultInstance.newBuilderForType
	def newBuilder(prototype: RpcTestRequest) = defaultInstance.mergeFrom(prototype)

}
final case class RpcTestResponse (
	`id`: Option[Int] = None
) extends com.google.protobuf.GeneratedMessageLite
	with com.google.protobuf.MessageLite.Builder
	with net.sandrogrzicic.scalabuff.Message[RpcTestResponse]
	with net.sandrogrzicic.scalabuff.Parser[RpcTestResponse] {

	def setId(_f: Int) = copy(`id` = Some(_f))

	def clearId = copy(`id` = None)

	def writeTo(output: com.google.protobuf.CodedOutputStream) {
		if (`id`.isDefined) output.writeInt32(1, `id`.get)
	}

	def getSerializedSize = {
		import com.google.protobuf.CodedOutputStream._
		var __size = 0
		if (`id`.isDefined) __size += computeInt32Size(1, `id`.get)

		__size
	}

	def mergeFrom(in: com.google.protobuf.CodedInputStream, extensionRegistry: com.google.protobuf.ExtensionRegistryLite): RpcTestResponse = {
		import com.google.protobuf.ExtensionRegistryLite.{getEmptyRegistry => _emptyRegistry}
		var __id: Option[Int] = `id`

		def __newMerged = RpcTestResponse(
			__id
		)
		while (true) in.readTag match {
			case 0 => return __newMerged
			case 8 => __id = Some(in.readInt32())
			case default => if (!in.skipField(default)) return __newMerged
		}
		null
	}

	def mergeFrom(m: RpcTestResponse) = {
		RpcTestResponse(
			m.`id`.orElse(`id`)
		)
	}

	def getDefaultInstanceForType = RpcTestResponse.defaultInstance
	def clear = getDefaultInstanceForType
	def isInitialized = true
	def build = this
	def buildPartial = this
	def parsePartialFrom(cis: com.google.protobuf.CodedInputStream, er: com.google.protobuf.ExtensionRegistryLite) = mergeFrom(cis, er)
	override def getParserForType = this
	def newBuilderForType = getDefaultInstanceForType
	def toBuilder = this
	def toJson(indent: Int = 0): String = "ScalaBuff JSON generation not enabled. Use --generate_json_method to enable."
}

object RpcTestResponse {
	@beans.BeanProperty val defaultInstance = new RpcTestResponse()

	def parseFrom(data: Array[Byte]): RpcTestResponse = defaultInstance.mergeFrom(data)
	def parseFrom(data: Array[Byte], offset: Int, length: Int): RpcTestResponse = defaultInstance.mergeFrom(data, offset, length)
	def parseFrom(byteString: com.google.protobuf.ByteString): RpcTestResponse = defaultInstance.mergeFrom(byteString)
	def parseFrom(stream: java.io.InputStream): RpcTestResponse = defaultInstance.mergeFrom(stream)
	def parseDelimitedFrom(stream: java.io.InputStream): Option[RpcTestResponse] = defaultInstance.mergeDelimitedFromStream(stream)

	val ID_FIELD_NUMBER = 1

	def newBuilder = defaultInstance.newBuilderForType
	def newBuilder(prototype: RpcTestResponse) = defaultInstance.mergeFrom(prototype)

}
final case class RpcTestEvent (
	`id`: Option[Int] = None
) extends com.google.protobuf.GeneratedMessageLite
	with com.google.protobuf.MessageLite.Builder
	with net.sandrogrzicic.scalabuff.Message[RpcTestEvent]
	with net.sandrogrzicic.scalabuff.Parser[RpcTestEvent] {

	def setId(_f: Int) = copy(`id` = Some(_f))

	def clearId = copy(`id` = None)

	def writeTo(output: com.google.protobuf.CodedOutputStream) {
		if (`id`.isDefined) output.writeInt32(1024, `id`.get)
	}

	def getSerializedSize = {
		import com.google.protobuf.CodedOutputStream._
		var __size = 0
		if (`id`.isDefined) __size += computeInt32Size(1024, `id`.get)

		__size
	}

	def mergeFrom(in: com.google.protobuf.CodedInputStream, extensionRegistry: com.google.protobuf.ExtensionRegistryLite): RpcTestEvent = {
		import com.google.protobuf.ExtensionRegistryLite.{getEmptyRegistry => _emptyRegistry}
		var __id: Option[Int] = `id`

		def __newMerged = RpcTestEvent(
			__id
		)
		while (true) in.readTag match {
			case 0 => return __newMerged
			case 8192 => __id = Some(in.readInt32())
			case default => if (!in.skipField(default)) return __newMerged
		}
		null
	}

	def mergeFrom(m: RpcTestEvent) = {
		RpcTestEvent(
			m.`id`.orElse(`id`)
		)
	}

	def getDefaultInstanceForType = RpcTestEvent.defaultInstance
	def clear = getDefaultInstanceForType
	def isInitialized = true
	def build = this
	def buildPartial = this
	def parsePartialFrom(cis: com.google.protobuf.CodedInputStream, er: com.google.protobuf.ExtensionRegistryLite) = mergeFrom(cis, er)
	override def getParserForType = this
	def newBuilderForType = getDefaultInstanceForType
	def toBuilder = this
	def toJson(indent: Int = 0): String = "ScalaBuff JSON generation not enabled. Use --generate_json_method to enable."
}

object RpcTestEvent {
	@beans.BeanProperty val defaultInstance = new RpcTestEvent()

	def parseFrom(data: Array[Byte]): RpcTestEvent = defaultInstance.mergeFrom(data)
	def parseFrom(data: Array[Byte], offset: Int, length: Int): RpcTestEvent = defaultInstance.mergeFrom(data, offset, length)
	def parseFrom(byteString: com.google.protobuf.ByteString): RpcTestEvent = defaultInstance.mergeFrom(byteString)
	def parseFrom(stream: java.io.InputStream): RpcTestEvent = defaultInstance.mergeFrom(stream)
	def parseDelimitedFrom(stream: java.io.InputStream): Option[RpcTestEvent] = defaultInstance.mergeDelimitedFromStream(stream)

	val ID_FIELD_NUMBER = 1024

	def newBuilder = defaultInstance.newBuilderForType
	def newBuilder(prototype: RpcTestEvent) = defaultInstance.mergeFrom(prototype)

}
final case class RpcTestInfo (

) extends com.google.protobuf.GeneratedMessageLite
	with com.google.protobuf.MessageLite.Builder
	with net.sandrogrzicic.scalabuff.Message[RpcTestInfo]
	with net.sandrogrzicic.scalabuff.Parser[RpcTestInfo] {



	def writeTo(output: com.google.protobuf.CodedOutputStream) {
	}

	def getSerializedSize = {
		import com.google.protobuf.CodedOutputStream._
		var __size = 0

		__size
	}

	def mergeFrom(in: com.google.protobuf.CodedInputStream, extensionRegistry: com.google.protobuf.ExtensionRegistryLite): RpcTestInfo = {
		import com.google.protobuf.ExtensionRegistryLite.{getEmptyRegistry => _emptyRegistry}

		def __newMerged = RpcTestInfo(

		)
		while (true) in.readTag match {
			case 0 => return __newMerged
			case default => if (!in.skipField(default)) return __newMerged
		}
		null
	}

	def mergeFrom(m: RpcTestInfo) = {
		RpcTestInfo(

		)
	}

	def getDefaultInstanceForType = RpcTestInfo.defaultInstance
	def clear = getDefaultInstanceForType
	def isInitialized = true
	def build = this
	def buildPartial = this
	def parsePartialFrom(cis: com.google.protobuf.CodedInputStream, er: com.google.protobuf.ExtensionRegistryLite) = mergeFrom(cis, er)
	override def getParserForType = this
	def newBuilderForType = getDefaultInstanceForType
	def toBuilder = this
	def toJson(indent: Int = 0): String = "ScalaBuff JSON generation not enabled. Use --generate_json_method to enable."
}

object RpcTestInfo {
	@beans.BeanProperty val defaultInstance = new RpcTestInfo()

	def parseFrom(data: Array[Byte]): RpcTestInfo = defaultInstance.mergeFrom(data)
	def parseFrom(data: Array[Byte], offset: Int, length: Int): RpcTestInfo = defaultInstance.mergeFrom(data, offset, length)
	def parseFrom(byteString: com.google.protobuf.ByteString): RpcTestInfo = defaultInstance.mergeFrom(byteString)
	def parseFrom(stream: java.io.InputStream): RpcTestInfo = defaultInstance.mergeFrom(stream)
	def parseDelimitedFrom(stream: java.io.InputStream): Option[RpcTestInfo] = defaultInstance.mergeDelimitedFromStream(stream)


	def newBuilder = defaultInstance.newBuilderForType
	def newBuilder(prototype: RpcTestInfo) = defaultInstance.mergeFrom(prototype)

}

object RpcProtobuf {
	def registerAllExtensions(registry: com.google.protobuf.ExtensionRegistryLite) {
	}

	private val fromBinaryHintMap = collection.immutable.HashMap[String, Array[Byte] ⇒ com.google.protobuf.GeneratedMessageLite](
		 "RpcTestRequest" -> (bytes ⇒ RpcTestRequest.parseFrom(bytes)),
		 "RpcTestResponse" -> (bytes ⇒ RpcTestResponse.parseFrom(bytes)),
		 "RpcTestEvent" -> (bytes ⇒ RpcTestEvent.parseFrom(bytes)),
		 "RpcTestInfo" -> (bytes ⇒ RpcTestInfo.parseFrom(bytes))
	)

	def deserializePayload(payload: Array[Byte], payloadType: String): com.google.protobuf.GeneratedMessageLite = {
		fromBinaryHintMap.get(payloadType) match {
			case Some(f) ⇒ f(payload)
			case None    ⇒ throw new IllegalArgumentException(s"unimplemented deserialization of message payload of type [${payloadType}]")
		}
	}
}