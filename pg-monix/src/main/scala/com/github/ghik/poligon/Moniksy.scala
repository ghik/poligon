package com.github.ghik.poligon

import cats.effect.ExitCode
import monix.eval.{Task, TaskApp, TaskLocal}
import monix.execution.misc.Local

object Moniksy extends TaskApp {
  final val numLocal = new Local(() => 0)

  val printNum: Task[Unit] = Task(println(numLocal.get)).executeAsync

  def withNum[T](num: Int)(task: Task[T]): Task[T] =
    TaskLocal.isolate(Task.defer {
      numLocal := num
      task
    }).executeWithOptions(_.enableLocalContextPropagation)

  def run(args: List[String]): Task[ExitCode] = for {
    _ <- printNum
    _ <- withNum(42)(printNum.startAndForget)
    _ <- withNum(69)(Task.parSequenceUnordered(Seq(printNum, printNum)))
  } yield ExitCode.Success
}
