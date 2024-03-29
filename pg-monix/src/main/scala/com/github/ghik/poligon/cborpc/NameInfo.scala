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
  @infer lazySchemaFor: => CborTypeFor[T],
) extends TypedMetadata[T] {

  lazy val schemaFor: CborTypeFor[T] = lazySchemaFor

  def rawField: CborSchema.Field = CborSchema.Field(
    nameInfo.rawName,
    schemaFor.schema,
    optional,
    flags.hasDefaultValue || hasWhenAbsent,
    transientDefault,
  )
}
