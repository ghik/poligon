

val commonSettings = Seq(
  scalaVersion := "2.13.16",

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
    "-Werror",
    "-Xlint:_,-strict-unsealed-patmat,-missing-interpolator,-adapted-args,-unused",
    "-Xnon-strict-patmat-analysis",
    "-Ycache-plugin-class-loader:last-modified",
    "-Ycache-macro-class-loader:last-modified",
  ),

  Test / scalacOptions := (Compile / scalacOptions).value,
  Compile / doc / sources := Seq.empty,
  Compile / compileOrder := CompileOrder.Mixed,
)

lazy val `pg-macros` = project.settings(commonSettings *).settings(
  libraryDependencies ++= Seq(
    "com.avsystem.commons" %% "commons-macros" % V.AvsCommons,
  ),
)

lazy val `pg-monix` = project.settings(commonSettings *).dependsOn(`pg-macros`).settings(
  libraryDependencies ++= Seq(
    "com.avsystem.commons" %% "commons-core" % V.AvsCommons,
    "com.avsystem.commons" %% "commons-mongo" % V.AvsCommons,
    "com.avsystem.commons" %% "commons-redis" % V.AvsCommons,
    "org.mongodb" % "mongodb-driver-reactivestreams" % V.Mongodb,
    "io.udash" %% "udash-rest" % V.UdashRest,
    "io.udash" %% "udash-rest-jetty" % V.UdashRest,
    "io.monix" %% "monix" % V.Monix,
    "io.monix" %% "monix-bio" % V.MonixBio,
    "org.eclipse.jetty" % "jetty-server" % V.Jetty,
    "org.eclipse.jetty" % "jetty-servlet" % V.Jetty,
    "org.eclipse.jetty" % "jetty-client" % V.Jetty,
    "dev.zio" %% "zio" % V.Zio,
    "dev.zio" %% "zio-streams" % V.Zio,
    "io.netty" % "netty-all" % V.Netty,
    "com.typesafe.akka" %% "akka-actor" % V.Akka,
    "com.typesafe.akka" %% "akka-actor-typed" % V.Akka,
    "com.typesafe.akka" %% "akka-stream" % V.Akka,
    "com.typesafe.akka" %% "akka-stream-typed" % V.Akka,
    "com.typesafe.akka" %% "akka-cluster" % V.Akka,
    "com.typesafe.akka" %% "akka-http" % V.AkkaHttp,
    "io.micrometer" % "micrometer-registry-prometheus" % V.Micrometer,
    "co.fs2" %% "fs2-core" % V.Fs2_2,
    "co.fs2" %% "fs2-io" % V.Fs2_2,
    "co.fs2" %% "fs2-reactive-streams" % V.Fs2_2,
    "io.grpc" % "grpc-core" % V.Grpc,
    "io.grpc" % "grpc-netty-shaded" % V.Grpc,
    "io.grpc" % "grpc-protobuf" % V.Grpc,
    "org.apache.avro" % "avro" % V.Avro,
    "org.apache.avro" % "avro-grpc" % V.Avro,
    "com.sksamuel.avro4s" %% "avro4s-core" % V.Avro4s,
    "org.springframework.boot" % "spring-boot" % V.SpringBoot,
    "org.apache.kafka" % "kafka-clients" % V.KafkaClients,
    "org.scalatest" %% "scalatest" % V.Scalatest % Test,
  ),
)

lazy val `pg-cats` = project.settings(commonSettings *).settings(
  libraryDependencies ++= Seq(
    "com.avsystem.commons" %% "commons-core" % V.AvsCommons,
    "org.typelevel" %% "cats-free" % V.CatsFree,
    "org.typelevel" %% "cats-effect" % V.CatsEffect,
    "co.fs2" %% "fs2-core" % V.Fs2_3,
    "co.fs2" %% "fs2-io" % V.Fs2_3,
    "co.fs2" %% "fs2-reactive-streams" % V.Fs2_3,
    "org.scalatest" %% "scalatest" % V.Scalatest % Test,
  ),
)

lazy val `pg-three` = project.settings(
  scalaVersion := "3.6.1",

  Compile / scalacOptions ++= Seq(
    "-encoding", "utf-8",
    "-Werror",
  ),

  libraryDependencies ++= Seq(
    "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
    "io.circe" %% "circe-core" % V.Circe,
    "io.circe" %% "circe-generic" % V.Circe,
    "io.circe" %% "circe-parser" % V.Circe,
  ),
)
