package com.github.ghik.poligon.cborpc

import com.avsystem.commons._
import com.avsystem.commons.serialization.TransparentWrapperCompanion

final case class CborSchemas(registry: Map[String, DirectCborSchema]) {
  private def resolveSchema[T <: CborSchema, D <: T with DirectCborSchema : ClassTag](schema: T): D = schema match {
    case CborSchema.Reference(name) =>
      registry.get(name).collect({ case d: D => d })
        .getOrElse(throw new NoSuchElementException(s"No registered ${classTag[D].runtimeClass.getSimpleName} named $name"))
    case d: D => d
  }

  def resolveType(tpe: CborType): DirectCborType =
    resolveSchema[CborType, DirectCborType](tpe)

  def resolveApi(api: CborApi): DirectCborApi =
    resolveSchema[CborApi, DirectCborApi](api)
}

object CborSchemas extends TransparentWrapperCompanion[Map[String, DirectCborSchema], CborSchemas] {
  def apply[T: CborSchemaFor]: CborSchemas = {
    val tmpRegistry = new MHashMap[String, DirectCborSchema]

    def traverse(schema: CborSchemaFor[_]): Unit = schema.nameOpt match {
      case Opt(name) =>
        val directSchema = schema.directSchema
        tmpRegistry.get(name) match {
          case None =>
            tmpRegistry(name) = directSchema // MUST happen BEFORE traversing dependencies to handle recursive types
            schema.dependencies.foreach(traverse)
          case Some(`directSchema`) => // already registered, do nothing
          case Some(_) => throw new IllegalStateException(s"Multiple conflicting schemas named $name detected")
        }
      case Opt.Empty =>
        schema.dependencies.foreach(traverse)
    }

    traverse(CborSchemaFor[T])
    CborSchemas(tmpRegistry.to(ITreeMap))
  }
}
