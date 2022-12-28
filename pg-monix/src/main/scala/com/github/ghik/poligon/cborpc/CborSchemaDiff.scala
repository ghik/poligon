package com.github.ghik.poligon.cborpc

sealed trait CborSchemaDiff

object CborSchemaDiff {
  final case class Mismatch(reader: CborSchema, writer: CborSchema) extends CborSchemaDiff
  final case class Match(schema: CborSchema) extends CborSchemaDiff
  final case class PrimitivePromote(reader: CborPrimitiveType, writer: CborPrimitiveType) extends CborSchemaDiff

  final case class AsNullable(schema: CborSchemaDiff) extends CborSchemaDiff
  final case class InNullable(diff: CborSchemaDiff) extends CborSchemaDiff
  final case class InCollection(elemDiff: CborSchemaDiff) extends CborSchemaDiff
  final case class InTuple(elemDiffs: Seq[CborSchemaDiff]) extends CborSchemaDiff

  final case class TupleAsCollection(elems: Seq[CborSchemaDiff]) extends CborSchemaDiff
  final case class MissingTupleElem(reader: CborSchema) extends CborSchemaDiff

  def compare(
    reader: CborSchema,
    writer: CborSchema,
    resolver: CborSchemas,
  ): CborSchemaDiff = ???
}
