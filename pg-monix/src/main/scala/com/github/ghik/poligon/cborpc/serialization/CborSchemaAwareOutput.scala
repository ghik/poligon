package com.github.ghik.poligon.cborpc.serialization

import com.avsystem.commons.serialization.GenCodec.WriteFailure
import com.avsystem.commons.serialization._
import com.avsystem.commons.serialization.cbor.{CborListOutput, CborObjectOutput, CborOutput}
import com.github.ghik.poligon.cborpc._

class SchemaAwareCborOutput(
  protected val wrapped: CborOutput,
  protected val schemas: CborSchemas,
  tpe: CborType,
) extends OutputWrapper with SchemaUtils {

  override def writeList(): ListOutput = nonNullable(tpe) match {
    case CborSchema.Collection(elemSchema) =>
      new SchemaAwareCborListOutput(wrapped.writeList(), schemas, elemSchema)
    case _ =>
      throw new WriteFailure("unexpected list")
  }

  override def writeObject(): ObjectOutput = nonNullable(tpe) match {
    case record: CborSchema.Record =>
      new CborRecordOutput(wrapped.writeObject(), schemas, record)
    case union: CborSchema.Union =>
      new CborUnionOutput(wrapped.writeObject(), schemas, union)
    case _ =>
      throw new WriteFailure("unexpected object")
  }

  def writeSchemaAwareMap[K, V, M](marker: SchemaAwareCborMap[K, V, M], value: M): Unit =
    nonNullable(tpe) match {
      case CborSchema.Dictionary(keyTpe, valueTpe) =>
        val objOutput = wrapped.writeObject()
        val entries = marker.toIterable(value)
        objOutput.declareSizeOf(entries)
        entries.foreach { case (key, value) =>
          marker.keyCodec.write(new SchemaAwareCborOutput(objOutput.writeKey(), schemas, keyTpe), key)
          marker.valueCodec.write(new SchemaAwareCborOutput(objOutput.writeValue(), schemas, valueTpe), value)
        }
        objOutput.finish()
      case _ =>
        throw new WriteFailure("unexpected map")
    }

  override def writeCustom[T](typeMarker: TypeMarker[T], value: T): Boolean = typeMarker match {
    case tm: SchemaAwareCborMap[k, v, m] =>
      writeSchemaAwareMap(tm, value: m)
      true
    case _ =>
      super.writeCustom(typeMarker, value)
  }
}

class SchemaAwareCborListOutput(
  protected val wrapped: CborListOutput,
  protected val schemas: CborSchemas,
  elemTpe: CborType,
) extends ListOutputWrapper with SchemaUtils {
  override def writeElement(): Output =
    new SchemaAwareCborOutput(wrapped.writeElement(), schemas, elemTpe)
}

class CborRecordOutput(
  protected val wrapped: CborObjectOutput,
  protected val schemas: CborSchemas,
  tpe: CborSchema.Record,
) extends ObjectOutputWrapper with SchemaUtils {
  override def writeField(key: String): Output =
    tpe.fieldsByName.get(key) match {
      case None => NoOutput // ignore field
      case Some(field) =>
        // +1 because 0 is reserved for discriminator field in unions
        wrapped.writeKey().writeInt(field.index + 1)
        new SchemaAwareCborOutput(wrapped.writeValue(), schemas, field.value.tpe)
    }
}

class CborUnionOutput(
  protected val wrapped: CborObjectOutput,
  protected val schemas: CborSchemas,
  tpe: CborSchema.Union,
) extends ObjectOutputWrapper with SchemaUtils {
  private[this] var recordOutput: CborRecordOutput = _

  private def onCaseChosen(c: CborSchema.Case): Unit = {
    recordOutput = new CborRecordOutput(wrapped, schemas, schemas.resolveRecord(c.schema))
  }

  override def writeField(key: String): Output =
    if (key == tpe.discriminator) {
      wrapped.writeKey().writeInt(0)
      new CborDiscriminatorFieldOutput(wrapped.writeValue(), tpe, onCaseChosen)
    } else {
      if (recordOutput eq null) {
        val defaultCase = tpe.defaultCase.map(_.value)
          .getOrElse(throw new WriteFailure("discriminator field was not written"))
        onCaseChosen(defaultCase)
      }
      recordOutput.writeField(key)
    }
}

class CborDiscriminatorFieldOutput(
  wrapped: CborOutput,
  tpe: CborSchema.Union,
  onCaseChosen: CborSchema.Case => Unit
) extends OutputAndSimpleOutput {
  override def writeString(str: String): Unit =
    tpe.casesByName.get(str) match {
      case Some(c) =>
        onCaseChosen(c.value)
        wrapped.writeInt(c.index)
      case None =>
        throw new WriteFailure(s"unknown discriminator: $str")
    }

  def writeNull(): Unit = throw new WriteFailure(s"unexpected null")
  def writeList(): ListOutput = throw new WriteFailure(s"unexpected list")
  def writeObject(): ObjectOutput = throw new WriteFailure(s"unexpected object")
  def writeBoolean(boolean: Boolean): Unit = throw new WriteFailure(s"unexpected boolean")
  def writeInt(int: Int): Unit = throw new WriteFailure(s"unexpected int")
  def writeLong(long: Long): Unit = throw new WriteFailure(s"unexpected long")
  def writeDouble(double: Double): Unit = throw new WriteFailure(s"unexpected double")
  def writeBigInt(bigInt: BigInt): Unit = throw new WriteFailure(s"unexpected big int")
  def writeBigDecimal(bigDecimal: BigDecimal): Unit = throw new WriteFailure(s"unexpected big decimal")
  def writeBinary(binary: Array[Byte]): Unit = throw new WriteFailure(s"unexpected binary")
}

object NoOutput extends Output with SimpleOutput with ListOutput with ObjectOutput {
  def writeNull(): Unit = ()
  def writeSimple(): SimpleOutput = this
  def writeList(): ListOutput = this
  def writeObject(): ObjectOutput = this
  def writeString(str: String): Unit = ()
  def writeBoolean(boolean: Boolean): Unit = ()
  def writeInt(int: Int): Unit = ()
  def writeLong(long: Long): Unit = ()
  def writeDouble(double: Double): Unit = ()
  def writeBigInt(bigInt: BigInt): Unit = ()
  def writeBigDecimal(bigDecimal: BigDecimal): Unit = ()
  def writeBinary(binary: Array[Byte]): Unit = ()
  def writeElement(): Output = this
  def writeField(key: String): Output = this
  def finish(): Unit = ()
}
