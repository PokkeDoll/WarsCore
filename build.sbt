name := "WarsCore"

version := "2.2"

scalaVersion := "2.13.7"

organization := "hm.moe.pokkedoll"

crossPaths := false

resolvers += "papermc" at "https://papermc.io/repo/repository/maven-public/"
resolvers += "jitpack.io" at "https://jitpack.io"
resolvers += "dmulloy2-repo" at "https://repo.dmulloy2.net/nexus/repository/public/"
resolvers += "sk89q-repo" at "https://maven.enginehub.org/repo/"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.9" % Test,
  "io.papermc.paper" % "paper-api" % "1.17.1-R0.1-SNAPSHOT" % Provided,
  "com.zaxxer" % "HikariCP" % "5.0.0" % Provided,
  // "com.sk89q.worldguard" % "worldguard-bukkit" % "7.0.5" % Compile
)

val libs = Seq(
  "lib/scala-library-2.13.7.jar",
  "lib/HikariCP-5.0.0.jar",
  "lib/CrackShotPP-lib.jar"
)

Compile / packageBin / packageOptions +=
// packageOptions in (Compile, packageBin) +=
  Package.ManifestAttributes("Class-Path" -> libs.mkString(" "))

// artifactName :={(sv: ScalaVersion,module: ModuleID, artifact: Artifact) => "WarsCore-" + module.revision + "." + artifact.extension}

// publish
githubOwner := "PokkeDoll"
githubRepository := "WarsCore"
githubTokenSource := TokenSource.GitConfig("github.token")