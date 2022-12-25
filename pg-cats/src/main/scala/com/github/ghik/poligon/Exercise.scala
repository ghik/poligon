package com.github.ghik.poligon

import cats.effect.IOApp

object Exercise1 extends App {
  case class UserId(raw: Int) extends AnyVal
  case class User(id: UserId, login: String)

  object db {
    def loadUser(id: UserId): User = ???
    def storeUser(user: User): Unit = ???
  }

  def modifyUser(userId: UserId): Unit = ???
}

object Exercise2 {
  object db {
    def loadText(): String = ???
    def storeText(str: String): Unit = ???
  }

  def program(): Unit = {
    val txt = db.loadText()
    db.storeText(txt)
  }

//  def program2(): Unit = {
//    val txt = measure("loading", db.loadText())
//    measure("storing", db.storeText(txt))
//  }


  /*
   * Output:
   *
   * loading took 5ms
   * storing took 10ms
   */
}
