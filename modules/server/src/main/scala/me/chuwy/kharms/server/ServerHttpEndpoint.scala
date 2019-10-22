package me.chuwy.kharms.server

import java.time.Instant
import java.nio.file.{ Path, Files }

import scala.collection.immutable.Queue

import fs2.concurrent.{ Queue => StreamQueue }

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

  case class State(messages: Queue[Message], last: Long)

  val init = State(Queue.empty, 0L)

  case class Records[F[_]](fifo: Ref[F, State], sink: StreamQueue[F, Message])

  def initializeState[F[_]: ContextShift: Concurrent](storage: Path, blocker: Blocker): F[Records[F]] = {
    for {
      dir     <- Sync[F].delay(Files.isDirectory(storage))
      _       <- if (dir) Sync[F].unit else Sync[F].raiseError[Unit](new RuntimeException(s"Storage path $storage is not a directory"))
      queue   <- StreamQueue.unbounded[F, Message]
      sink     = Storage.write[F](storage, blocker)
      fiber   <- Concurrent[F].start(queue.dequeue.through(sink).compile.drain)
      records <- Ref.of[F, State](init).map(ref => Records(ref, queue))
    } yield records
  }

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
          (State(existing :+ message, updated), meta)
      }
    } yield meta

  object Max extends OptionalQueryParamDecoderMatcher[Int]("max")
  object Ack extends OptionalQueryParamDecoderMatcher[Boolean]("ack")

  def clientRegistryRoutes(state: Records[IO]): HttpApp[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "push" =>
      for {
        payload  <- req.as[Array[Byte]]
        meta     <- addPayload(state, payload)
        message   = Message(meta, payload)
        _        <- state.sink.enqueue1(message)
        response <- Ok(meta.asJson)
      } yield response
    case GET -> Root / "pull" :? Max(max) :? Ack(ack) =>
      val maxElem = max.getOrElse(5)
      for {
        data <- state.fifo.modify {
          case State(existing, i) if ack.contains(true) =>
            val (toRespond, remaining) = existing.splitAt(maxElem)
            (State(remaining, i), RecordsResponse(toRespond.toList))
          case State(existing, i) =>
            val toRespond = existing.take(maxElem)
            (State(existing, i), RecordsResponse(toRespond.toList))
        }
        response <- Ok(data.asJson)
      } yield response
  }.orNotFound
}
