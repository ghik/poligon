package com.github.ghik.poligon.grpc

import com.avsystem.commons._
import com.avsystem.commons.serialization.cbor.RawCbor
import io.grpc.{Metadata, ServerCall, ServerCallHandler, Status}
import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, Observer}

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.{Future, Promise}

class CyborgRpcServerCallListener(
  raw: RawCyborgRpc,
  call: ServerCall[RawCbor, RawCbor]
)(implicit
  scheduler: Scheduler
) extends ServerCall.Listener[RawCbor] {
  private var outputDownstreamReady: Promise[Unit] = _
  private val inputDownstreamSubscriber = new AtomicReference[Subscriber[RawCbor]]

  call.sendHeaders(new Metadata)

  // TODO make this subscribable once, fail on subscribe attempt after the call finished
  private val inputObservable = Observable.unsafeCreate { subscriber: Subscriber[RawCbor] =>
    if (inputDownstreamSubscriber.compareAndSet(null, subscriber)) {
      call.request(1) //TODO buffer and request more
      () => call.close(Status.CANCELLED, new Metadata) //TODO: is it ok to close the entire call?
    } else {
      subscriber.onError(new IllegalStateException(s"Cannot subscribe more than once to a gRPC streaming request"))
      Cancelable.empty
    }
  }

  private val outputUpstreamCancelable = raw
    .streamingCall(call.getMethodDescriptor.getBareMethodName, inputObservable)
    .subscribe(new Observer[RawCbor] {
      def onNext(elem: RawCbor): Future[Ack] =
        if (call.isReady) {
          call.sendMessage(elem)
          Ack.Continue
        } else {
          val prom = Promise[Unit]()
          outputDownstreamReady = prom
          prom.future.flatMap { _ =>
            outputDownstreamReady = null
            onNext(elem)
          }
        }

      def onError(ex: Throwable): Unit =
        call.close(Status.fromThrowable(ex), Status.trailersFromThrowable(ex).opt.getOrElse(new Metadata))

      def onComplete(): Unit =
        call.close(Status.OK, new Metadata)
    })

  override def onMessage(message: RawCbor): Unit = {
    inputDownstreamSubscriber.get().onNext(message) match {
      case Ack.Continue => call.request(1)
      case Ack.Stop => call.close(Status.CANCELLED, new Metadata)
      case fut => fut.foreach { //TODO: tis thread safe to use `call` here?
        case Ack.Continue => call.request(1)
        case Ack.Stop => call.close(Status.CANCELLED, new Metadata)
      }
    }
  }

  override def onHalfClose(): Unit =
    inputDownstreamSubscriber.get().onComplete()

  override def onComplete(): Unit =
    super.onComplete()

  override def onReady(): Unit = {
    val prom = outputDownstreamReady
    if (prom ne null) {
      prom.trySuccess(())
    }
  }

  override def onCancel(): Unit =
    outputUpstreamCancelable.cancel()
}

class CyborgRpcServerCallHandler(raw: RawCyborgRpc)(implicit scheduler: Scheduler)
  extends ServerCallHandler[RawCbor, RawCbor] {

  def startCall(call: ServerCall[RawCbor, RawCbor], headers: Metadata): ServerCall.Listener[RawCbor] =
    new CyborgRpcServerCallListener(raw, call)
}
