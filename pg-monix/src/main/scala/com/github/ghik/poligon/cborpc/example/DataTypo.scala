package com.github.ghik.poligon.cborpc
package example

import com.avsystem.commons.serialization.cbor._
import com.avsystem.commons.serialization.{GenCodec, SizePolicy, flatten}
import com.github.ghik.poligon.cborpc.serialization.{SchemaAwareCborInput, SchemaAwareCborOutput}
import monix.eval.Task
import monix.reactive.Observable

import java.io.{ByteArrayOutputStream, DataOutputStream}

case class Thingy(int: Int, str: String)
object Thingy extends CborAdtCompanion[Thingy]

@flatten sealed trait DataTypo
object DataTypo extends CborAdtCompanion[DataTypo] {
  case class Sumfin(int: Int, list: List[DataTypo], mappy: Map[Thingy, Thingy]) extends DataTypo
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
    val tpe = CborTypeFor[DataTypo].schema
    val data = DataTypo.Sumfin(42, List(DataTypo.Nuffin), Map(Thingy(1, "fuu") -> Thingy(2, "fag")))

    val schemas = CborSchemas[DataTypo]
    val baos = new ByteArrayOutputStream
    val cborOutput = new CborOutput(new DataOutputStream(baos), CborKeyCodec.Default, SizePolicy.Required)
    val output = new SchemaAwareCborOutput(cborOutput, schemas, tpe)
    GenCodec[DataTypo].write(output, data)
    val rawCbor = RawCbor(baos.toByteArray)
    println(rawCbor)

    val cborInput = new CborInput(new CborReader(rawCbor), CborKeyCodec.Default)
    val input = new SchemaAwareCborInput(cborInput, schemas, tpe)
    val alsoData = GenCodec[DataTypo].read(input)

    println(alsoData)
  }
}
