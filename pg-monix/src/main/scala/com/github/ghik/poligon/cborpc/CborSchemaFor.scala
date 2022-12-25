package com.github.ghik.poligon.cborpc

import com.avsystem.commons._
import com.avsystem.commons.annotation.positioned
import com.avsystem.commons.meta._
import com.avsystem.commons.misc.{Bytes, Timestamp}
import com.avsystem.commons.serialization._
import com.avsystem.commons.serialization.cbor.CborOptimizedCodecs

trait CborSchemaFor[T] {
  def schema: CborSchema

  final def forType[U]: CborSchemaFor[U] =
    this.asInstanceOf[CborSchemaFor[U]]
}

sealed trait CborAdtSchemaFor[T] extends CborSchemaFor[T] with TypedMetadata[T]
object CborAdtSchemaFor extends AdtMetadataCompanion[CborAdtSchemaFor] {
  case class NameInfo(
    @reifyName sourceName: String,
    @optional @reifyAnnot annotName: Opt[name],
  ) {
    def rawName: String = annotName.fold(sourceName)(_.name)
  }

  @positioned(positioned.here)
  final class Union[T](
    @reifyAnnot flatten: flatten,
    @multi @adtCaseMetadata cases: List[UnionCase[_]],
  ) extends CborAdtSchemaFor[T] {
    lazy val schema: CborSchema.Union = CborSchema.Union(
      cases.map(_.rawCase),
      cases.findOpt(_.defaultCase).map(_.nameInfo.rawName)
    )
  }

  sealed trait UnionCase[T] extends CborAdtSchemaFor[T] {
    def nameInfo: NameInfo
    def defaultCase: Boolean
    def schema: CborSchema.Record

    final def rawCase: CborSchema.Case = CborSchema.Case(nameInfo.rawName, schema)
  }

  @positioned(positioned.here)
  final class Record[T](
    @composite val nameInfo: NameInfo,
    @isAnnotated[defaultCase] val defaultCase: Boolean,
    @multi @adtParamMetadata fields: List[Field[_]]
  ) extends UnionCase[T] {
    lazy val schema: CborSchema.Record = CborSchema.Record(fields.map(_.rawField))
  }

  @positioned(positioned.here)
  final class Singleton[T](
    @composite val nameInfo: NameInfo,
    @isAnnotated[defaultCase] val defaultCase: Boolean,
    @infer @checked valueOf: ValueOf[T]
  ) extends UnionCase[T] {
    def schema: CborSchema.Record = CborSchema.Record(Nil)
  }

  final class Field[T](
    @composite nameInfo: NameInfo,
    @isAnnotated[optionalParam] optional: Boolean,
    @isAnnotated[whenAbsent[T]] hasWhenAbsent: Boolean,
    @isAnnotated[transientDefault] transientDefault: Boolean,
    @reifyFlags flags: ParamFlags,
    @infer dataType: CborSchemaFor[T],
  ) extends TypedMetadata[T] {

    def rawField: CborSchema.Field = CborSchema.Field(
      nameInfo.rawName,
      dataType.schema,
      optional,
      flags.hasDefaultValue || hasWhenAbsent,
      transientDefault,
    )
  }
}

trait CborAdtInstances[T] {
  def codec: GenCodec[T]
  def schemaFor: CborAdtSchemaFor[T]
}

abstract class CborAdtCompanion[T](implicit
  instances: MacroInstances[CborOptimizedCodecs, CborAdtInstances[T]]
) {
  implicit lazy val codec: GenCodec[T] = instances(CborOptimizedCodecs, this).codec
  implicit lazy val metadata: CborAdtSchemaFor[T] = instances(CborOptimizedCodecs, this).schemaFor
}

object CborSchemaFor {
  def apply[T](implicit dt: CborSchemaFor[T]): CborSchemaFor[T] = dt

  def plain[T](theSchema: CborSchema): CborSchemaFor[T] =
    new CborSchemaFor[T] {
      def schema: CborSchema = theSchema
    }

  implicit val UnitType: CborSchemaFor[Unit] = plain(CborSchema.Primitive(CborType.Null))
  implicit val BooleanType: CborSchemaFor[Boolean] = plain(CborSchema.Primitive(CborType.Boolean))
  implicit val CharType: CborSchemaFor[Char] = plain(CborSchema.Primitive(CborType.Char))
  implicit val ByteType: CborSchemaFor[Byte] = plain(CborSchema.Primitive(CborType.Byte))
  implicit val ShortType: CborSchemaFor[Short] = plain(CborSchema.Primitive(CborType.Short))
  implicit val IntType: CborSchemaFor[Int] = plain(CborSchema.Primitive(CborType.Int))
  implicit val LongType: CborSchemaFor[Long] = plain(CborSchema.Primitive(CborType.Long))
  implicit val FloatType: CborSchemaFor[Float] = plain(CborSchema.Primitive(CborType.Float))
  implicit val DoubleType: CborSchemaFor[Double] = plain(CborSchema.Primitive(CborType.Double))
  implicit val StringType: CborSchemaFor[String] = plain(CborSchema.Primitive(CborType.String))
  implicit val TimestampType: CborSchemaFor[Timestamp] = plain(CborSchema.Primitive(CborType.Timestamp))
  implicit val ByteArrayType: CborSchemaFor[Array[Byte]] = plain(CborSchema.Primitive(CborType.Binary))
  implicit val BytesType: CborSchemaFor[Bytes] = plain(CborSchema.Primitive(CborType.Binary))

  implicit def optionType[T: CborSchemaFor]: CborSchemaFor[Option[T]] =
    plain(CborSchema.Nullable(CborSchemaFor[T].schema))

  implicit def optType[T: CborSchemaFor]: CborSchemaFor[Opt[T]] =
    plain(CborSchema.Nullable(CborSchemaFor[T].schema))

  implicit def optArgType[T: CborSchemaFor]: CborSchemaFor[OptArg[T]] =
    plain(CborSchema.Nullable(CborSchemaFor[T].schema))

  implicit def seqType[C[X] <: BSeq[X], T: CborSchemaFor]: CborSchemaFor[C[T]] =
    plain(CborSchema.Collection(CborSchemaFor[T].schema))

  implicit def setType[C[X] <: BSet[X], T: CborSchemaFor]: CborSchemaFor[C[T]] =
    plain(CborSchema.Collection(CborSchemaFor[T].schema))

  implicit def mapType[M[X, Y] <: BMap[X, Y], K: CborSchemaFor, V: CborSchemaFor]: CborSchemaFor[M[K, V]] =
    plain(CborSchema.Dictionary(CborSchemaFor[K].schema, CborSchemaFor[V].schema))

  implicit def transparentWrapperType[R, T](implicit
    tw: TransparentWrapping[R, T], wrappedType: CborSchemaFor[R]
  ): CborSchemaFor[T] = wrappedType.forType[T]
}
