package com.github.ghik.poligon.grpc

import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.cbor.{CborInput, CborOutput}
import com.google.common.io.ByteStreams
import io.grpc.MethodDescriptor.Marshaller

import java.io.{ByteArrayInputStream, InputStream}

class CborMarshaller[T: GenCodec] extends Marshaller[T] {
  def stream(value: T): InputStream =
    new ByteArrayInputStream(CborOutput.write[T](value))

  def parse(stream: InputStream): T =
    CborInput.read(ByteStreams.toByteArray(stream))
}
