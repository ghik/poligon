package com.github.ghik.poligon.cborpc.grpc

import com.avsystem.commons.serialization.cbor.RawCbor
import com.github.ghik.poligon.cborpc._
import io.grpc.MethodDescriptor
import io.grpc.MethodDescriptor.MethodType


object CborDescriptors {
  final val SchemaExchangeMethodName = "__schemaExchange"

  def schemaExchangeDescriptor(serviceName: String): MethodDescriptor[CborSchemas, CborSchemas] =
    MethodDescriptor.newBuilder[CborSchemas, CborSchemas](CborMarshaller[CborSchemas], CborMarshaller[CborSchemas])
      .setType(MethodType.UNARY)
      .setIdempotent(true)
      .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, SchemaExchangeMethodName))
      .build()

  def allMethodDescriptors[T: CborApiMetadata]: Seq[MethodDescriptor[RawCbor, RawCbor]] = {
    val apiName = CborApiMetadata[T].nameInfo.rawName
    val serverSchemas = CborSchemas[T]

    val builder = Vector.newBuilder[MethodDescriptor[RawCbor, RawCbor]]

    def traverse(prefix: List[String], api: QualifiedApi): Unit = {
      api.methods.foreach {
        case m: QualifiedSubapiMethod => traverse(m.method.name :: prefix, m.result)
        case m: QualifiedCallMethod =>
          val methodName = (m.method.name :: prefix).reverse.mkString(".")
          val descriptor = MethodDescriptor
            .newBuilder[RawCbor, RawCbor](RawCborMarshaller, RawCborMarshaller)
            .setType(if (m.method.stream) MethodType.SERVER_STREAMING else MethodType.UNARY)
            .setFullMethodName(MethodDescriptor.generateFullMethodName(apiName, methodName))
            .setSchemaDescriptor(m)
            .build()
          builder.addOne(descriptor)
      }
    }
    traverse(Nil, serverSchemas.qualifiedApi(apiName))
    builder.result()
  }
}
