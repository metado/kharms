package me.chuwy.kharms.server

import java.nio.file.{Files, Path}

import scala.collection.convert.ImplicitConversionsToScala._
import cats.effect.{Blocker, ContextShift, Sync}

import fs2.Stream
import fs2.io.file
import fs2.text.utf8Decode

object Storage {
  def readFromDir[F[_]: ContextShift: Sync](dir: Path, blocker: Blocker, seq: Long): Stream[F, String] = {
    for {
      files <- Stream.eval(Sync[F].delay(Files.list(dir)))
      path <- Stream.fromIterator(files.iterator())
      line <- file.readAll(path, blocker, 4096).through(utf8Decode)
    } yield line
  }
}
