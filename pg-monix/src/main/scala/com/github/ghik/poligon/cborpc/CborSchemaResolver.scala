package com.github.ghik.poligon.cborpc

import com.avsystem.commons._

trait CborSchemaResolver {
  def registeredNames: Set[String]
  def resolve(name: String): DirectCborSchema

  final def resolve(schema: CborSchema): DirectCborSchema = schema match {
    case CborSchema.Reference(name) => resolve(name)
    case d: DirectCborSchema => d
  }
}

trait CborSchemaRegistry {
  def register(name: String, schema: DirectCborSchema): Boolean
}

class SimpleCborSchemaRegistry extends CborSchemaResolver with CborSchemaRegistry {
  private val registry = new MHashMap[String, DirectCborSchema]

  lazy val registeredNames: Set[String] = registry.keys.toSet

  def resolve(name: String): DirectCborSchema =
    registry.getOrElse(name, throw new NoSuchElementException(s"No CborSchema named $name in registry"))

  def register(name: String, schema: DirectCborSchema): Boolean =
    registry.get(name) match {
      case None =>
        registry(name) = schema
        true
      case Some(prev) if prev == schema =>
        false
      case Some(_) =>
        throw new IllegalStateException(s"Multiple different CborSchemas with name $name detected")
    }
}
