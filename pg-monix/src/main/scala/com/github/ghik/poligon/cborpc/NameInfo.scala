package com.github.ghik.poligon.cborpc

import com.avsystem.commons._
import com.avsystem.commons.meta._
import com.avsystem.commons.serialization.{name, optionalParam, transientDefault, whenAbsent}

final case class NameInfo(
  @reifyName sourceName: String,
  @optional @reifyAnnot annotName: Opt[name],
) {
  def rawName: String = annotName.fold(sourceName)(_.name)
}

final class CborField[T](
  @composite val nameInfo: NameInfo,
  @isAnnotated[optionalParam] optional: Boolean,
  @isAnnotated[whenAbsent[T]] hasWhenAbsent: Boolean,
  @isAnnotated[transientDefault] transientDefault: Boolean,
  @reifyFlags flags: ParamFlags,
  @infer val schemaFor: CborTypeFor[T],
) extends TypedMetadata[T] {

  def rawField: CborSchema.Field = CborSchema.Field(
    nameInfo.rawName,
    schemaFor.cborType,
    optional,
    flags.hasDefaultValue || hasWhenAbsent,
    transientDefault,
  )
}
