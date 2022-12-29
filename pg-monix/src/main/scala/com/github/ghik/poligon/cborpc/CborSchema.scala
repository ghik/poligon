package com.github.ghik.poligon.cborpc

import com.avsystem.commons._
import com.avsystem.commons.misc.{AbstractValueEnum, AbstractValueEnumCompanion, EnumCtx}
import com.avsystem.commons.serialization._
import com.github.ghik.poligon.cborpc.util.Indexed

final class CborPrimitiveType(implicit enumCtx: EnumCtx) extends AbstractValueEnum {
  import CborPrimitiveType._

  def canRead(other: CborPrimitiveType): Boolean = this == other || ((this, other) match {
    case (Raw, _) => true
    case (Short, Byte) => true
    case (Int, Short | Byte) => true
    case (Long, Int | Short | Byte) => true
    case (Float, Short) => true
    case (Double, Int | Short | Float) => true
    case (String, Char) => true
    case _ => false
  })
}
object CborPrimitiveType extends AbstractValueEnumCompanion[CborPrimitiveType] {
  final val Null, Boolean, Char: Value = new CborPrimitiveType
  final val Byte, Short, Int, Long: Value = new CborPrimitiveType
  final val Float, Double: Value = new CborPrimitiveType
  final val String, Timestamp, Binary, Raw: Value = new CborPrimitiveType
}

@flatten("case") sealed trait CborSchema

/**
 * A [[CborSchema]] that is not a [[CborSchema.Reference]]
 * (although it may contain references).
 */
sealed trait DirectCborSchema extends CborSchema
object DirectCborSchema extends HasGenCodec[DirectCborSchema]

/**
 * [[CborSchema]] for a data type.
 */
sealed trait CborType extends CborSchema
object CborType extends HasGenCodec[CborType]

sealed trait DirectCborType extends CborType with DirectCborSchema
object DirectCborType extends HasGenCodec[DirectCborType]

/**
 * [[CborSchema]] for an API (interface).
 */
sealed trait CborApi extends CborSchema
object CborApi extends HasGenCodec[CborApi]

sealed trait DirectCborApi extends CborApi with DirectCborSchema
object DirectCborApi extends HasGenCodec[DirectCborApi]

object CborSchema extends HasGenCodec[CborSchema] {
  final case class Reference(name: String) extends CborType with CborApi

  final case class Primitive(tpe: CborPrimitiveType) extends DirectCborType
  final case class Nullable(schema: CborType) extends DirectCborType
  final case class Collection(elemSchema: CborType) extends DirectCborType
  final case class Dictionary(keySchema: CborType, valueSchema: CborType) extends DirectCborType

  final case class Record(fields: Seq[Field]) extends DirectCborType {
    lazy val fieldsByName: Map[String, Indexed[Field]] =
      fields.iterator.zipWithIndex.map(Indexed[Field]).toMapBy(_.value.name)
  }
  object Record extends HasGenCodec[Record]

  final case class Union(
    @transientDefault @whenAbsent("_case") discriminator: String,
    cases: Seq[Case],
    @optionalParam defaultCaseName: Opt[String],
  ) extends DirectCborType {
    lazy val casesByName: Map[String, Indexed[Case]] =
      cases.iterator.zipWithIndex.map(Indexed[Case]).toMapBy(_.value.name)
    lazy val defaultCase: Opt[Indexed[Case]] =
      defaultCaseName.map(casesByName)
  }
  object Union extends HasGenCodec[Union]

  final case class Api(methods: Seq[Method]) extends DirectCborApi {
    lazy val methodsByName: Map[String, Indexed[Method]] =
      methods.iterator.zipWithIndex.map(Indexed[Method]).toMapBy(_.value.name)
  }

  final case class Field(
    name: String,
    tpe: CborType,
    @transientDefault optional: Boolean = false,
    @transientDefault hasDefaultValue: Boolean = false,
    @transientDefault transientDefault: Boolean = false,
  ) {
    def optionalOnWrite: Boolean = optional || (hasDefaultValue && transientDefault)
    def optionalOnRead: Boolean = optional || hasDefaultValue
  }
  object Field extends HasGenCodec[Field]

  final case class Case(name: String) {
    def schema: CborSchema.Reference = CborSchema.Reference(name)
  }
  object Case extends StringWrapperCompanion[Case]

  final case class Method(
    name: String,
    @transientDefault @whenAbsent(CborSchema.Record(Nil)) input: CborSchema.Record,
    result: MethodResult,
  )
  object Method extends HasGenCodec[Method]

  @flatten("type") sealed trait MethodResult
  object MethodResult extends HasGenCodec[MethodResult] {
    final case class Subapi(subapi: CborApi) extends MethodResult
    final case class Call(schema: CborType, @transientDefault stream: Boolean = false) extends MethodResult
  }
}
