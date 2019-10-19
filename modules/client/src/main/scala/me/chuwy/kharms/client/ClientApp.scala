package me.chuwy.kharms.client

import scala.concurrent.ExecutionContext.global

import cats.implicits._
import cats.effect.{ExitCode, IO, IOApp}

import com.monovore.decline.Command

import me.chuwy.kharms.common.Config

import org.http4s.client.blaze.BlazeClientBuilder

object ClientApp extends IOApp {

  val command = Command("kharms-client", "Kharms client")(Config.clientConf)

  def run(args: List[String]): IO[ExitCode] =
    command.parse(args) match {
      case Right(config) =>
        BlazeClientBuilder[IO](global).resource.use { client =>
          val connection = Actions.Connection(client, config.serverHost, config.serverPort)
          for {
            response <- config.command match {
              case Config.Action.Push(data) =>
                Actions.push(connection, data)
              case Config.Action.Pull =>
                Actions.pull(connection)
            }
            (success, message) = Actions.interpret(response)
            _ <- IO.delay(println(message))
          } yield if (success) ExitCode.Success else ExitCode.Error
        }
      case Left(error) =>
        IO(System.err.println(error)).as(ExitCode(2))
    }
}
