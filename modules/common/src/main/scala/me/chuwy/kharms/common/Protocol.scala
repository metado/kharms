package me.chuwy.kharms.common

import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{ jsonOf, jsonEncoderOf }

object Protocol {
  case class Response(message: String)

  implicit val circeResponseDecoder: Decoder[Response] = deriveDecoder[Response]

  implicit val circeResponseEncoder: Encoder[Response] = deriveEncoder[Response]

  implicit def http4sResponseDecoder[F[_]: Sync]: EntityDecoder[F, Response] =
    jsonOf[F, Response]

  implicit def http4sResponseEncoder[F[_]: Sync]: EntityEncoder[F, Response] =
    jsonEncoderOf[F, Response]
}
