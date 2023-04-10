package com.github.ghik.poligon

import com.avsystem.commons._
import org.w3c.dom.Element

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object Xmle {
  case class ResourceDef(
    id: Int,
    name: String,
    operations: String,
    multipleInstances: Boolean,
    mandatory: Boolean,
    tpe: String,
    range: Opt[Range],
    description: String,
  ) {
    override def toString: String = {
      val mand = if(mandatory) "*" else ""
      val multi = if(multipleInstances) "+" else ""
      val ran = range.fold("")(r => s"(${r.start}..${r.end})")
      s"$id$mand$multi[$tpe$ran,$operations]: $name"
    }
  }

  def main(args: Array[String]): Unit = {
    val dbf = DocumentBuilderFactory.newInstance()
    val db = dbf.newDocumentBuilder()
    val doc = db.parse(new File("mcfota.xml"))
    val items = doc.getElementsByTagName("Item")
    val resourceDefs = (0 until items.getLength)
      .map(i => items.item(i).asInstanceOf[Element])
      .map { el =>
        ResourceDef(
          el.getAttribute("ID").toInt,
          el.getElementsByTagName("Name").item(0).getTextContent,
          el.getElementsByTagName("Operations").item(0).getTextContent,
          el.getElementsByTagName("MultipleInstances").item(0).getTextContent == "Multiple",
          el.getElementsByTagName("Mandatory").item(0).getTextContent == "Mandatory",
          el.getElementsByTagName("Type").item(0).getTextContent,
          el.getElementsByTagName("RangeEnumeration").item(0)
            .getTextContent.opt.filter(_.nonEmpty)
            .map(_.split("\\.\\.").map(_.toInt))
            .map(a => a(0) to a(1)), ""
//          el.getElementsByTagName("Description").item(0).getTextContent
        )
      }

    resourceDefs.foreach(println)
  }
}
