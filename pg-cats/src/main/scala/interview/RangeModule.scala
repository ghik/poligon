package interview

import scala.collection.mutable

sealed trait Boundary {
  def at: Int

  override def toString: String = this match {
    case Start(at) => s"[$at"
    case End(at) => s"$at)"
  }
}

final case class Start(at: Int) extends Boundary

final case class End(at: Int) extends Boundary

object Boundary {
  implicit val ordering: Ordering[Boundary] =
    Ordering.by[Boundary, Int](_.at).orElseBy {
      case End(_) => 0
      case Start(_) => 1
    }
}

class IntervalTreeNode(splitAt: Int) {
  private val intervalsByStart: mutable.TreeSet[Range] =
    new mutable.TreeSet[Range]()(Ordering.by(_.start))

  private val intervalsByEnd: mutable.TreeSet[Range] =
    new mutable.TreeSet[Range]()(Ordering.by(_.end))

  def add(range: Range): Unit = {
    intervalsByStart.add(range)
    intervalsByEnd.add(range)
  }

  def remove(range: Range): Unit = {
    intervalsByStart.remove(range)
    intervalsByEnd.remove(range)
  }
}

class RangeModule() {
  private val boundaries = new mutable.TreeSet[Boundary]

  override def toString: String =
    boundaries.mkString(",")

  def addRange(left: Int, right: Int): Unit = {
    val cleanupStart =
      boundaries.maxBefore(End(left)).collect({ case b: Start => b.at }).getOrElse {
        boundaries.add(Start(left))
        left
      }

    val cleanupEnd =
      boundaries.minAfter(End(right + 1)).collect({ case b: End => b.at }).getOrElse {
        boundaries.add(End(right))
        right
      }

    boundaries.range(End(cleanupStart + 1), End(cleanupEnd))
      .toVector.foreach(boundaries.remove)
  }

  def removeRange(left: Int, right: Int): Unit = {
    val cleanupStart =
      boundaries.maxBefore(Start(left)) match {
        case None => left
        case Some(End(at)) => at
        case Some(Start(_)) =>
          boundaries.add(End(left))
          left
      }

    val cleanupEnd =
      boundaries.minAfter(Start(right)) match {
        case None => right
        case Some(Start(at)) => at
        case Some(End(_)) =>
          boundaries.add(Start(right))
          right
      }

    boundaries.range(Start(cleanupStart), Start(cleanupEnd))
      .toVector.foreach(boundaries.remove)
  }

  def queryRange(left: Int, right: Int): Boolean =
    boundaries.maxBefore(End(left + 1)) match {
      case Some(Start(_)) => boundaries.minAfter(End(left + 1)) match {
        case Some(End(at)) => at >= right
        case _ => false
      }
      case _ => false
    }
}

object RangeModule {
  def main(args: Array[String]): Unit = {
    val rm = new RangeModule()
    rm.addRange(6, 8)
    println(rm)
    rm.removeRange(7, 8)
    println(rm)
    rm.removeRange(8, 9)
    println(rm)
    rm.addRange(8, 9)
    println(rm)
    rm.removeRange(1, 3)
    println(rm)
    rm.addRange(1, 8)
    println(rm)
    println(rm.queryRange(2, 4))
    println(rm.queryRange(2, 9))
    println(rm.queryRange(4, 6))
  }
}
