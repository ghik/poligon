package com.github.ghik.poligon

object Deps {
  given Config = Config("deps")
}

final case class Klas(int: Int)
object Klas {
  given codec: MCodec[Klas] = MCodec.derivedWithDeps[Deps.type, Klas]
  
  def main(args: Array[String]): Unit = {
    println(codec.config)
  }
}
