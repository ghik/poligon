package com.github.ghik.poligon.cborpc

import com.avsystem.commons._
import com.avsystem.commons.annotation.positioned
import com.avsystem.commons.meta._
import com.avsystem.commons.misc.{Bytes, Timestamp}
import com.avsystem.commons.serialization._
import com.avsystem.commons.serialization.cbor.{CborOptimizedCodecs, RawCbor}

trait CborTypeFor[T] { self =>
  def dependencies: IIterable[CborTypeFor[_]]
  def directType: DirectCborType
  def nameOpt: Opt[String] = Opt.Empty

  final def cborType: CborType =
    nameOpt.mapOr(directType, CborSchema.Reference)

  final def forType[U]: CborTypeFor[U] =
    this.asInstanceOf[CborTypeFor[U]]

  final def map[U](f: CborType => DirectCborType): CborTypeFor[U] =
    new CborTypeFor[U] {
      def dependencies: IIterable[CborTypeFor[_]] = self.dependencies
      def directType: DirectCborType = f(self.cborType)
    }
}

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

    def directType: CborSchema.Union = CborSchema.Union(
      cases.map(_.rawCase),
      cases.findOpt(_.defaultCase).map(_.nameInfo.rawName)
    )
  }

  sealed trait UnionCase[T] extends CborAdtMetadata[T] {
    def nameInfo: NameInfo
    def defaultCase: Boolean
    def directType: CborSchema.Record

    final def rawCase: CborSchema.Case =
      CborSchema.Case(nameInfo.rawName, directType)
  }

  @positioned(positioned.here)
  final class Record[T](
    @composite val nameInfo: NameInfo,
    @isAnnotated[defaultCase] val defaultCase: Boolean,
    @multi @adtParamMetadata val fields: List[CborField[_]],
  ) extends UnionCase[T] {
    def directType: CborSchema.Record =
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
    def directType: CborSchema.Record = CborSchema.Record(Nil)
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

object CborTypeFor {
  def apply[T](implicit dt: CborTypeFor[T]): CborTypeFor[T] = dt

  def primitive[T](tpe: CborPrimitiveType): CborTypeFor[T] =
    new CborTypeFor[T] {
      def directType: DirectCborType = CborSchema.Primitive(tpe)
      def dependencies: IIterable[CborTypeFor[_]] = Nil
    }

  implicit val UnitType: CborTypeFor[Unit] = primitive(CborPrimitiveType.Null)
  implicit val BooleanType: CborTypeFor[Boolean] = primitive(CborPrimitiveType.Boolean)
  implicit val CharType: CborTypeFor[Char] = primitive(CborPrimitiveType.Char)
  implicit val ByteType: CborTypeFor[Byte] = primitive(CborPrimitiveType.Byte)
  implicit val ShortType: CborTypeFor[Short] = primitive(CborPrimitiveType.Short)
  implicit val IntType: CborTypeFor[Int] = primitive(CborPrimitiveType.Int)
  implicit val LongType: CborTypeFor[Long] = primitive(CborPrimitiveType.Long)
  implicit val FloatType: CborTypeFor[Float] = primitive(CborPrimitiveType.Float)
  implicit val DoubleType: CborTypeFor[Double] = primitive(CborPrimitiveType.Double)
  implicit val StringType: CborTypeFor[String] = primitive(CborPrimitiveType.String)
  implicit val TimestampType: CborTypeFor[Timestamp] = primitive(CborPrimitiveType.Timestamp)
  implicit val ByteArrayType: CborTypeFor[Array[Byte]] = primitive(CborPrimitiveType.Binary)
  implicit val BytesType: CborTypeFor[Bytes] = primitive(CborPrimitiveType.Binary)
  implicit val RawType: CborTypeFor[RawCbor] = primitive(CborPrimitiveType.Raw)

  implicit def optionType[T: CborTypeFor]: CborTypeFor[Option[T]] =
    CborTypeFor[T].map(CborSchema.Nullable)

  implicit def optType[T: CborTypeFor]: CborTypeFor[Opt[T]] =
    CborTypeFor[T].map(CborSchema.Nullable)

  implicit def optArgType[T: CborTypeFor]: CborTypeFor[OptArg[T]] =
    CborTypeFor[T].map(CborSchema.Nullable)

  implicit def seqType[C[X] <: BSeq[X], T: CborTypeFor]: CborTypeFor[C[T]] =
    CborTypeFor[T].map(CborSchema.Collection)

  implicit def setType[C[X] <: BSet[X], T: CborTypeFor]: CborTypeFor[C[T]] =
    CborTypeFor[T].map(CborSchema.Collection)

  def map2[A: CborTypeFor, B: CborTypeFor, C](
    f: (CborType, CborType) => DirectCborType
  ): CborTypeFor[C] = new CborTypeFor[C] {
    def dependencies: IIterable[CborTypeFor[_]] =
      List(CborTypeFor[A], CborTypeFor[B])
    def directType: DirectCborType =
      f(CborTypeFor[A].cborType, CborTypeFor[B].cborType)
  }

  implicit def mapType[M[X, Y] <: BMap[X, Y], K: CborTypeFor, V: CborTypeFor]: CborTypeFor[M[K, V]] =
    map2[K, V, M[K, V]](CborSchema.Dictionary)

  implicit def transparentWrapperType[R, T](implicit
    tw: TransparentWrapping[R, T], wrappedType: CborTypeFor[R]
  ): CborTypeFor[T] = wrappedType.forType[T]
}
