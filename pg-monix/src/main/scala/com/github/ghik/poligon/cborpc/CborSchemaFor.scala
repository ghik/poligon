package com.github.ghik.poligon.cborpc

import com.avsystem.commons._
import com.avsystem.commons.misc.{Bytes, Timestamp}
import com.avsystem.commons.serialization.TransparentWrapping
import com.avsystem.commons.serialization.cbor.RawCbor

sealed trait CborSchemaFor[T] {
  def directSchema: DirectCborSchema
  def dependencies: IIterable[CborSchemaFor[_]]

  def schema: CborSchema
  def nameOpt: Opt[String] = Opt.Empty
}
object CborSchemaFor {
  def apply[T](implicit schema: CborSchemaFor[T]): CborSchemaFor[T] = schema
}

trait CborTypeFor[T] extends CborSchemaFor[T] { self =>
  def dependencies: IIterable[CborTypeFor[_]]
  def directSchema: DirectCborType

  final def schema: CborType =
    nameOpt.mapOr(directSchema, CborSchema.Reference)

  final def forType[U]: CborTypeFor[U] =
    this.asInstanceOf[CborTypeFor[U]]

  final def map[U](f: CborType => DirectCborType): CborTypeFor[U] =
    new CborTypeFor[U] {
      def dependencies: IIterable[CborTypeFor[_]] = self.dependencies
      def directSchema: DirectCborType = f(self.schema)
    }
}
object CborTypeFor {
  def apply[T](implicit dt: CborTypeFor[T]): CborTypeFor[T] = dt

  def primitive[T](tpe: CborPrimitiveType): CborTypeFor[T] =
    new CborTypeFor[T] {
      def directSchema: DirectCborType = CborSchema.Primitive(tpe)
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
    def directSchema: DirectCborType =
      f(CborTypeFor[A].schema, CborTypeFor[B].schema)
  }

  implicit def mapType[M[X, Y] <: BMap[X, Y], K: CborTypeFor, V: CborTypeFor]: CborTypeFor[M[K, V]] =
    map2[K, V, M[K, V]](CborSchema.Dictionary)

  implicit def transparentWrapperType[R, T](implicit
    tw: TransparentWrapping[R, T], wrappedType: CborTypeFor[R]
  ): CborTypeFor[T] = wrappedType.forType[T]
}

trait CborApiFor[T] extends CborSchemaFor[T] {
  def directSchema: DirectCborApi

  final def schema: CborApi =
    nameOpt.mapOr(directSchema, CborSchema.Reference)

  def apiDependencies: IIterable[CborApiFor[_]]
  def dataDependencies: IIterable[CborTypeFor[_]]

  final def dependencies: IIterable[CborSchemaFor[_]] = apiDependencies ++ dataDependencies
}
object CborApiFor {
  def apply[T](implicit api: CborApiFor[T]): CborApiFor[T] = api
}
