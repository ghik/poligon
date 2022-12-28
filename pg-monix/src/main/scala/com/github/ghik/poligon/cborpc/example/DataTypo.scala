package com.github.ghik.poligon.cborpc
package example

import com.avsystem.commons.serialization.flatten
import com.avsystem.commons.serialization.json.JsonStringOutput
import monix.eval.Task
import monix.reactive.Observable

@flatten sealed trait DataTypo
object DataTypo extends CborAdtCompanion[DataTypo] {
  case class Sumfin(int: Int, list: List[Sumfin]) extends DataTypo
  object Sumfin extends CborAdtCompanion[Sumfin]
  case object Nuffin extends DataTypo
}

@flatten sealed trait DataTypoExt
object DataTypoExt extends CborAdtCompanion[DataTypoExt] {
  case class Sumfin(int: Int, list: List[String], moar: Boolean) extends DataTypoExt
  case object Nuffin extends DataTypoExt
}

trait Inyerface {
  def thingies: Thingies
}
object Inyerface extends CborApiCompanion[Inyerface]

trait Thingies {
  def moreThingies: Thingies

  def singleThingy(typo: DataTypo, bul: Boolean): Task[DataTypo]
  def manyThingies(idsy: List[String]): Observable[DataTypo]
}
object Thingies extends CborApiCompanion[Thingies]

object Testity {
  def main(args: Array[String]): Unit = {
    println(JsonStringOutput.writePretty(CborSchemas[Inyerface]))
  }
}
