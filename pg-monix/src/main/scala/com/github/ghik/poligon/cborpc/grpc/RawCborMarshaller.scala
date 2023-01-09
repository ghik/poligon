package com.github.ghik.poligon.cborpc.grpc

import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.cbor.RawCbor
import com.google.common.io.ByteStreams
import io.grpc.KnownLength
import io.grpc.MethodDescriptor.Marshaller

import java.io.{ByteArrayInputStream, InputStream}

object RawCborMarshaller extends Marshaller[RawCbor] {
  def stream(value: RawCbor): InputStream =
    new ByteArrayInputStream(value.bytes, value.offset, value.length) with KnownLength

  def parse(stream: InputStream): RawCbor =
    RawCbor(ByteStreams.toByteArray(stream))
}

class CborMarshaller[T: GenCodec] extends Marshaller[T] {
  def stream(value: T): InputStream =
    new ByteArrayInputStream(RawCbor.write(value).bytes)

  def parse(stream: InputStream): T =
    RawCbor(stream.readAllBytes()).readAs[T]()
}
object CborMarshaller {
  def apply[T: GenCodec]: CborMarshaller[T] = new CborMarshaller[T]
}
