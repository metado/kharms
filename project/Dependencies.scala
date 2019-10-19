import sbt._

object Dependencies {

  object V {
    val decline          = "0.6.2"
    val fs2              = "2.0.1"
    val catsFree         = "2.0.0"
    val http4s           = "0.21.0-M5"
    val circe            = "0.12.1"
    val scodec           = "2.0.0"

    val specs2           = "4.7.0"
    val scalaCheck       = "1.14.0"
  }

  val decline          = "com.monovore"          %% "decline"                      % V.decline
  val fs2              = "co.fs2"                %% "fs2-core"                     % V.fs2
  val fs2Io            = "co.fs2"                %% "fs2-io"                       % V.fs2
  val catsFree         = "org.typelevel"         %% "cats-free"                    % V.catsFree
  val http4s           = "org.http4s"            %% "http4s-dsl"                   % V.http4s
  val http4sServer     = "org.http4s"            %% "http4s-blaze-server"          % V.http4s
  val http4sClient     = "org.http4s"            %% "http4s-blaze-client"          % V.http4s
  val http4sCirce      = "org.http4s"            %% "http4s-circe"                 % V.http4s
  val circeGeneric     = "io.circe"              %% "circe-generic"                % V.circe
  val scodec           = "org.scodec"            %% "scodec-stream"                % V.scodec

  val simpleLogger     = "org.slf4j"             % "slf4j-simple"                  % "1.7.28"

  val specs2           = "org.specs2"            %% "specs2-core"                  % V.specs2        % Test
  val scalaCheck       = "org.scalacheck"        %% "scalacheck"                   % V.scalaCheck    % Test
}
