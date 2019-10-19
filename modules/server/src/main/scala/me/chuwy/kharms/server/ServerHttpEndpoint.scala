package me.chuwy.kharms.server

import java.util.UUID

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server.blaze._
import org.http4s.syntax.kleisli._
import org.http4s.circe._
import io.circe.syntax._
import me.chuwy.kharms.common.Config.ServerConf
import me.chuwy.kharms.common.Protocol.{ Response => KResponse }

object ServerHttpEndpoint {

  case class Records[F[_]](fifo: Ref[F, List[Array[Byte]]])

  def generateId[F[_]: Sync]: F[UUID] =
    Sync[F].delay(UUID.randomUUID())

  def initialize[F[_]: Sync]: F[Records[F]] =
    Ref.of[F, List[Array[Byte]]](Nil).map(Records.apply)

  def buildServer(state: Records[IO], conf: ServerConf)(implicit CS: ContextShift[IO], T: Timer[IO]) =
    BlazeServerBuilder[IO].bindHttp(conf.port, conf.host).withHttpApp(clientRegistryRoutes(state))

  def clientRegistryRoutes(state: Records[IO]): HttpApp[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "push" =>
      for {
        data     <- req.as[Array[Byte]]
        _        <- state.fifo.update { s => data :: s }
        response <- Ok(KResponse("Fin").asJson)
      } yield response
    case GET -> Root / "pull" =>
      for {
        data     <- state.fifo.modify {
          case Nil => (Nil, Array[Byte]())
          case a :: tail => (tail, a)
        }
        response <- Ok(KResponse(new String(data)).asJson)
      } yield response
  }.orNotFound
}
