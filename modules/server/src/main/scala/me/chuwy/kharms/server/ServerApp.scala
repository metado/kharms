package me.chuwy.kharms.server

import java.util.concurrent.{ExecutorService, Executors}

import cats.syntax.functor._
import cats.effect.{Blocker, ExitCode, IO, IOApp}
import com.monovore.decline.Command
import me.chuwy.kharms.common.Config

import scala.concurrent.ExecutionContext

object ServerApp extends IOApp {

  val a = Executors.newFixedThreadPool(2)
  val b = ExecutionContext.fromExecutor(a)
  val blocker = Blocker.liftExecutionContext(b)

  val command = Command("kharms-server", "Kharms server")(Config.serverConf)

  def run(args: List[String]): IO[ExitCode] =
    command.parse(args) match {
      case Right(config) =>
        for {
          registry <- ServerHttpEndpoint.initializeState[IO](config.storage, blocker)
          _        <- ServerHttpEndpoint.buildServer(registry, config).serve.compile.drain
        } yield ExitCode.Success
      case Left(error) =>
        IO(System.err.println(error)).as(ExitCode(2))
    }
}
