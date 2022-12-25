package com.github.ghik.poligon.grpc

import com.avsystem.commons.serialization.cbor.RawCbor
import io.grpc._
import monix.execution.Ack
import monix.reactive.Observable

final class RawCyborgRpcClient(
  channel: ManagedChannel,
  methodDescriptors: Map[String, MethodDescriptor[RawCbor, RawCbor]]
) extends RawCyborgRpc {
  def unaryCall(name: String, input: RawCbor): Observable[RawCbor] = ???
  def streamingCall(name: String, input: Observable[RawCbor]): Observable[RawCbor] = ???

  def singleInputCall(name: String, input: RawCbor): Observable[RawCbor] = {
    Observable.unsafeCreate { subscriber =>
      import subscriber.scheduler
      val clientCall = channel.newCall(methodDescriptors(name), CallOptions.DEFAULT)

      val respListener = new ClientCall.Listener[RawCbor] {
        override def onMessage(message: RawCbor): Unit =
          subscriber.onNext(message) match {
            case ack: Ack => onAck(ack)
            case fack => fack.foreach(onAck)
          }

        private def onAck(ack: Ack): Unit = ack match {
          case Ack.Continue => clientCall.request(1) //TODO buffer & request more
          case Ack.Stop => clientCall.cancel(null, null)
        }

        override def onClose(status: Status, trailers: Metadata): Unit =
          if (status.isOk) subscriber.onComplete()
          else subscriber.onError(new StatusException(status, trailers))
      }

      clientCall.start(respListener, new Metadata)
      clientCall.sendMessage(input)
      clientCall.halfClose()
      clientCall.request(1)

      () => clientCall.cancel(null, null)
    }
  }
}
