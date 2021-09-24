

val commonSettings = Seq(
  scalaVersion := "2.13.6",

  Compile / scalacOptions ++= Seq(
    "-encoding", "utf-8",
    "-Yrangepos",
    "-explaintypes",
    "-feature",
    "-deprecation",
    "-unchecked",
    "-language:implicitConversions",
    "-language:existentials",
    "-language:dynamics",
    "-language:experimental.macros",
    "-language:higherKinds",
    "-Xfatal-warnings",
    "-Xlint:-missing-interpolator,-adapted-args,-unused,_",
    "-Ycache-plugin-class-loader:last-modified",
    "-Ycache-macro-class-loader:last-modified",
  ),

  Test / scalacOptions := (Compile / scalacOptions).value,
  Compile / doc / sources := Seq.empty,

  libraryDependencies ++= Seq(
    "com.avsystem.commons" %% "commons-core" % Version.AvsCommons,
  )
)

lazy val `pg-monix` = project.settings(commonSettings: _*).settings(
  libraryDependencies ++= Seq(
    "com.avsystem.commons" %% "commons-mongo" % Version.AvsCommons,
    "com.avsystem.commons" %% "commons-redis" % Version.AvsCommons,
    "io.monix" %% "monix" % Version.Monix,
    "io.monix" %% "monix-bio" % Version.MonixBio,
  )
)

lazy val `pg-cats` = project.settings(commonSettings: _*).settings(
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-free" % Version.CatsFree,
    "org.typelevel" %% "cats-effect" % Version.CatsEffect,
  )
)
