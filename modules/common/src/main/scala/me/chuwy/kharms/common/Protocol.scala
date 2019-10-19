package me.chuwy.kharms.common

import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{ jsonOf, jsonEncoderOf }

object Protocol {
  /** Successful push response */
  case class MessageReceived(message: String)

  implicit val circeMessageReceivedDecoder: Decoder[MessageReceived] = deriveDecoder[MessageReceived]
  implicit val circeMessageReceivedEncoder: Encoder[MessageReceived] = deriveEncoder[MessageReceived]
  implicit def http4sMessageReceivedDecoder[F[_]: Sync]: EntityDecoder[F, MessageReceived] =
    jsonOf[F, MessageReceived]
  implicit def http4sMessageReceivedEncoder[F[_]: Sync]: EntityEncoder[F, MessageReceived] =
    jsonEncoderOf[F, MessageReceived]

  /** Pull response */
  case class RecordsResponse(records: List[Array[Byte]])

  implicit val circeRecordsResponseDecoder: Decoder[RecordsResponse] = deriveDecoder[RecordsResponse]
  implicit val circeRecordsResponseEncoder: Encoder[RecordsResponse] = deriveEncoder[RecordsResponse]
  implicit def http4sRecordsResponseDecoder[F[_]: Sync]: EntityDecoder[F, RecordsResponse] =
    jsonOf[F, RecordsResponse]
  implicit def http4sRecordsResponseEncoder[F[_]: Sync]: EntityEncoder[F, RecordsResponse] =
    jsonEncoderOf[F, RecordsResponse]

}
