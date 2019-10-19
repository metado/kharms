package me.chuwy.kharms.server

import java.util.UUID
import java.time.Instant

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.io._
import org.http4s.server.blaze._
import org.http4s.syntax.kleisli._
import org.http4s.circe._
import io.circe.syntax._
import me.chuwy.kharms.common.Config.ServerConf
import me.chuwy.kharms.common.Protocol._

object ServerHttpEndpoint {

  case class State(messages: List[Message], last: Long)

  val init = State(Nil, 0L)

  case class Records[F[_]](fifo: Ref[F, State])

  def initialize[F[_]: Sync]: F[Records[F]] =
    Ref.of[F, State](init).map(Records.apply)

  def buildServer(state: Records[IO], conf: ServerConf)(implicit CS: ContextShift[IO], T: Timer[IO]) =
    BlazeServerBuilder[IO].bindHttp(conf.port, conf.host).withHttpApp(clientRegistryRoutes(state))

  def addPayload[F[_]: Sync](current: Records[F], payload: Array[Byte]): F[Meta] =
    for {
      now <- Sync[F].delay(Instant.now())
      meta <- current.fifo.modify {
        case State(existing, q) =>
          val updated = q + 1
          val meta = Meta(updated, now)
          val message = Message(meta, payload)
          (State(message :: existing, updated), meta)
      }
    } yield meta

  def clientRegistryRoutes(state: Records[IO]): HttpApp[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "push" =>
      for {
        payload  <- req.as[Array[Byte]]
        meta     <- addPayload(state, payload)
        response <- Ok(meta.asJson)
      } yield response
    case GET -> Root / "pull" =>
      for {
        data <- state.fifo.modify {
          case State(existing, i) => (State(Nil, i), RecordsResponse(existing))
        }
        response <- Ok(data.asJson)
      } yield response
  }.orNotFound
}
