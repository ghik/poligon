package com.github.ghik.poligon.cborpc.grpc.server

import com.github.ghik.poligon.cborpc._
import com.github.ghik.poligon.cborpc.grpc.CborDescriptors
import io.grpc.ServerServiceDefinition
import monix.execution.Scheduler

object CborServiceDefinition {
  def apply[T: CborApiMetadata : RawCborApi.AsRawRpc](
    impl: T
  )(implicit
    scheduler: Scheduler
  ): ServerServiceDefinition = {
    val metadata = CborApiMetadata[T]
    val apiName = metadata.nameInfo.rawName
    val serverSchemas = CborSchemas[T]

    val schemaExchangeHandler = new CborRpcRpcSchemaExchangeHandler(apiName, serverSchemas)
    val apiHandler = new CborRpcServerCallHandler(apiName, schemaExchangeHandler, _ => RawCborApi.asRaw(impl))
    val ssdBuilder =
      ServerServiceDefinition.builder(apiName)
        .addMethod(CborDescriptors.schemaExchangeDescriptor(apiName), schemaExchangeHandler)

    CborDescriptors.allMethodDescriptors[T].foreach { desc =>
      ssdBuilder.addMethod(desc, apiHandler)
    }
    ssdBuilder.build()
  }
}
