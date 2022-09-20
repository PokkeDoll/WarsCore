name := "WarsCore"

version := "3.0.0"

scalaVersion := "3.2.0"

organization := "hm.moe.pokkedoll"

crossPaths := false

resolvers += "papermc" at "https://papermc.io/repo/repository/maven-public/"
resolvers += "jitpack.io" at "https://jitpack.io"
resolvers += "dmulloy2-repo" at "https://repo.dmulloy2.net/nexus/repository/public/"
resolvers += "sk89q-repo" at "https://maven.enginehub.org/repo/"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  "io.papermc.paper" % "paper-api" % "1.17.1-R0.1-SNAPSHOT" % Provided,
  "com.zaxxer" % "HikariCP" % "5.0.1" % Provided
)

val libs = Seq(
  "lib/scala3-library_3-3.2.0.jar",
  "lib/HikariCP-5.0.1.jar",
  "lib/CrackShotPP-lib.jar"
)

Compile / packageBin / packageOptions +=
// packageOptions in (Compile, packageBin) +=
  Package.ManifestAttributes("Class-Path" -> libs.mkString(" "))

// artifactName :={(sv: ScalaVersion,module: ModuleID, artifact: Artifact) => "WarsCore-" + module.revision + "." + artifact.extension}
