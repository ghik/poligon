package com.github.ghik.poligon.macros

import scala.reflect.macros.blackbox

class RandomMacros(val c: blackbox.Context) {

  import c.universe._

  def ImmutablePkg = q"_root_.scala.collection.immutable"

  def methodCountImpl[T: c.WeakTypeTag]: Tree = {
    val tpe: Type = weakTypeOf[T].dealias
    val count = tpe.members.size
    q"$count"
  }

  def sourcePositionImpl: Tree = {
    val pos = c.enclosingPosition
    val fileName = pos.source.file.name
    val line = pos.line
    val posStr = s"$fileName:$line"
    q"""$posStr"""
  }
}
