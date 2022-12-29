package com.github.ghik.poligon.cborpc.util

final case class Indexed[+T](
  value: T,
  index: Int,
)
object Indexed {
  def apply[T](t: (T, Int)): Indexed[T] = apply(t._1, t._2)
}
