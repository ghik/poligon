package com.github.ghik.poligon.cborpc

import com.avsystem.commons.serialization.flatten

@flatten sealed trait DataTypo
object DataTypo extends CborAdtCompanion[DataTypo] {
  case class Sumfin(int: Int, list: List[String]) extends DataTypo
  case object Nuffin extends DataTypo
}

@flatten sealed trait DataTypoExt
object DataTypoExt extends CborAdtCompanion[DataTypoExt] {
  case class Sumfin(int: Int, list: List[String], moar: Boolean) extends DataTypoExt
  case object Nuffin extends DataTypoExt
}

object Testity {
  def main(args: Array[String]): Unit = {
    //    println(JsonStringOutput.writePretty(DataTypo.metadata.schema))
    println(DataTypoExt.metadata.schema.canRead(DataTypo.metadata.schema))
  }
}
