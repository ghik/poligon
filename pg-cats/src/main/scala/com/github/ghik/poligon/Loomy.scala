package com.github.ghik.poligon

import java.util.concurrent.Executors

object Loomy {
  def main(args: Array[String]): Unit = {
    val ex = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())

    for (_ <- 0 until 1024) {
      ex.submit({ () => runStuff() }: Runnable)
    }

    Thread.sleep(35000)
  }

  private def runStuff(): Unit = {
    Thread.sleep(30000)
    runMore()
  }

  private def runMore(): Unit = {
    Thread.sleep(1000)
    println(Thread.currentThread())
  }
}
