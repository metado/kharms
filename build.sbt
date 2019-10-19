ThisBuild / version := "0.1.0-rc1"
ThisBuild / organization := "me.chuwy"
ThisBuild / scalaVersion := "2.12.10"
ThisBuild / scalacOptions := BuildSettings.scalacOptions
ThisBuild / scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings")

lazy val common = project.in(file("modules/common"))
  .settings(name := "kharms-common")
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](organization, name, version),
    buildInfoPackage := "me.chuwy.kharms.generated"
  )
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.decline,
      Dependencies.fs2,
      Dependencies.fs2Io,
      Dependencies.catsFree,
      Dependencies.http4s,
      Dependencies.http4sServer,
      Dependencies.http4sClient,
      Dependencies.http4sCirce,
      Dependencies.circeGeneric,
      Dependencies.simpleLogger,
      Dependencies.scodec,

      Dependencies.specs2,
      Dependencies.scalaCheck
    )
  )
  .settings(BuildSettings.helpersSettings)

lazy val server = project.in(file("modules/server"))
  .settings(name := "kharms-server")
  .dependsOn(common)

lazy val client = project.in(file("modules/client"))
  .settings(name := "kharms-client")
  .dependsOn(common)

