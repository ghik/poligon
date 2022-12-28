package com.github.ghik.poligon.cborpc

import com.avsystem.commons._
import com.avsystem.commons.annotation.positioned
import com.avsystem.commons.meta._
import com.avsystem.commons.rpc._
import monix.eval.Task
import monix.reactive.Observable

final class CborApiMetadata[T](
  @composite val nameInfo: NameInfo,
  @multi @rpcMethodMetadata val methods: List[CborApiMetadata.Method[_]],
) extends CborApiFor[T] with TypedMetadata[T] {
  import CborApiMetadata._

  override def nameOpt: Opt[String] = nameInfo.rawName.opt
  def directSchema: CborSchema.Api = CborSchema.Api(methods.map(_.rawMethod))

  lazy val apiDependencies: IIterable[CborApiFor[_]] =
    methods.view.map(_.result)
      .collect {
        case MethodResultFor.Subapi(api) => api
      }
      .toList

  lazy val dataDependencies: IIterable[CborTypeFor[_]] =
    methods.flatMap(_.schemaDependencies)

  lazy val methodsByName: Map[String, Method[_]] =
    methods.toMapBy(_.nameInfo.rawName)
}
object CborApiMetadata extends RpcMetadataCompanion[CborApiMetadata] {
  @positioned(positioned.here)
  final class Method[T](
    @composite val nameInfo: NameInfo,
    @multi @rpcParamMetadata val params: List[CborField[_]],
    @infer val result: MethodResultFor[T],
  ) extends TypedMetadata[T] {
    def rawMethod: CborSchema.Method = CborSchema.Method(
      nameInfo.rawName,
      CborSchema.Record(params.map(_.rawField)),
      result.rawResult,
    )

    lazy val paramsByName: Map[String, CborField[_]] =
      params.toMapBy(_.nameInfo.rawName)

    def schemaDependencies: IIterable[CborTypeFor[_]] = {
      val fromParams = params.map(_.schemaFor)
      val fromResult = result.opt.collect({ case MethodResultFor.Call(schema, _) => schema })
      fromResult ++ fromParams
    }
  }

  sealed trait MethodResultFor[T] {
    def rawResult: CborSchema.MethodResult = this match {
      case MethodResultFor.Call(tpe, stream) => CborSchema.MethodResult.Call(tpe.schema, stream)
      case MethodResultFor.Subapi(api) => CborSchema.MethodResult.Subapi(api.schema)
    }
  }
  object MethodResultFor {
    final case class Call[T](schema: CborTypeFor[_], stream: Boolean) extends MethodResultFor[T]
    final case class Subapi[T](api: CborApiFor[T]) extends MethodResultFor[T]

    implicit def single[T: CborTypeFor]: MethodResultFor[Task[T]] =
      Call(CborTypeFor[T], stream = false)

    implicit def stream[T: CborTypeFor]: MethodResultFor[Observable[T]] =
      Call(CborTypeFor[T], stream = true)

    implicit def subapi[T: CborApiFor]: MethodResultFor[T] =
      Subapi(CborApiFor[T])
  }
}

trait CborApiInstances[T] {
  def asRawApi: AsRaw[RawCborApi, T]
  def asRealApi: AsReal[RawCborApi, T]
  def metadata: CborApiMetadata[T]
}

abstract class CborApiCompanion[T](implicit instances: MacroInstances[CborApiImplicits, CborApiInstances[T]]) {
  implicit lazy val asRawApi: AsRaw[RawCborApi, T] = instances(CborApiImplicits, this).asRawApi
  implicit lazy val asRealApi: AsReal[RawCborApi, T] = instances(CborApiImplicits, this).asRealApi
  implicit lazy val metadata: CborApiMetadata[T] = instances(CborApiImplicits, this).metadata
}
