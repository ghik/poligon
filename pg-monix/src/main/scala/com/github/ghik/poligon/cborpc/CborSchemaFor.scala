package com.github.ghik.poligon.cborpc

import com.avsystem.commons._
import com.avsystem.commons.annotation.positioned
import com.avsystem.commons.meta._
import com.avsystem.commons.misc.{Bytes, Timestamp}
import com.avsystem.commons.serialization._
import com.avsystem.commons.serialization.cbor.{CborOptimizedCodecs, RawCbor}
import com.github.ghik.poligon.cborpc.CborAdtSchemaFor.NameInfo

trait CborSchemaFor[T] { self =>
  def dependencies: IIterable[CborSchemaFor[_]]
  def directSchema: DirectCborSchema
  def nameOpt: Opt[String] = Opt.Empty

  final def schema: CborSchema =
    nameOpt.mapOr(directSchema, CborSchema.Reference)

  final def forType[U]: CborSchemaFor[U] =
    this.asInstanceOf[CborSchemaFor[U]]

  final def map[U](f: CborSchema => DirectCborSchema): CborSchemaFor[U] =
    new CborSchemaFor[U] {
      def dependencies: IIterable[CborSchemaFor[_]] = self.dependencies
      def directSchema: DirectCborSchema = f(self.schema)
    }
}

sealed trait CborAdtSchemaFor[T] extends CborSchemaFor[T] with TypedMetadata[T] {
  protected def nameInfo: NameInfo

  final def name: String = nameInfo.rawName
  override final def nameOpt: Opt[String] = name.opt
}
object CborAdtSchemaFor extends AdtMetadataCompanion[CborAdtSchemaFor] {
  final case class NameInfo(
    @reifyName sourceName: String,
    @optional @reifyAnnot annotName: Opt[name],
  ) {
    def rawName: String = annotName.fold(sourceName)(_.name)
  }

  @positioned(positioned.here)
  final class Union[T](
    @reifyAnnot flatten: flatten,
    @composite val nameInfo: NameInfo,
    @multi @adtCaseMetadata val cases: List[UnionCase[_]],
  ) extends CborAdtSchemaFor[T] {
    def dependencies: IIterable[CborSchemaFor[_]] = cases

    def directSchema: CborSchema.Union = CborSchema.Union(
      cases.map(_.rawCase),
      cases.findOpt(_.defaultCase).map(_.nameInfo.rawName)
    )
  }

  sealed trait UnionCase[T] extends CborAdtSchemaFor[T] {
    def nameInfo: NameInfo
    def defaultCase: Boolean
    def directSchema: CborSchema.Record

    final def rawCase: CborSchema.Case =
      CborSchema.Case(nameInfo.rawName, directSchema)
  }

  @positioned(positioned.here)
  final class Record[T](
    @composite val nameInfo: NameInfo,
    @isAnnotated[defaultCase] val defaultCase: Boolean,
    @multi @adtParamMetadata val fields: List[Field[_]],
  ) extends UnionCase[T] {
    def directSchema: CborSchema.Record =
      CborSchema.Record(fields.map(_.rawField))

    def dependencies: IIterable[CborSchemaFor[_]] =
      fields.map(_.schemaFor)
  }

  @positioned(positioned.here)
  final class Singleton[T](
    @composite val nameInfo: NameInfo,
    @isAnnotated[defaultCase] val defaultCase: Boolean,
    @infer @checked val valueOf: ValueOf[T],
  ) extends UnionCase[T] {
    def directSchema: CborSchema.Record = CborSchema.Record(Nil)
    def dependencies: IIterable[CborSchemaFor[_]] = Nil
  }

  final class Field[T](
    @composite val nameInfo: NameInfo,
    @isAnnotated[optionalParam] optional: Boolean,
    @isAnnotated[whenAbsent[T]] hasWhenAbsent: Boolean,
    @isAnnotated[transientDefault] transientDefault: Boolean,
    @reifyFlags flags: ParamFlags,
    @infer val schemaFor: CborSchemaFor[T],
  ) extends TypedMetadata[T] {

    def rawField: CborSchema.Field = CborSchema.Field(
      nameInfo.rawName,
      schemaFor.schema,
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

  def primitive[T](tpe: CborType): CborSchemaFor[T] =
    new CborSchemaFor[T] {
      def directSchema: DirectCborSchema = CborSchema.Primitive(tpe)
      def dependencies: IIterable[CborSchemaFor[_]] = Nil
    }

  implicit val UnitType: CborSchemaFor[Unit] = primitive(CborType.Null)
  implicit val BooleanType: CborSchemaFor[Boolean] = primitive(CborType.Boolean)
  implicit val CharType: CborSchemaFor[Char] = primitive(CborType.Char)
  implicit val ByteType: CborSchemaFor[Byte] = primitive(CborType.Byte)
  implicit val ShortType: CborSchemaFor[Short] = primitive(CborType.Short)
  implicit val IntType: CborSchemaFor[Int] = primitive(CborType.Int)
  implicit val LongType: CborSchemaFor[Long] = primitive(CborType.Long)
  implicit val FloatType: CborSchemaFor[Float] = primitive(CborType.Float)
  implicit val DoubleType: CborSchemaFor[Double] = primitive(CborType.Double)
  implicit val StringType: CborSchemaFor[String] = primitive(CborType.String)
  implicit val TimestampType: CborSchemaFor[Timestamp] = primitive(CborType.Timestamp)
  implicit val ByteArrayType: CborSchemaFor[Array[Byte]] = primitive(CborType.Binary)
  implicit val BytesType: CborSchemaFor[Bytes] = primitive(CborType.Binary)
  implicit val RawType: CborSchemaFor[RawCbor] = primitive(CborType.Raw)

  implicit def optionType[T: CborSchemaFor]: CborSchemaFor[Option[T]] =
    CborSchemaFor[T].map(CborSchema.Nullable)

  implicit def optType[T: CborSchemaFor]: CborSchemaFor[Opt[T]] =
    CborSchemaFor[T].map(CborSchema.Nullable)

  implicit def optArgType[T: CborSchemaFor]: CborSchemaFor[OptArg[T]] =
    CborSchemaFor[T].map(CborSchema.Nullable)

  implicit def seqType[C[X] <: BSeq[X], T: CborSchemaFor]: CborSchemaFor[C[T]] =
    CborSchemaFor[T].map(CborSchema.Collection)

  implicit def setType[C[X] <: BSet[X], T: CborSchemaFor]: CborSchemaFor[C[T]] =
    CborSchemaFor[T].map(CborSchema.Collection)

  def map2[A: CborSchemaFor, B: CborSchemaFor, C](
    f: (CborSchema, CborSchema) => DirectCborSchema
  ): CborSchemaFor[C] = new CborSchemaFor[C] {
    def dependencies: IIterable[CborSchemaFor[_]] =
      List(CborSchemaFor[A], CborSchemaFor[B])
    def directSchema: DirectCborSchema =
      f(CborSchemaFor[A].schema, CborSchemaFor[B].schema)
  }

  implicit def mapType[M[X, Y] <: BMap[X, Y], K: CborSchemaFor, V: CborSchemaFor]: CborSchemaFor[M[K, V]] =
    map2[K, V, M[K, V]](CborSchema.Dictionary)

  implicit def transparentWrapperType[R, T](implicit
    tw: TransparentWrapping[R, T], wrappedType: CborSchemaFor[R]
  ): CborSchemaFor[T] = wrappedType.forType[T]
}
