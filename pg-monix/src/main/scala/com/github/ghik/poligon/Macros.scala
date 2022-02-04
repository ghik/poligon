package com.github.ghik.poligon

object Macros {
  def sourcePosition: String = macro macros.RandomMacros.sourcePositionImpl

  def methodCount[T]: Int = macro macros.RandomMacros.methodCountImpl[T]

  def main(args: Array[String]): Unit = {
    println(methodCount[String])
  }
}