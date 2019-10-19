package me.chuwy.kharms.common

import fs2.{io, text, Stream}

trait Source[F[_], A] {
  def get: Stream[F, A]
}

object Source {
  case class S3Source[F[_]](key: String) extends Source[F, String] {
    def get: Stream[F, String] = ???
  }

  case class FileSource[F[_]](file: String) extends Source[F, String] {
    def get: Stream[F, String] = ???
  }
}
