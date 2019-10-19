package me.chuwy.kharms.client

import java.util.Base64

import io.circe.syntax._

import cats.effect.Sync

import fs2.Stream

import org.http4s.{Method, Query, Request, Uri}
import org.http4s.Uri._
import org.http4s.client.Client

import scodec.bits.ByteVector

import me.chuwy.kharms.common.Protocol.{Meta, RecordsResponse}

object Actions {

  case class Connection[F[_]](client: Client[F], host: String, port: Int)

  def push[F[_]: Sync](connection: Connection[F], data: ByteVector): F[Meta] = {
    val authority = Authority(host = RegName(connection.host), port = Some(connection.port))
    val uri = Uri(authority = Some(authority), path = "/push")
    val req = Request[F](Method.POST, body = Stream.emits(data.toArray), uri = uri)
    connection.client.fetch(req)(_.as[Meta])
  }

  def pull[F[_]: Sync](connection: Connection[F], max: Int, ack: Boolean): F[RecordsResponse] = {
    val authority = Authority(host = RegName(connection.host), port = Some(connection.port))
    val query = Query(("max", Some(max.toString)), ("ack", Some(ack.toString)))
    val uri = Uri(authority = Some(authority), path = "/pull", query = query)
    val req = Request[F](Method.GET, uri = uri)
    connection.client.fetch(req)(_.as[RecordsResponse])
  }

  def interpret(response: Meta): (Boolean, String) =
    (true, response.asJson.spaces2)

  def interpret(response: RecordsResponse): (Boolean, String) =
    (true, response.records.map(message => new String(Base64.getEncoder.encode(message.payload))).asJson.spaces2 )
}
