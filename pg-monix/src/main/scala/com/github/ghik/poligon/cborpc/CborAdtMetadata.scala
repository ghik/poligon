package com.github.ghik.poligon.cborpc

import com.avsystem.commons._
import com.avsystem.commons.annotation.positioned
import com.avsystem.commons.meta._
import com.avsystem.commons.serialization._
import com.avsystem.commons.serialization.cbor.CborOptimizedCodecs

sealed trait CborAdtMetadata[T] extends CborTypeFor[T] with TypedMetadata[T] {
  protected def nameInfo: NameInfo

  final def name: String = nameInfo.rawName
  override final def nameOpt: Opt[String] = name.opt
}
object CborAdtMetadata extends AdtMetadataCompanion[CborAdtMetadata] {
  @positioned(positioned.here)
  final class Union[T](
    @reifyAnnot flatten: flatten,
    @composite val nameInfo: NameInfo,
    @multi @adtCaseMetadata val cases: List[UnionCase[_]],
  ) extends CborAdtMetadata[T] {
    def dependencies: IIterable[CborTypeFor[_]] = cases

    def directSchema: CborSchema.Union = CborSchema.Union(
      cases.map(_.rawCase),
      cases.findOpt(_.defaultCase).map(_.nameInfo.rawName)
    )
  }

  sealed trait UnionCase[T] extends CborAdtMetadata[T] {
    def nameInfo: NameInfo
    def defaultCase: Boolean
    def directSchema: CborSchema.Record

    final def rawCase: CborSchema.Case =
      CborSchema.Case(nameInfo.rawName)
  }

  @positioned(positioned.here)
  final class Record[T](
    @composite val nameInfo: NameInfo,
    @isAnnotated[defaultCase] val defaultCase: Boolean,
    @multi @adtParamMetadata val fields: List[CborField[_]],
  ) extends UnionCase[T] {
    def directSchema: CborSchema.Record =
      CborSchema.Record(fields.map(_.rawField))

    def dependencies: IIterable[CborTypeFor[_]] =
      fields.map(_.schemaFor)
  }

  @positioned(positioned.here)
  final class Singleton[T](
    @composite val nameInfo: NameInfo,
    @isAnnotated[defaultCase] val defaultCase: Boolean,
    @infer @checked val valueOf: ValueOf[T],
  ) extends UnionCase[T] {
    def directSchema: CborSchema.Record = CborSchema.Record(Nil)
    def dependencies: IIterable[CborTypeFor[_]] = Nil
  }
}

trait CborAdtInstances[T] {
  def codec: GenCodec[T]
  def metadata: CborAdtMetadata[T]
}

abstract class CborAdtCompanion[T](implicit
  instances: MacroInstances[CborOptimizedCodecs, CborAdtInstances[T]]
) {
  implicit lazy val codec: GenCodec[T] = instances(CborOptimizedCodecs, this).codec
  implicit lazy val metadata: CborAdtMetadata[T] = instances(CborOptimizedCodecs, this).metadata
}


