package com.github.ghik.poligon

import monix.eval.Task

object Moniksy {
  def doMoar(cos: Int): Task[Unit] = ???

  Task.defer {
    val cos = Console.in.read()
    doMoar(cos)
  }
}
