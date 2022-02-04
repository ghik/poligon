package com.github.ghik.poligon

object PartialDecoding {
  def main(args: Array[String]): Unit = {
    println(stuff { str => s"$str thing" })
    println(stuff(thing = 1) { str => s"$str thing" })
  }


  def stuff[T](thing: Int = 42)(fun: String => T): T = fun(thing.toString)
  def stuff[T](fun: String => T): T = stuff()(fun)


}
