name := "candlestick-proxy"

version := "0.1"

scalaVersion := "2.12.10"

resolvers ++= Seq(
  "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Typesafe maven releases" at "http://repo.typesafe.com/typesafe/maven-releases/"
)

val fs2Version = "2.0.1"
val circeVersion = "0.12.2"
val http4sVersion = "0.21.0-M5"
val log4catsVersion = "1.0.1"

libraryDependencies ++= Seq(
  "org.typelevel"         %% "cats-effect"         % "2.0.0-RC2",

  "co.fs2"                %% "fs2-core"            % fs2Version,
  "co.fs2"                %% "fs2-io"              % fs2Version,

  "org.scodec"            %% "scodec-stream"       % "2.0.0",
  "com.github.pureconfig" %% "pureconfig"          % "0.12.1",

  "io.circe"              %% "circe-core"          % circeVersion,
  "io.circe"              %% "circe-generic"       % circeVersion,
  "io.circe"              %% "circe-parser"        % circeVersion,

  "org.http4s"            %% "http4s-dsl"          % http4sVersion,
  "org.http4s"            %% "http4s-blaze-server" % http4sVersion,
  "org.http4s"            %% "http4s-circe"        % http4sVersion,

  "io.chrisdavenport"     %% "log4cats-core"       % log4catsVersion,
  "io.chrisdavenport"     %% "log4cats-slf4j"      % log4catsVersion,
  "org.slf4j"              % "slf4j-simple"        % "1.7.28",

  "org.scalactic"         %% "scalactic"           % "3.0.5",
  "org.scalatest"         %% "scalatest"           % "3.0.5" % "test"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-explaintypes",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-feature",
  "-unchecked",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ypartial-unification"
)
