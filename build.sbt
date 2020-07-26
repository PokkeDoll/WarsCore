name := "WarsCore"

version := "0.1"

scalaVersion := "2.13.3"

resolvers += "papermc" at "https://papermc.io/repo/repository/maven-public/"
resolvers += "jitpack.io" at "https://jitpack.io"

libraryDependencies ++= Seq(
  "com.destroystokyo.paper" % "paper-api" % "1.12.2-R0.1-SNAPSHOT" % Provided,
  "com.zaxxer" % "HikariCP" % "3.4.2" % Provided,
  "com.github.MilkBowl" % "VaultAPI" % "1.7"
)

val libs = Seq(
  "lib/scala-library-2.13.3.jar",
  "lib/HikariCP-3.4.2.jar"
)

packageOptions in (Compile, packageBin) +=
  Package.ManifestAttributes("Class-Path" -> libs.mkString(" "))

artifactName :={(sv: ScalaVersion,module: ModuleID, artifact: Artifact) => "WarsCore-" + module.revision + "." + artifact.extension}