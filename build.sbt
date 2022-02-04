

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

lazy val `pg-macros` = project.settings(commonSettings: _*).settings(

)

lazy val `pg-monix` = project.settings(commonSettings: _*).dependsOn(`pg-macros`).settings(
  libraryDependencies ++= Seq(
    "com.avsystem.commons" %% "commons-mongo" % Version.AvsCommons,
    "com.avsystem.commons" %% "commons-redis" % Version.AvsCommons,
    "org.mongodb" % "mongodb-driver-reactivestreams" % Version.Mongodb,
    "io.udash" %% "udash-rest" % Version.UdashRest,
    "io.udash" %% "udash-rest-jetty" % Version.UdashRest,
    "io.monix" %% "monix" % Version.Monix,
    "io.monix" %% "monix-bio" % Version.MonixBio,
    "org.eclipse.jetty" % "jetty-server" % Version.Jetty,
    "org.eclipse.jetty" % "jetty-servlet" % Version.Jetty,
    "org.eclipse.jetty" % "jetty-client" % Version.Jetty,
    "dev.zio" %% "zio" % Version.Zio,
    "dev.zio" %% "zio-streams" % Version.Zio,
    "io.netty" % "netty-all" % Version.Netty,
    "com.typesafe.akka" %% "akka-actor" % Version.Akka,
    "com.typesafe.akka" %% "akka-actor-typed" % Version.Akka,
    "com.typesafe.akka" %% "akka-stream" % Version.Akka,
    "com.typesafe.akka" %% "akka-stream-typed" % Version.Akka,
    "com.typesafe.akka" %% "akka-cluster" % Version.Akka,
    "com.typesafe.akka" %% "akka-http" % Version.AkkaHttp,
    "io.micrometer" % "micrometer-registry-prometheus" % Version.Micrometer,
  )
)

lazy val `pg-cats` = project.settings(commonSettings: _*).settings(
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-free" % Version.CatsFree,
    "org.typelevel" %% "cats-effect" % Version.CatsEffect,
    "co.fs2" %% "fs2-core" % Version.Fs2,
    "co.fs2" %% "fs2-io" % Version.Fs2,
    "co.fs2" %% "fs2-reactive-streams" % Version.Fs2,
    "org.scalatest" %% "scalatest" % "3.2.9" % Test,
  )
)
