package com.github.ghik.poligon

import com.avsystem.commons._

import java.util.concurrent.atomic.AtomicLong
import scala.annotation.tailrec

final class VarHandling extends HasVar {
  final val Iters = 10000000
  final val Threads = 8

  private[this] def iteration(tid: Int, i: Int): Unit = {
    val x: Long = HasVar.X.get(this)
    HasVar.X.getAndAdd(this, (i + 0xBACABACA): Long)
  }

  private[this] def thread(tid: Int): Unit = {
    @tailrec def loop(i: Int): Unit = if (i < Iters) {
      iteration(tid, i)
      loop(i + 1)
    }
    loop(0)
  }

  def run(): Unit = {
    while (true) {
      println("STARTING")
      val start = System.nanoTime()
      val dursum = new AtomicLong(0)
      val threads = (0 until Threads).map { tid =>
        new Thread({ () =>
          val start = System.nanoTime()
          thread(tid)
          dursum.addAndGet(System.nanoTime() - start)
        }).setup(_.start())
      }
      threads.foreach(_.join())
      val totaldur = System.nanoTime() - start
      val meandur = dursum.get() / Threads
      println(s"total duration: ${totaldur / 1000000}ms, mean duration: ${meandur / 1000000}ms")
      Thread.sleep(200)
    }
  }
}
object VarHandling {
  def main(args: Array[String]): Unit = {
    new VarHandling().run()
  }
}
