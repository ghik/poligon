package com.github.ghik.poligon.cborpc.serialization

import com.github.ghik.poligon.cborpc.{CborSchema, CborSchemas, CborType, DirectCborType}

import scala.annotation.tailrec

trait SchemaUtils {
  protected def schemas: CborSchemas

  @tailrec
  protected final def nonNullable(tpe: CborType): DirectCborType =
    schemas.resolveType(tpe) match {
      case CborSchema.Nullable(schema) => nonNullable(schema)
      case tpe => tpe
    }
}
