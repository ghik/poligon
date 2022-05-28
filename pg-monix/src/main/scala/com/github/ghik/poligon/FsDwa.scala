package com.github.ghik.poligon

import monix.eval.Task

object FsDwa {
  def main(args: Array[String]): Unit = {
    val task = fs2.Stream.fromIterator[Task](Iterator(1, 2, 3)).evalTap(i => Task(println(i))).compile.drain

    import monix.execution.Scheduler.Implicits.global
    task.runSyncUnsafe()
  }
}
