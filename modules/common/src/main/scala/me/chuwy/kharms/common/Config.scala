package me.chuwy.kharms.common

import java.util.Base64
import java.nio.file.Path

import cats.data.Validated
import cats.implicits._

import com.monovore.decline._

import scodec.bits.ByteVector


object Config {

  val DefaultServerHost = "localhost"
  val DefaultServerPort = 8000

  val DefaultMax = 5

  // Common
  val host: Opts[String] = Opts.option[String]("host", short = "h", metavar = "str", help = "Hostname to bind").withDefault(DefaultServerHost)
  val port: Opts[Int] = Opts.option[Int]("port", short = "p", metavar = "int", help = "Port to bind").withDefault(DefaultServerPort)

  // Server
  val storage: Opts[Path] = Opts.option[Path]("storage", short = "d", metavar = "str", help = "Path to persistent storage")

  // Push
  val data: Opts[String] = Opts.option[String]("data", short = "d", metavar = "base64", help = "Base64-encoded")
  val raw: Opts[Boolean] = Opts.flag("raw", "Pass raw bytes instead of string").orFalse

  // Pull
  val ack: Opts[Boolean] = Opts.flag("ack", "Ack all received messages").orFalse
  val max: Opts[Int] = Opts.option[Int]("max", "Maximum items to receive").withDefault(DefaultMax)

  case class ServerConf(host: String, port: Int, storage: Path)
  val serverConf: Opts[ServerConf] = (host, port, storage).mapN(ServerConf.apply)


  sealed trait Action extends Product with Serializable
  object Action {
    case class Push(data: ByteVector) extends Action
    case class Pull(max: Int, ack: Boolean) extends Action

    val pushOpts = Opts.subcommand[Push]("push", "Push data to a stream") { (data, raw).mapN {
      case (s, true) => Validated.catchOnly[IllegalArgumentException](Base64.getDecoder.decode(s)).leftMap(_.getMessage)
      case (s, false) => s.getBytes.valid[String]
    }.mapValidated(_.map(bytes => Push(ByteVector(bytes))).toValidatedNel)}

    val pullOpts = Opts.subcommand[Pull]("pull", "Pull data from a stream") { (max, ack).mapN(Pull.apply) }

    val opts = pushOpts.orElse(pullOpts)
  }

  case class ClientConf(command: Action, serverHost: String, serverPort: Int)
  val clientConf: Opts[ClientConf] = (Action.opts, host, port).mapN(ClientConf.apply)
}
