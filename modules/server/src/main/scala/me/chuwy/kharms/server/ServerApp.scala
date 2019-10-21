package me.chuwy.kharms.server

import cats.syntax.functor._
import cats.effect.{ExitCode, IO, IOApp}

import com.monovore.decline.Command

import me.chuwy.kharms.common.Config

object ServerApp extends IOApp {

  val command = Command("kharms-server", "Kharms server")(Config.serverConf)

  def run(args: List[String]): IO[ExitCode] =
    command.parse(args) match {
      case Right(config) =>
        for {
          registry <- ServerHttpEndpoint.initializeState[IO]
          _        <- ServerHttpEndpoint.buildServer(registry, config).serve.compile.drain
        } yield ExitCode.Success
      case Left(error) =>
        IO(System.err.println(error)).as(ExitCode(2))
    }
}
