package com.github.ghik.poligon.cborpc

sealed trait QualifiedSymbol {
  def schemas: CborSchemas
}

sealed trait QualifiedMethod extends QualifiedSymbol {
  def index: Int
  def parent: QualifiedApi
  def method: CborSchema.Method

  def schemas: CborSchemas = parent.schemas
}

final class QualifiedSubapiMethod(
  val index: Int,
  val parent: QualifiedApi,
  val method: CborSchema.SubapiMethod,
) extends QualifiedMethod {
  def result: QualifiedApi = new QualifiedApi(Right(this), method.result)
}

final class QualifiedCallMethod(
  val index: Int,
  val parent: QualifiedApi,
  val method: CborSchema.CallMethod,
) extends QualifiedMethod {
  def params: Seq[QualifiedParam] =
    method.input.fields.iterator.zipWithIndex
      .map { case (field, index) => new QualifiedParam(index, this, field) }
      .to(Vector)
}

final class QualifiedParam(
  val index: Int,
  val parent: QualifiedCallMethod,
  val param: CborSchema.Field,
) extends QualifiedSymbol {
  def schemas: CborSchemas = parent.schemas
}

final class QualifiedApi(
  schemasOrParent: Either[CborSchemas, QualifiedSubapiMethod],
  val api: CborApi,
) extends QualifiedSymbol {
  def parent: Option[QualifiedSubapiMethod] = schemasOrParent.toOption
  val schemas: CborSchemas = schemasOrParent.fold(identity, _.schemas)

  lazy val methods: Seq[QualifiedMethod] = schemas.resolveApi(api) match {
    case CborSchema.Api(methods) =>
      methods.iterator.zipWithIndex
        .map {
          case (m: CborSchema.SubapiMethod, index) => new QualifiedSubapiMethod(index, this, m)
          case (m: CborSchema.CallMethod, index) => new QualifiedCallMethod(index, this, m)
        }
        .to(Vector)
  }

  lazy val subapiMethodsByName: Map[String, QualifiedSubapiMethod] =
    methods.iterator.collect({ case m: QualifiedSubapiMethod => (m.method.name, m) }).toMap

  lazy val callMethodsByName: Map[String, QualifiedCallMethod] =
    methods.iterator.collect({ case m: QualifiedCallMethod => (m.method.name, m) }).toMap
}
