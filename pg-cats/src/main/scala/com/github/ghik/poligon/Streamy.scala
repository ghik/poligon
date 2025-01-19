package com.github.ghik.poligon

import cats.effect._
import fs2._
import fs2.concurrent.Topic

import scala.collection.mutable

object Streamy extends IOApp.Simple {

  val q = new mutable.Queue[String]
  val ts = new mutable.TreeSet[String]

  Topic[IO, String].flatMap { topic =>
    val publisher = Stream.constant("1").covary[IO].through(topic.publish)
    val subscriber = topic.subscribe(10).take(4)
    subscriber.concurrently(publisher).compile.toVector
  }

  def repeat[F[_], A](stream: Stream[F, A]): Stream[F, A] =
    stream ++ repeat(stream)

  def drain[F[_], A](stream: Stream[F, A]): Stream[F, Nothing] =
    stream.chunks.flatMap(_ => Stream.empty)

  override def run: IO[Unit] =
    repeat[IO, Int](Stream(1, 2, 3)).take(10).compile.toList.flatTap(IO.println(_)).void
}
