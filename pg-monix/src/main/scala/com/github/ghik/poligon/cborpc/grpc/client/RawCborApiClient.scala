package com.github.ghik.poligon.cborpc.grpc.client

import com.avsystem.commons.serialization.cbor.RawCbor
import com.github.ghik.poligon.cborpc.grpc.CborDescriptors
import com.github.ghik.poligon.cborpc.grpc.client.RawCborApiClient.schemaExchange
import com.github.ghik.poligon.cborpc.{CborSchemas, RawCborApi}
import io.grpc._
import monix.eval.Task
import monix.reactive.Observable

final class RawCborApiClient private(
  serviceName: String,
  channel: Channel,
  clientSchemas: CborSchemas,
  serverSchemas: Task[CborSchemas],
  prefix: List[String],
) extends RawCborApi {
  def this(
    serviceName: String,
    channel: Channel,
    clientSchemas: CborSchemas,
  ) = this(
    serviceName,
    channel,
    clientSchemas,
    schemaExchange(serviceName, channel, clientSchemas).memoizeOnSuccess,
    Nil
  )

  def prefix(name: String): RawCborApi =
    new RawCborApiClient(serviceName, channel, clientSchemas, serverSchemas, name :: prefix)

  def call(invocation: RawCborApi.Invocation): Observable[RawCbor] = {
    ???
  }
}

object RawCborApiClient {
  private def schemaExchange(serviceName: String, channel: Channel, clientSchemas: CborSchemas): Task[CborSchemas] =
    Task.async[CborSchemas] { callback =>
      val desc = CborDescriptors.schemaExchangeDescriptor(serviceName)
      val listener = new ClientCall.Listener[CborSchemas] {
        override def onMessage(message: CborSchemas): Unit =
          callback.onSuccess(message)
        override def onClose(status: Status, trailers: Metadata): Unit =
          if (!status.isOk) {
            callback.onError(new StatusException(status, trailers))
          }
      }

      val call = channel.newCall(desc, CallOptions.DEFAULT)
      call.start(listener, new Metadata)
      call.sendMessage(clientSchemas)
      call.halfClose()
      call.request(1)
    }
}
