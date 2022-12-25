package com.github.ghik.poligon.grpc

import com.avsystem.commons.meta.{MacroInstances, multi}
import com.avsystem.commons.rpc._
import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.cbor.{CborInput, CborOutput, RawCbor}
import com.google.common.io.ByteStreams
import io.grpc.KnownLength
import io.grpc.MethodDescriptor.Marshaller
import monix.eval.Task
import monix.reactive.Observable

import java.io.{ByteArrayInputStream, InputStream}

trait RawCyborgRpc {
  @multi @encoded
  def unaryCall(@methodName name: String, @encoded input: RawCbor): Observable[RawCbor]

  @multi @encoded
  def streamingCall(@methodName name: String, @encoded input: Observable[RawCbor]): Observable[RawCbor]
}
object RawCyborgRpc extends RawRpcCompanion[RawCyborgRpc]

object CyborgRpcImplicits {
  implicit def rawCborAsRawReal[T: GenCodec]: AsRawReal[RawCbor, T] =
    AsRawReal.create(
      t => CborOutput.writeRawCbor[T](t),
      rawCbor => CborInput.readRawCbor[T](rawCbor)
    )

  implicit def rawCborObservableAsRaw[T](
    implicit forT: AsRaw[RawCbor, T]
  ): AsRaw[Observable[RawCbor], Observable[T]] =
    obs => obs.map(forT.asRaw)

  implicit def rawCborObservableAsReal[T](
    implicit forT: AsReal[RawCbor, T]
  ): AsReal[Observable[RawCbor], Observable[T]] =
    obs => obs.map(forT.asReal)

  implicit def rawCborTaskAsRaw[T](
    implicit forT: AsRaw[RawCbor, T]
  ): AsRaw[Observable[RawCbor], Task[T]] =
    task => Observable.fromTask(task.map(forT.asRaw))

  implicit def rawCborTaskAsReal[T](
    implicit forT: AsReal[RawCbor, T]
  ): AsReal[Observable[RawCbor], Task[T]] =
    obs => obs.firstL.map(forT.asReal)
}

abstract class CyborgRpcCompanion[T](implicit
  instances: MacroInstances[CyborgRpcImplicits.type, () => AsRawReal[RawCyborgRpc, T]]
) {
  implicit lazy val asRawReal: AsRawReal[RawCyborgRpc, T] = instances(CyborgRpcImplicits, this).apply()
}

object RawCborMarshaller extends Marshaller[RawCbor] {
  def stream(value: RawCbor): InputStream =
    new ByteArrayInputStream(value.bytes, value.offset, value.length) with KnownLength

  def parse(stream: InputStream): RawCbor =
    RawCbor(ByteStreams.toByteArray(stream))
}
