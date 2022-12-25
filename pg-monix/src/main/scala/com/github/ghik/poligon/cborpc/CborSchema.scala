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

@flatten("case") sealed trait CborSchema {
  import CborSchema._

  final def canRead(written: CborSchema): Boolean = (this, written) match {
    case (Primitive(rtpe), Primitive(wtpe)) => rtpe.canRead(wtpe)

    case (Nullable(_), Primitive(CborType.Null)) => true
    case (Nullable(_), Nullable(wschema)) => this.canRead(wschema)
    case (Nullable(rschema), wschema) => rschema.canRead(wschema)

    case (Tuple(relems), Tuple(welems)) => relems.corresponds(welems)(_ canRead _)

    case (Collection(relem), Collection(welem)) => relem.canRead(welem)
    case (Collection(relem), Tuple(welems)) => welems.forall(relem.canRead)

    case (Dictionary(rkey, rvalue), Dictionary(wkey, wvalue)) => rkey.canRead(wkey) && rvalue.canRead(wvalue)

    case (rrec: Record, wrec: Record) =>
      rrec.fields.forall { wfield =>
        wrec.fieldMap.get(wfield.name) match {
          case Some(wf) => (wfield.optionalOnRead || !wf.optionalOnWrite) && wfield.schema.canRead(wf.schema)
          case None => wfield.optionalOnRead
        }
      }

    case (runion: Union, wunion: Union) =>
      wunion.cases.forall { wcase =>
        runion.caseMap.get(wcase.name).exists(_.schema.canRead(wcase.schema))
      }

    case (runion: Union, wrec: Record) =>
      runion.defaultCase.map(runion.caseMap).exists(_.schema.canRead(wrec))

    case _ =>
      false
  }
}
object CborSchema extends HasGenCodec[CborSchema] {
  final case class Primitive(tpe: CborType) extends CborSchema
  final case class Nullable(schema: CborSchema) extends CborSchema
  final case class Tuple(elemSchemas: Seq[CborSchema]) extends CborSchema
  final case class Collection(elemSchema: CborSchema) extends CborSchema
  final case class Dictionary(keySchema: CborSchema, valueSchema: CborSchema) extends CborSchema

  final case class Record(fields: Seq[Field]) extends CborSchema {
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

  final case class Union(cases: Seq[Case], @optionalParam defaultCase: Opt[String]) extends CborSchema {
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

  def canAccept(client: CborApi): Boolean =
    client.methods.forall { cmethod =>
      methodMap.get(cmethod.name).exists { smethod =>
        smethod.input.canRead(cmethod.input) && smethod.result.canAccept(cmethod.result)
      }
    }
}
object CborApi {
  final case class Method(
    name: String,
    input: CborSchema.Record,
    result: MethodResult,
  )

  sealed trait MethodResult {
    import MethodResult._

    def canAccept(client: MethodResult): Boolean = (this, client) match {
      case (SubApi(ssubapi), SubApi(csubapi)) => ssubapi.canAccept(csubapi)
      case (Single(sschema), Single(cschema)) => cschema.canRead(sschema)
      case (Single(sschema), Stream(cschema)) => cschema.canRead(sschema) // can accept single element as stream
      case (Stream(sschema), Stream(cschema)) => cschema.canRead(sschema)
      case _ => false
    }
  }
  object MethodResult {
    final case class SubApi(subapi: CborApi) extends MethodResult
    final case class Single(schema: CborSchema) extends MethodResult
    final case class Stream(schema: CborSchema) extends MethodResult
  }
}
