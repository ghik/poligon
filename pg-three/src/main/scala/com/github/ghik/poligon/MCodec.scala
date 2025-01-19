package com.github.ghik.poligon

import dotty.tools.dotc.ast.untpd
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.StdNames

import scala.annotation.nowarn
import scala.quoted.*
import scala.quoted.runtime.impl.QuotesImpl

case class Config(info: String)
object Config {
  given default: Config = Config("default")
}

class MCodec[T](val config: Config)

object MCodec {
  transparent inline def apply[T](using inline codec: MCodec[T]): MCodec[T] = codec

  inline def derived[T]: MCodec[T] = ${ derivedImpl[T] }

  inline def derivedWithDeps[D, T](using deps: ValueOf[D]): MCodec[T] =
    ${ derivedWithDepsImpl[T]('{ deps.value }) }

  opaque type WithDeps[D, T] = MCodec[T]
  extension [D, T](dwd: WithDeps[D, T]) {
    def codec: MCodec[T] = dwd
  }
  object WithDeps {
    inline given derived[D: ValueOf, T]: WithDeps[D, T] = MCodec.derivedWithDeps[D, T]
  }

  private def derivedWithDepsImpl[T](deps: Expr[Any])(using q: Quotes)(using Type[T]): Expr[MCodec[T]] = {
    import q.reflect.*

    val givenSelector: Selector = {
      import dotty.tools.dotc.ast.untpd
      import dotty.tools.dotc.core.Contexts.Context
      import dotty.tools.dotc.core.StdNames

      import scala.quoted.runtime.impl.QuotesImpl

      given ctx: Context = q.asInstanceOf[QuotesImpl].ctx
      untpd.ImportSelector(untpd.Ident(StdNames.nme.EMPTY)).asInstanceOf[Selector]
    }

    val theImport = Import(deps.asTerm, List(givenSelector))
    Block(List(theImport), '{ MCodec.derived[T] }.asTerm).asExprOf[MCodec[T]]
  }

  private def derivedImpl[T](using q: Quotes)(using Type[T]): Expr[MCodec[T]] = {
    import q.reflect.*
    val config = Expr.summon[Config].getOrElse(report.errorAndAbort("Config not found"))
    '{ new MCodec[T]($config) }
  }
}


trait HasMCodecWithDeps[D, T](using dwd: MCodec.WithDeps[D, T]) {
  given codec: MCodec[T] = dwd.codec
}

trait HasMCodec[T](using dwd: MCodec.WithDeps[Unit, T]) extends HasMCodecWithDeps[Unit, T]
