package com.github.ghik.poligon

final class FalseSharing {
  @volatile private[this] var i: Int = 0

  private[this] var p00: Int = _
  private[this] var p01: Int = _
  private[this] var p02: Int = _
  private[this] var p03: Int = _
  private[this] var p04: Int = _
  private[this] var p05: Int = _
  private[this] var p06: Int = _
  private[this] var p07: Int = _
  private[this] var p08: Int = _
  private[this] var p09: Int = _
  private[this] var p10: Int = _
  private[this] var p11: Int = _
  private[this] var p12: Int = _
  private[this] var p13: Int = _
  private[this] var p14: Int = _
  private[this] var p15: Int = _

  @volatile private[this] var j: Int = 0

  private def increment(): Unit =
    while (i < FalseSharing.Its) {
      i += 1
    }

  private def jncrement(): Unit =
    while (j < FalseSharing.Its) {
      j += 1
    }

  def run(): Unit = {
    val it = new Thread(() => increment())
    val jt = new Thread(() => jncrement())

    val startTime = System.nanoTime()
    it.start()
    jt.start()
    it.join()
    jt.join()
    val duration = System.nanoTime() - startTime
    println(s"Took ${duration / 1000000}ms")
  }
}
object FalseSharing {
  final val Its = 1024 * 1024 * 128

  def main(args: Array[String]): Unit = {
    new FalseSharing().run()
  }
}
