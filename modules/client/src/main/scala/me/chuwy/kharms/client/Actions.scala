package me.chuwy.kharms.client

import java.util.Base64

import io.circe.syntax._

import cats.implicits._
import cats.effect.Sync

import fs2.Stream

import org.http4s.{Method, Request}
import org.http4s.client.Client
import org.http4s.Uri
import org.http4s.Uri._

import scodec.bits.ByteVector
import me.chuwy.kharms.common.Protocol.{MessageReceived, RecordsResponse}

object Actions {
  case class Connection[F[_]](client: Client[F], host: String, port: Int)

  def push[F[_]: Sync](connection: Connection[F], data: ByteVector): F[MessageReceived] = {
    val authority = Authority(host = RegName(connection.host), port = Some(connection.port))
    val uri = Uri(authority = Some(authority), path = "/push")
    val req = Request[F](Method.POST, body = Stream.emits(data.toArray), uri = uri)
    connection.client.fetch(req)(_.as[MessageReceived])
  }

  def pull[F[_]: Sync](connection: Connection[F]): F[RecordsResponse] = {
    val authority = Authority(host = RegName(connection.host), port = Some(connection.port))
    val uri = Uri(authority = Some(authority), path = "/pull")
    val req = Request[F](Method.GET, uri = uri)
    connection.client.fetch(req)(_.as[RecordsResponse])
  }

  def interpret(response: MessageReceived): (Boolean, String) =
    (true, response.message)

  def interpret(response: RecordsResponse): (Boolean, String) =
    (true, response.records.map(array => new String(Base64.getEncoder.encode(array))).asJson.spaces2 )
}
