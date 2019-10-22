package me.chuwy.kharms.server

import java.nio.file.{Files, Path, Paths}
import java.time.Instant

import scala.collection.convert.ImplicitConversionsToScala._
import cats.effect.{Blocker, ContextShift, Sync}
import fs2.{Pipe, Pull, Stream}
import fs2.io.file
import fs2.text.utf8Decode
import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs.implicits._
import scodec.stream.StreamEncoder
import me.chuwy.kharms.common.Protocol.{Message, Meta}

object Storage {

  val MaxPerFile = 8

  case class SeqFile(from: Long, to: Long, fullPath: Path) {
    def has(seq: Long): Boolean = seq >= from && seq < to
  }

  object SeqFile {
    private object IntStr {
      def unapply(arg: String): Option[Long] =
        try { Some(arg.toLong) } catch { case _: NumberFormatException => None }
    }

    def fromPath(path: Path): Either[String, SeqFile] = {
      val fileName = path.getFileName.toString
      fileName.split("-").toList match {
        case List("seq", IntStr(from), IntStr(to)) => Right(SeqFile(from, to, path))
        case _ => Left(s"Filename $path doesn't match sequence file format")
      }
    }
  }

  // Every file has dir/seq-00000001-00000010 format, meaning that files contain records from 1-to-10
  def readFromDir[F[_]: ContextShift: Sync](dir: Path, blocker: Blocker, seq: Long): Stream[F, String] = {
    val allFiles = for {
      files <- Stream.eval(Sync[F].delay(Files.list(dir)))
      seqFile <- Stream.fromIterator(files.iterator()).flatMap { p => SeqFile.fromPath(p) match {
        case Right(seqFile) => Stream.emit(seqFile)
        case Left(error) => Stream.raiseError(new RuntimeException(error))
      }}
    } yield seqFile
    val data = allFiles.dropWhile(file => !file.has(seq)).flatMap {
      case SeqFile(from, _, fullPath) if from >= seq => file.readAll(fullPath, blocker, 4096)
      case SeqFile(from, _, fullPath) => ??? // Read from pointer
    }
    data.through(utf8Decode)
  }

  def readIndex[F[_]: ContextShift: Sync](index: Path, blocker: Blocker, start: Long) =
    file.readRange(index, blocker, 8, start, start + 8)
      .chunks
      .map { chunk => chunk.toByteBuffer.getLong }

  implicit val arrayEncoder: Codec[Array[Byte]] =
    Codec[BitVector].xmap(_.toByteArray, (x: Array[Byte]) => BitVector.apply(x))

  implicit val instantCodec: Codec[Instant] =
    Codec[Long].xmap(Instant.ofEpochMilli, _.toEpochMilli)

  def baseMetaCodec = Codec[Long] :: Codec[Instant]
  implicit val metaMetaCodec: Codec[Meta] = baseMetaCodec.as[Meta]

  def baseMessageCodec = Codec[Meta] :: Codec[Array[Byte]]
  implicit def metaMessageCodec: Codec[Message] = baseMessageCodec.as[Message]

  def getFile(base: Path, id: Long): SeqFile = {
    val from = id
    val to = from + MaxPerFile
    val fullPath = Paths.get(base.toAbsolutePath.toString, s"seq-$from-$to")
    SeqFile(from, to, fullPath)
  }

  def write[F[_]: ContextShift: Sync](dir: Path, blocker: Blocker): Pipe[F, Message, Unit] = {
    val bytePipe = StreamEncoder.many(metaMessageCodec).toPipe

    /** Declaratively create stream of byte-sinks */
    def sinks: Stream[fs2.Pure, Pipe[F, Byte, Unit]] = {
      def go(i: Int): Stream[fs2.Pure, Int] =
        Stream.emit(i).flatMap { ii => Stream.emit(ii) ++ go(ii + MaxPerFile) }

      go(1)
        .map { i => getFile(dir, i.toLong) }
        .map { s => file.writeAll(s.fullPath, blocker) }
    }

    messages => {
      sinks.flatMap { sink =>
        messages.take(MaxPerFile)
          .through(bytePipe)
          .flatMap(bytes => Stream.emits(bytes.toByteArray))
          .through(sink)
      }
    }
  }
}
