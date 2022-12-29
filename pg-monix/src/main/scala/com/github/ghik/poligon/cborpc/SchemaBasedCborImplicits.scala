package com.github.ghik.poligon.cborpc

import com.avsystem.commons.jiop.JFactory
import com.avsystem.commons.serialization.GenCodec.ReadFailure
import com.avsystem.commons.serialization.{GenCodec, Input, Output, TypeMarker}
import com.avsystem.commons.{BIterable, BMap, JMap}

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
    def read(input: Input): M[K, V] =
      input.readCustom(SchemaAwareCborMap[K, V]()).map(_.to(fac))
        .getOrElse(throw new ReadFailure("gtfo")) //TODO

    def write(output: Output, value: M[K, V]): Unit =
      if (!output.writeCustom(SchemaAwareCborMap[K, V](), toIterable(value))) {
        throw new ReadFailure("gtfo") // TODO
      }
  }
}
object SchemaBasedCborImplicits extends SchemaBasedCborImplicits

case class SchemaAwareCborMap[K: GenCodec, V: GenCodec]() extends TypeMarker[BIterable[(K, V)]] {
  def keyCodec: GenCodec[K] = GenCodec[K]
  def valueCodec: GenCodec[V] = GenCodec[V]
}
