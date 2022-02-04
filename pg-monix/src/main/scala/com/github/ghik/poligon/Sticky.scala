package com.github.ghik.poligon

import com.avsystem.commons.IArraySeq
import com.avsystem.commons.rpc._
import com.avsystem.commons.meta._
import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.cbor.{CborOutput, HasCborCodec, RawCbor}
import monix.eval.Task

trait BusinessRpcApiImplicits {
  implicit def asCbor[T: GenCodec]: AsRaw[RawCbor, T] =
    value => CborOutput.writeRawCbor[T](value)

  implicit def fromCbor[T: GenCodec]: AsReal[RawCbor, T] =
    rawCbor => rawCbor.readAs[T]()

  implicit def taskAsCbor[T: GenCodec]: AsRaw[Task[RawCbor], Task[T]] =
    task => task.map(asCbor[T].asRaw)

  implicit def taskFromCbor[T: GenCodec]: AsReal[Task[RawCbor], Task[T]] =
    task => task.map(fromCbor[T].asReal)
}
object BusinessRpcApiImplicits extends BusinessRpcApiImplicits

trait BusinessRpcApiInstances[T] {
  def asRawNetApi: RawNetApi.AsRawRpc[T]
  def asRealNetApi: RawNetApi.AsRealRpc[T]
}

abstract class BusinessRpcApiCompanion[T](implicit
  instances: MacroInstances[BusinessRpcApiImplicits, BusinessRpcApiInstances[T]]
) {
  implicit lazy val asRawNetApi: RawNetApi.AsRawRpc[T] = instances(BusinessRpcApiImplicits, this).asRawNetApi
  implicit lazy val asRealNetApi: RawNetApi.AsRealRpc[T] = instances(BusinessRpcApiImplicits, this).asRealNetApi
}

case class Invocation(@methodName name: String, @encoded @multi params: IArraySeq[RawCbor])

trait RawNetApi {
  @encoded @multi def invoke(@composite invocation: Invocation): Task[RawCbor]
  @encoded @multi def get(@composite invocation: Invocation): RawNetApi
}
object RawNetApi extends RawRpcCompanion[RawNetApi]

/* ---------------------------------------------------------- */

case class MoneyResult(
                      stuff: Float,
                      ok: Boolean
                      )
object MoneyResult extends HasCborCodec[MoneyResult]

trait SeriousBusinessApi {
  def earnMoney(money: Int, name: String): Task[String]
  def earnMoarMoney(money: BigInt, name: String): Task[Unit]

  def verySeriousApi: VerySeriousBusinessApi
}
object SeriousBusinessApi extends BusinessRpcApiCompanion[SeriousBusinessApi]

trait VerySeriousBusinessApi {
  def earnVeryMuchMoneyWow(monies: Double, currency: String): Task[Unit]
}
object VerySeriousBusinessApi extends BusinessRpcApiCompanion[VerySeriousBusinessApi]
