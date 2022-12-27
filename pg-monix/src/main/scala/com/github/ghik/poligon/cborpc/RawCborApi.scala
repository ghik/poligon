package com.github.ghik.poligon.cborpc

import com.avsystem.commons._
import com.avsystem.commons.meta._
import com.avsystem.commons.rpc._
import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.cbor.{CborOutput, RawCbor}
import monix.eval.Task
import monix.reactive.Observable

trait RawCborApi {
  @multi @encoded def prefix(@methodName name: String): RawCborApi
  @multi @encoded def call(@composite invocation: RawCborApi.Invocation): Observable[RawCbor]
}
object RawCborApi extends RawRpcCompanion[RawCborApi] {
  final case class Invocation(
    @methodName name: String,
    @encoded @multi args: IIndexedSeq[RawCbor],
  )
}

trait CborApiImplicits {
  implicit def asCbor[T: GenCodec]: AsRaw[RawCbor, T] =
    value => CborOutput.writeRawCbor[T](value)

  implicit def fromCbor[T: GenCodec]: AsReal[RawCbor, T] =
    rawCbor => rawCbor.readAs[T]()

  implicit def taskAsCbor[T](implicit asCbor: AsRaw[RawCbor, T]): AsRaw[Observable[RawCbor], Task[T]] =
    task => Observable.fromTask(task.map(asCbor.asRaw))

  implicit def taskFromCbor[T](implicit fromCbor: AsReal[RawCbor, T]): AsReal[Observable[RawCbor], Task[T]] =
    obs => obs.firstL.map(fromCbor.asReal)

  implicit def observableAsCbor[T](implicit asCbor: AsRaw[RawCbor, T]): AsRaw[Observable[RawCbor], Observable[T]] =
    obs => obs.map(asCbor.asRaw)

  implicit def observableFromCbor[T](implicit fromCbor: AsReal[RawCbor, T]): AsReal[Observable[RawCbor], Observable[T]] =
    obs => obs.map(fromCbor.asReal)
}
object CborApiImplicits extends CborApiImplicits
