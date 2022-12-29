package com.github.ghik.poligon.cborpc

import com.avsystem.commons._
import com.avsystem.commons.jiop.JFactory
import com.avsystem.commons.serialization.GenCodec.ReadFailure
import com.avsystem.commons.serialization.{GenCodec, Input, Output, TypeMarker}

import scala.collection.compat.Factory

trait SchemaBasedCborImplicits {
  implicit def cborMapCodec[M[X, Y] <: BMap[X, Y], K: GenCodec, V: GenCodec](
    implicit fac: Factory[(K, V), M[K, V]]
  ): GenCodec[M[K, V]] = mkMapCodec(identity)

  implicit def cborJMapCodec[M[X, Y] <: JMap[X, Y], K: GenCodec, V: GenCodec](
    implicit fac: JFactory[(K, V), M[K, V]]
  ): GenCodec[M[K, V]] = mkMapCodec(_.asScala)

  private def mkMapCodec[M[X, Y] <: AnyRef, K: GenCodec, V: GenCodec](
    toIterable: M[K, V] => BIterable[(K, V)]
  )(implicit
    fac: Factory[(K, V), M[K, V]]
  ): GenCodec[M[K, V]] = new GenCodec[M[K, V]] {
    private val marker = SchemaAwareCborMap[K, V, M[K, V]](GenCodec[K], GenCodec[V], toIterable, fac)

    def read(input: Input): M[K, V] =
      input.readCustom(marker).getOrElse(throw new ReadFailure("gtfo")) //TODO

    def write(output: Output, value: M[K, V]): Unit =
      if (!output.writeCustom(marker, value)) {
        throw new ReadFailure("gtfo") // TODO
      }
  }
}
object SchemaBasedCborImplicits extends SchemaBasedCborImplicits

case class SchemaAwareCborMap[K, V, M](
  keyCodec: GenCodec[K],
  valueCodec: GenCodec[V],
  toIterable: M => BIterable[(K, V)],
  factory: Factory[(K, V), M],
) extends TypeMarker[M]
