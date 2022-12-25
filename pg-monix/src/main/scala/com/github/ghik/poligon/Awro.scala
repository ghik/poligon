package com.github.ghik.poligon

import com.avsystem.commons._
import com.sksamuel.avro4s.{Encoder, SchemaFor}
import org.apache.avro.Resolver
import org.apache.avro.Resolver._

case class Rekord[T](
  int: Int,
  list: List[T],
)
object Rekord {
  implicit def schema[T: SchemaFor]: SchemaFor[Rekord[T]] = SchemaFor.gen[Rekord[T]]
  implicit def encoder[T: Encoder]: Encoder[Rekord[T]] = Encoder.gen[Rekord[T]]
}

case class RecW(
  thing: Int
)
case class RecR(
  thingy: Int
)

object Awro {
  def resolution(action: Action): String = action match {
    case skip: Skip =>
      s"skip(${skip.writer})"
    case doNothing: DoNothing =>
      s"doNothing(${doNothing.writer})"
    case promote: Promote =>
      s"promote(${promote.writer}->${promote.reader})"
    case error: ErrorAction =>
      s"error[${error.error}]($error)"
    case container: Container =>
      s"container(${resolution(container.elementAction)})"
    case recordAdjust: RecordAdjust =>
      val fields = recordAdjust.fieldActions.iterator
        .zip(recordAdjust.writer.getFields.asScala.iterator)
        .map { case (action, field) =>
          s"${field.name}: ${resolution(action)}".indent(2)
        }
        .mkString("\n")
      s"recordAdjust(\n$fields)"
    case _ =>
      throw new Exception("źle")
  }

  def main(args: Array[String]): Unit = {
    val s1 = SchemaFor.gen[RecW].schema
    val s2 = SchemaFor.gen[RecR].schema
    println(resolution(Resolver.resolve(s1, s2)))
  }
}
