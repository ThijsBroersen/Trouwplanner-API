import Dependencies._
import com.typesafe.sbt.packager.docker._
import sbtcrossproject.CrossProject
// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

ThisBuild / organization := "nl.thijsbroersen"
ThisBuild / scalaVersion := "2.12.8"

lazy val settings = commonSettings

lazy val compilerOptions = Seq(
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-language:reflectiveCalls",
  "-Ypartial-unification",
  "-deprecation",
  "-encoding",
  "utf8"
)

lazy val projectSettings = Seq(
//  homepage := Some(url("https://github.com/...L-space/L-space")),
//  licenses := List("MIT" -> url("https://opensource.org/licenses/MIT")),
  developers := List(
    Developer(
      "thijsbroersen",
      "Thijs Broersen",
      "thijsbroersen@gmail.com",
      url("https://github.com/ThijsBroersen")
    )
  )
)

lazy val commonSettings = projectSettings ++ Seq(
  scalacOptions ++= compilerOptions,
  organization := "nl.thijsbroersen",
  scalaVersion := "2.12.8",
  crossScalaVersions := Seq("2.12.8"),
  updateOptions := updateOptions.value.withCachedResolution(true)
)

val dirtyEnd = """(\+\d\d\d\d\d\d\d\d-\d\d\d\d)-SNAPSHOT$""".r
def stripTime(version: String) = dirtyEnd.findFirstIn(version) match {
  case Some(end) => version.stripSuffix(end).replace('+', '-') + "-SNAPSHOT"
  case None => version.replace('+', '-')
}

ThisBuild / version ~= stripTime
ThisBuild / dynver ~= stripTime

lazy val weddingplanner = project
  .in(file("."))
  .settings(skip in publish := true)
  .aggregate(weddingplannerNS.jvm, weddingplannerNS.js, weddingplannerApi, weddingplannerService)

lazy val weddingplannerNS: CrossProject = (crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure) in file("ns"))
  .settings(settings)
  .settings(
    name := "weddingplanner-ns",
    libraryDependencies ++= nsDeps.value
  )
  .jvmSettings()
  .jsSettings(
    jsEnv in Test := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()
  )

lazy val weddingplannerApi = (project in file("api"))
  .dependsOn(weddingplannerNS.jvm)
  .settings(settings)
  .settings(
    name := "weddingplanner-api",
    libraryDependencies ++= apiDeps.value
  )

lazy val weddingplannerService = (project in file("service"))
  .enablePlugins(DockerPlugin).enablePlugins(JDKPackagerPlugin)
  .dependsOn(weddingplannerApi)
  .settings(settings)
  .settings(
    name := "weddingplanner-service",
    libraryDependencies ++= serviceDeps.value,
    mainClass in Compile := Some("weddingplanner.server.WeddingPlannerService"),
    topLevelDirectory := None, // Don't add a root folder to the archive
    dockerBaseImage := "openjdk:11-jre",
    dockerUpdateLatest := true,
//    dockerExposedPorts := Seq(8080),
    daemonUser in Docker := "librarian",
//    daemonUserUid in Docker := Some(1000),
//    daemonGroup in Docker := "librarian",
//    daemonGroupGid in Docker := Some(1000),
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      ExecCmd("RUN", "usermod", "-u", "1000", "librarian"),
//      ExecCmd("RUN", "groupadd", "-g", "1000", "librarian"),
//      ExecCmd("RUN", "groupmod", "-g", "1000", "librarian"),
      Cmd("USER", "librarian")
    ),
    killTimeout := 5,
    termTimeout := 10,
    packageName in Docker := name.value
  )

val makeSettingsYml = Def.task {
  val file     = (resourceManaged in Compile).value / "site" / "data" / "settings.yml"
  val contents = s"version: ${version.value}"
  IO.write(file, contents)
  Seq(file)
}

lazy val site = (project in file("site"))
  .enablePlugins(MicrositesPlugin)
  .dependsOn(weddingplannerService % "compile->compile;compile->test")
  .settings(name := "weddingplanner-site")
  .settings(skip in publish := true)
  .settings(projectSettings)
  .settings(
    resourceGenerators in Compile += makeSettingsYml.taskValue,
    makeMicrosite := (makeMicrosite dependsOn makeSettingsYml).value
  )
  .settings(
    micrositeName := "Weddingplanner-API",
    micrositeDescription := "Services for planning a wedding (location, wedding official etc.",
    micrositeDataDirectory := (resourceManaged in Compile).value / "site" / "data",
    //    unmanagedResources ++= Seq(
    //
    //    ),
    //    micrositeDocumentationUrl := "/yoursite/docs",
    //    micrositeDocumentationLabelDescription := "Documentation",
    micrositeUrl := "https://thijsbroersen.github.io",
    micrositeBaseUrl := "/Weddingplanner-API",
    micrositeAuthor := "Thijs Broersen",
    micrositeHomepage := "https://thijsbroersen.github.io/Weddingplanner-API/",
    micrositeGithubOwner := "ThijsBroersen",
    micrositeGithubRepo := "Weddingplanner-API",
    micrositeGitterChannel := true,
    micrositeFooterText := Some(
      "")
  )
