package com.github.ghik.poligon.cborpc

import com.avsystem.commons._
import com.avsystem.commons.misc.{AbstractValueEnum, AbstractValueEnumCompanion, EnumCtx}
import com.avsystem.commons.serialization._
import com.avsystem.commons.serialization.cbor.CborAdtMetadata.CborKeyInfo
import com.avsystem.commons.serialization.cbor.{CborAdtMetadata, cborDiscriminator, cborKey}

final class CborType(implicit enumCtx: EnumCtx) extends AbstractValueEnum {
  import CborType._

  def canRead(other: CborType): Boolean = this == other || ((this, other) match {
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
object CborType extends AbstractValueEnumCompanion[CborType] {
  final val Null, Boolean, Char: Value = new CborType
  final val Byte, Short, Int, Long: Value = new CborType
  final val Float, Double: Value = new CborType
  final val String, Timestamp, Binary, Raw: Value = new CborType
}

@flatten("case") sealed trait CborSchema

/**
 * A [[CborSchema]] that is not a [[CborSchema.Reference]]
 * (although it may contain references).
 */
sealed trait DirectCborSchema extends CborSchema

object CborSchema extends HasGenCodec[CborSchema] {
  final case class Reference(name: String) extends CborSchema

  final case class Primitive(tpe: CborType) extends DirectCborSchema
  final case class Nullable(schema: CborSchema) extends DirectCborSchema
  final case class Tuple(elemSchemas: Seq[CborSchema]) extends DirectCborSchema
  final case class Collection(elemSchema: CborSchema) extends DirectCborSchema
  final case class Dictionary(keySchema: CborSchema, valueSchema: CborSchema) extends DirectCborSchema

  final case class Record(fields: Seq[Field]) extends DirectCborSchema {
    lazy val fieldMap: Map[String, Field] = fields.toMapBy(_.name)

    def toCborAdtMetadata[T](name: String, idx: Int): CborAdtMetadata.Record[T] =
      new CborAdtMetadata.Record[T](
        new CborKeyInfo(name, Opt.Empty, Opt(new cborKey[Int](idx, GenCodec[Int]))),
        fields.iterator.zipWithIndex
          .map { case (f, idx) =>
            val cborKey = new cborKey[Int](idx, GenCodec[Int])
            val keyInfo = new CborKeyInfo[Any](f.name, Opt.Empty, Opt(cborKey))
            new CborAdtMetadata.Field[Any](keyInfo)
          }
          .toList
      )
  }
  object Record extends HasGenCodec[Record]

  final case class Union(cases: Seq[Case], @optionalParam defaultCase: Opt[String]) extends DirectCborSchema {
    lazy val caseMap: Map[String, Case] = cases.toMapBy(_.name)

    def toCborAdtMetadata[T](name: String): CborAdtMetadata.Union[T] =
      new CborAdtMetadata.Union[T](
        name,
        Opt(new cborDiscriminator[Int](0, GenCodec[Int])),
        Opt(new flatten),
        cases.iterator.zipWithIndex
          .map({ case (c, idx) => c.schema.toCborAdtMetadata(c.name, idx) })
          .toList
      )
  }
  object Union extends HasGenCodec[Union]

  final case class Field(
    name: String,
    schema: CborSchema, // TODO lazify for recursive types
    @transientDefault optional: Boolean = false,
    @transientDefault hasDefaultValue: Boolean = false,
    @transientDefault transientDefault: Boolean = false,
  ) {
    def optionalOnWrite: Boolean = optional || (hasDefaultValue && transientDefault)
    def optionalOnRead: Boolean = optional || hasDefaultValue
  }
  object Field extends HasGenCodec[Field]

  final case class Case(
    name: String,
    schema: CborSchema.Record,
  )
  object Case extends HasGenCodec[Case]
}

final case class CborApi(methods: Seq[CborApi.Method]) {
  val methodMap: Map[String, CborApi.Method] = methods.toMapBy(_.name)
}
object CborApi {
  final case class Method(
    name: String,
    input: CborSchema.Record,
    result: MethodResult,
  )

  sealed trait MethodResult
  object MethodResult {
    final case class SubApi(subapi: CborApi) extends MethodResult
    final case class Single(schema: CborSchema) extends MethodResult
    final case class Stream(schema: CborSchema) extends MethodResult
  }
}
