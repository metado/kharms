package me.chuwy.kharms.common

import java.util.Base64

import cats.implicits._
import com.monovore.decline._
import scodec.bits.ByteVector


object Config {

  private val Base64Encoder = Base64.getEncoder

  val DefaultServerHost = "localhost"
  val DefaultServerPort = 8000

  val host: Opts[String] = Opts.option[String]("host", short = "h", metavar = "str", help = "Hostname to bind").withDefault(DefaultServerHost)
  val port: Opts[Int] = Opts.option[Int]("port", short = "p", metavar = "int", help = "Port to bind").withDefault(DefaultServerPort)

  val data: Opts[String] = Opts.option[String]("data", short = "d", metavar = "base64", help = "Base64-encoded")
  val raw: Opts[Boolean] = Opts.flag("raw", "Pass raw bytes instead of string").orFalse

  case class ServerConf(host: String, port: Int)
  val serverConf: Opts[ServerConf] = (host, port).mapN(ServerConf.apply)


  sealed trait Action extends Product with Serializable
  object Action {
    case class Push(data: ByteVector) extends Action
    case object Pull extends Action

    val opts = Opts.subcommand[Action]("push", "Push data to a stream") { (data, raw).mapN {
      case (s, true) => Push(ByteVector(Base64.getDecoder.decode(s)))
      case (s, false) => Push(ByteVector(s.getBytes))
    } } orElse(Opts.subcommand[Action]("pull", "Pull data from a stream")(Opts(Pull)))
  }

  case class ClientConf(command: Action, serverHost: String, serverPort: Int)
  val clientConf: Opts[ClientConf] = (Action.opts, host, port).mapN(ClientConf.apply)
}
