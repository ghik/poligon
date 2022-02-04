package com.github.ghik.poligon

/*
 * - porównaj ZIO z Catsami
 * - czemu robimy asynchroniczne API
 * - jak działajo for comprehensiony
 */

object Rekru {
  // /foo/{param}/bar/{otherParam}/stuff
  type PathPattern
  // /foo/42/bar/oof/stuff
  type Path

  def renderPattern(pathPattern: PathPattern): String = ???

  def renderPath(path: Path): String = ???

  def extractParams(pattern: PathPattern, path: Path): Option[Map[String, String]] = ???

  def insertParams(pattern: PathPattern, values: Map[String, String]): Option[Path] = ???

  def parsePattern(rawPattern: String): PathPattern = ???

  def parsePath(rawPath: String): Path = ???

  for {
    a <- List(1, 2, 3) if a < 3
    b <- List(4, 5, 6)
  } yield a * b

  case class UserId(raw: String) extends AnyVal
  case class User(id: UserId, name: String)

  trait Database {
    def loadUser(id: UserId): Option[User]
    def saveUser(user: User): Unit
  }

  /**
   * Updates the name of user with given ID by ensuring that it starts with an uppercase letter.
   */
  def capitalizeUserName(id: UserId): Unit = ???

  def loadAllUsers(ids: List[UserId]): List[User] = ???
}
