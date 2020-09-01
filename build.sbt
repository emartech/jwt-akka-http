name         := "jwt-akka-http"
organization := "com.emarsys"
crossScalaVersions := List("2.13.1", "2.12.10")

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-language:higherKinds",
  "-Ywarn-dead-code",
  "-Xfatal-warnings",
  "-Xlint"
)

libraryDependencies ++= {
  val akkaV  = "2.5.31"
  val akkaHttpV  = "10.1.11"
  val scalaTestV = "3.2.2"
  Seq(
    "com.typesafe.akka" %% "akka-http"            % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpV % Test,
    "com.typesafe.akka" %% "akka-stream"          % akkaV,
    "com.typesafe.akka" %% "akka-stream-testkit"  % akkaV % Test,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    "org.scalatest"     %% "scalatest"            % scalaTestV % Test,
    "com.pauldijou"     %% "jwt-core"             % "4.1.0"
  )
}

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(List(
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("https://github.com/emartech/jwt-akka-http")),
  developers := List(
    Developer("itsdani", "Daniel Segesdi", "daniel.segesdi@emarsys.com", url("https://github.com/itsdani")),
    Developer("doczir", "Robert Doczi", "doczi.r@gmail.com", url("https://github.com/doczir")),
    Developer("tg44", "Gergo Torcsvari", "gergo.torcsvari@emarsys.com", url("https://github.com/tg44")),
    Developer("miklos-martin", "Miklos Martin", "miklos.martin@gmail.com", url("https://github.com/miklos-martin"))
  )
))
