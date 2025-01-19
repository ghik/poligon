package interview

import scala.annotation.tailrec
import scala.collection.mutable

class Node(val id: Int, lazyEdges: => Seq[Node]) {
  lazy val edges: Seq[Node] = lazyEdges
}

object Node {
  def apply(id: Int, edges: => Seq[Node]): Node =
    new Node(id, edges)
}

object GraphSearching {
  def dfs(nodes: Seq[Node])(action: Node => Unit): Unit = {
    val visited = new mutable.HashSet[Node]
    var stack: List[Iterator[Node]] = Nil

    def put(node: Node): Unit =
      if (visited.add(node)) {
        action(node)
        stack ::= node.edges.iterator
      }

    @tailrec
    def loop(): Unit = stack match {
      case head :: _ if head.hasNext =>
        put(head.next())
        loop()
      case _ :: tail =>
        stack = tail
        loop()
      case Nil =>
    }

    nodes.foreach { n =>
      put(n)
      loop()
    }
  }

  def bfs(nodes: Seq[Node])(action: Node => Unit): Unit = {
    val queue = new mutable.ArrayDeque[Node]
    val visited = new mutable.HashSet[Node]

    def enqueue(node: Node): Unit =
      if (visited.add(node)) {
        queue.addOne(node)
      }

    @tailrec
    def loop(): Unit = queue.removeHeadOption() match {
      case Some(head) =>
        action(head)
        head.edges.foreach(enqueue)
        loop()
      case None =>
    }

    nodes.foreach { root =>
      enqueue(root)
      loop()
    }
  }

  def main(args: Array[String]): Unit = {
    lazy val nodes: Vector[Node] = Vector(
      Node(0, Seq(nodes(3), nodes(1), nodes(2))),
      Node(1, Seq(nodes(4))),
      Node(2, Seq(nodes(4))),
      Node(3, Seq(nodes(5))),
      Node(4, Seq(nodes(6))),
      Node(5, Seq(nodes(0), nodes(4))),
      Node(6, Seq(nodes(1))),
    )

    bfs(nodes) { n =>
      println(s"BFS visiting node ${n.id}")
    }

    println()

    dfs(nodes) { n =>
      println(s"DFS visiting node ${n.id}")
    }
  }
}
