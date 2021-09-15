name := "WarsCore"

version := "1.14.0"

scalaVersion := "2.13.6"

javacOptions ++= Seq("-source", "11", "-target", "11")

organization := "hm.moe.pokkedoll"

crossPaths := false

resolvers += "papermc" at "https://papermc.io/repo/repository/maven-public/"
resolvers += "jitpack.io" at "https://jitpack.io"
resolvers += "dmulloy2-repo" at "https://repo.dmulloy2.net/nexus/repository/public/"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.9" % Test,
  "com.destroystokyo.paper" % "paper-api" % "1.16.4-R0.1-SNAPSHOT" % Provided,
  "com.zaxxer" % "HikariCP" % "5.0.0" % Provided
)

val libs = Seq(
  "lib/scala-library-2.13.6.jar",
  "lib/HikariCP-5.0.0.jar",
  "lib/CrackShotPP-lib.jar"
)

packageOptions in (Compile, packageBin) +=
  Package.ManifestAttributes("Class-Path" -> libs.mkString(" "))

// artifactName :={(sv: ScalaVersion,module: ModuleID, artifact: Artifact) => "WarsCore-" + module.revision + "." + artifact.extension}

// publish
githubOwner := "PokkeDoll"
githubRepository := "WarsCore"
githubTokenSource := TokenSource.GitConfig("github.token")