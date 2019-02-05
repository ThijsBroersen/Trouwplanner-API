import sbt._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

// Versions
object Version {
  val scala = "2.12.8"
  val lspace = "0.3.0.1"
  val `ns.lspace` = "0.0.6.1"
}

// Libraries
object Library {
  val lspaceServices = "eu.l-space" %% "lspace-services" % Version.lspace
  val lspaceServicesTests = "eu.l-space" %% "lspace-services" % Version.lspace % "test" classifier "tests"
  val lspaceNS = "eu.l-space" %% "lspace-ns" % Version.`ns.lspace`

  val scalaCsv = "com.github.tototoshi" %% "scala-csv" % "1.3.5"
  val scalaCsvRefined = "com.nrinaudo" %% "kantan.csv-refined" % "0.5.0"

  val ciris = "is.cir" %% "ciris-core" % "0.12.1"

  val scalaTest =
    Def.setting("org.scalatest" %%% "scalatest" % "3.0.5" % "test")
}

object Dependencies {
  import Library._

  val nsDeps =
    Def.setting(Seq(lspaceServices, lspaceNS, scalaTest.value))

  val apiDeps =
    Def.setting(Seq(lspaceServices, lspaceNS, scalaTest.value))

  val serviceDeps =
    Def.setting(
      Seq(lspaceServices,
          lspaceServicesTests,
          lspaceNS,
          ciris,
          scalaCsv,
          scalaTest.value))
}
