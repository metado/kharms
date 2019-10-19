package me.chuwy.kharms.common

import java.time.Instant

import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}

/** Entities common for Client and Server */
object Protocol {
  case class Message(meta: Meta, payload: Array[Byte])

  implicit val circeMessageDecoder: Decoder[Message] = deriveDecoder[Message]
  implicit val circeMessageEncoder: Encoder[Message] = deriveEncoder[Message]
  implicit def http4sMessageDecoder[F[_]: Sync]: EntityDecoder[F, Message] =
    jsonOf[F, Message]
  implicit def http4sMessageEncoder[F[_]: Sync]: EntityEncoder[F, Message] =
    jsonEncoderOf[F, Message]


  /** Metadata assigned by server */
  case class Meta(id: Long, tstamp: Instant)

  implicit val circeMetaDecoder: Decoder[Meta] = deriveDecoder[Meta]
  implicit val circeMetaEncoder: Encoder[Meta] = deriveEncoder[Meta]
  implicit def http4sMetaDecoder[F[_]: Sync]: EntityDecoder[F, Meta] =
    jsonOf[F, Meta]
  implicit def http4sMetaEncoder[F[_]: Sync]: EntityEncoder[F, Meta] =
    jsonEncoderOf[F, Meta]


  /** Successful push response */
  case class Pushed(meta: Meta)

  implicit val circeMessageReceivedDecoder: Decoder[Pushed] = deriveDecoder[Pushed]
  implicit val circeMessageReceivedEncoder: Encoder[Pushed] = deriveEncoder[Pushed]
  implicit def http4sMessageReceivedDecoder[F[_]: Sync]: EntityDecoder[F, Pushed] =
    jsonOf[F, Pushed]
  implicit def http4sMessageReceivedEncoder[F[_]: Sync]: EntityEncoder[F, Pushed] =
    jsonEncoderOf[F, Pushed]


  /** Pull response */
  case class RecordsResponse(records: List[Message])

  implicit val circeRecordsResponseDecoder: Decoder[RecordsResponse] = deriveDecoder[RecordsResponse]
  implicit val circeRecordsResponseEncoder: Encoder[RecordsResponse] = deriveEncoder[RecordsResponse]
  implicit def http4sRecordsResponseDecoder[F[_]: Sync]: EntityDecoder[F, RecordsResponse] =
    jsonOf[F, RecordsResponse]
  implicit def http4sRecordsResponseEncoder[F[_]: Sync]: EntityEncoder[F, RecordsResponse] =
    jsonEncoderOf[F, RecordsResponse]

}
