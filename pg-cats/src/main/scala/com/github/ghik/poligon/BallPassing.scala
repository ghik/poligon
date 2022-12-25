package com.github.ghik.poligon

import scala.annotation.tailrec

final class Ball {
  var count = 0
}

final class Player(players: Array[Player], i: Int) extends BallHandle {

  private[this] val nextPlayerIdx = (i + 1) % players.length
  private def nextPlayer: Player = players(nextPlayerIdx)

  override def run(): Unit = {
    @tailrec def loop(step: Long): Unit = readStep() match {
      case `step` =>
        Thread.onSpinWait()
        loop(step)
      case step if step < 10_000_000 =>
        val ns = step + 1
        nextPlayer.nextStep(ns)
        loop(ns)
      case step =>
        nextPlayer.nextStep(step + 1)
    }
    val start = System.nanoTime()
    loop(0)
    val dur = System.nanoTime() - start
    println(s"Player $i finished in ${dur / 1000000}ms")
  }
}

object BallPassing {
  def main(args: Array[String]): Unit = {
    while (true) {
      val players = new Array[Player](8)
      for (i <- players.indices) {
        players(i) = new Player(players, i)
      }
      players(0).nextStep(1)
      players.foreach(_.start())
      players.foreach(_.join())
    }
  }
}
