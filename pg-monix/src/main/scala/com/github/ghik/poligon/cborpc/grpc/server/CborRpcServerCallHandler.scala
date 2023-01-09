package com.github.ghik.poligon.cborpc.grpc.server

import com.avsystem.commons._
import com.avsystem.commons.serialization.cbor.RawCbor
import com.github.ghik.poligon.cborpc.{CborSchemas, RawCborApi}
import io.grpc._
import monix.execution.Scheduler

import java.net.SocketAddress
import java.util.concurrent.ConcurrentHashMap

trait ClientSchemaRegistry {
  def getClientSchema(address: SocketAddress): CborSchemas
}

class CborRpcServerCallHandler(
  serviceName: String,
  clientSchemaRegistry: ClientSchemaRegistry,
  rawApiForClientSchemas: CborSchemas => RawCborApi
)(implicit scheduler: Scheduler) extends ServerCallHandler[RawCbor, RawCbor] {

  def startCall(call: ServerCall[RawCbor, RawCbor], headers: Metadata): ServerCall.Listener[RawCbor] =
    new ServerCall.Listener[RawCbor] {

    }
}

class CborRpcRpcSchemaExchangeHandler(
  serviceName: String,
  serverSchemas: CborSchemas,
)(implicit scheduler: Scheduler) extends ServerCallHandler[CborSchemas, CborSchemas] with ClientSchemaRegistry {

  private val clientSchemaRegistry = new ConcurrentHashMap[SocketAddress, CborSchemas]().asScala

  def getClientSchema(address: SocketAddress): CborSchemas =
    clientSchemaRegistry(address)

  def startCall(call: ServerCall[CborSchemas, CborSchemas], headers: Metadata): ServerCall.Listener[CborSchemas] =
    new ServerCall.Listener[CborSchemas] {
      override def onMessage(message: CborSchemas): Unit = {
        val clientAddr = call.getAttributes.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR)
        clientSchemaRegistry(clientAddr) = message
        call.sendMessage(serverSchemas)
        call.close(Status.OK, new Metadata)
      }
    }
}
