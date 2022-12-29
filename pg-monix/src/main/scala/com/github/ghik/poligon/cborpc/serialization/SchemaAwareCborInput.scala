package com.github.ghik.poligon.cborpc.serialization

import com.avsystem.commons._
import com.avsystem.commons.serialization.GenCodec.ReadFailure
import com.avsystem.commons.serialization._
import com.avsystem.commons.serialization.cbor.{CborInput, CborListInput, CborObjectInput}
import com.github.ghik.poligon.cborpc._

class SchemaAwareCborInput(
  protected val wrapped: CborInput,
  protected val schemas: CborSchemas,
  tpe: CborType,
) extends InputWrapper with SchemaUtils {
  override def readList(): ListInput = nonNullable(tpe) match {
    case CborSchema.Collection(elemSchema) =>
      new SchemaAwareCborListInput(wrapped.readList(), schemas, elemSchema)
    case _ =>
      throw new ReadFailure("not a list")
  }

  override def readObject(): ObjectInput = nonNullable(tpe) match {
    case record: CborSchema.Record =>
      new CborRecordInput(wrapped.readObject(), schemas, record)
    case union: CborSchema.Union =>
      new CborUnionInput(wrapped.readObject(), schemas, union)
    case _ =>
      throw new ReadFailure("not an object")
  }

  def readSchemaAwareMap[K, V, M](marker: SchemaAwareCborMap[K, V, M]): M =
    nonNullable(tpe) match {
      case CborSchema.Dictionary(keyTpe, valueTpe) =>
        val builder = marker.factory.newBuilder
        val objInput = wrapped.readObject()
        val knownSize = objInput.knownSize
        if (knownSize >= 0) {
          builder.sizeHint(knownSize)
        }
        while (objInput.hasNext) {
          val key = marker.keyCodec.read(new SchemaAwareCborInput(objInput.nextKey(), schemas, keyTpe))
          val value = marker.valueCodec.read(new SchemaAwareCborInput(objInput.nextValue(), schemas, valueTpe))
          builder.addOne(key -> value)
        }
        builder.result()
      case _ => throw new ReadFailure("not a map")
    }

  override def readCustom[T](typeMarker: TypeMarker[T]): Opt[T] = typeMarker match {
    case tm: SchemaAwareCborMap[k, v, m] =>
      readSchemaAwareMap(tm).opt
    case _ => super.readCustom(typeMarker)
  }
}

class SchemaAwareCborListInput(
  protected val wrapped: CborListInput,
  protected val schemas: CborSchemas,
  elemTpe: CborType,
) extends ListInputWrapper with SchemaUtils {
  override def nextElement(): Input =
    new SchemaAwareCborInput(wrapped.nextElement(), schemas, elemTpe)
}

class CborRecordInput(
  protected val wrapped: CborObjectInput,
  protected val schemas: CborSchemas,
  tpe: CborSchema.Record,
) extends ObjectInputWrapper with SchemaUtils {
  override def nextField(): FieldInput = {
    val nextIdx = wrapped.nextKey().readInt() - 1
    val field = tpe.fields(nextIdx)
    new SchemaAwareCborFieldInput(field.name, wrapped.nextValue(), schemas, field.tpe)
  }
}

class CborUnionInput(
  protected val wrapped: CborObjectInput,
  protected val schemas: CborSchemas,
  tpe: CborSchema.Union,
) extends ObjectInputWrapper with SchemaUtils {
  private[this] var caseSchema: CborSchema.Record = _

  override def nextField(): FieldInput = wrapped.nextKey().readInt() match {
    case 0 =>
      val theCase = tpe.cases(wrapped.nextValue().readInt())
      caseSchema = schemas.resolveRecord(theCase.schema) // TODO: check if not already set
      new CborDiscriminatorFieldInput(tpe.discriminator, theCase.name, tpe)
    case idx =>
      if (caseSchema eq null) {
        val defaultCase = tpe.defaultCase.map(_.value)
          .getOrElse(throw new ReadFailure("discriminator field was not read"))
        caseSchema = schemas.resolveRecord(defaultCase.schema)
      }
      val field = caseSchema.fields(idx - 1)
      new SchemaAwareCborFieldInput(field.name, wrapped.nextValue(), schemas, field.tpe)
  }
}

class SchemaAwareCborFieldInput(
  val fieldName: String,
  wrapped: CborInput,
  schemas: CborSchemas,
  tpe: CborType,
) extends SchemaAwareCborInput(wrapped, schemas, tpe) with FieldInput

class CborDiscriminatorFieldInput(
  val fieldName: String,
  value: String,
  tpe: CborSchema.Union,
) extends InputAndSimpleInput with FieldInput {
  def readString(): String = value
  def skip(): Unit = ()

  def readNull(): Boolean = throw new ReadFailure("not a null")
  def readList(): ListInput = throw new ReadFailure("not a list")
  def readObject(): ObjectInput = throw new ReadFailure("not an object")
  def readBoolean(): Boolean = throw new ReadFailure("not a boolean")
  def readInt(): Int = throw new ReadFailure("not an int")
  def readLong(): Long = throw new ReadFailure("not a long")
  def readDouble(): Double = throw new ReadFailure("not a double")
  def readBigInt(): BigInt = throw new ReadFailure("not a big int")
  def readBigDecimal(): BigDecimal = throw new ReadFailure("not a big decimal")
  def readBinary(): Array[Byte] = throw new ReadFailure("not a binary")
}
