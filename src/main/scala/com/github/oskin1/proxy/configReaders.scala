package com.github.oskin1.proxy

import java.net.InetSocketAddress

import pureconfig.ConfigReader
import pureconfig.error.{CannotConvert, FailureReason}

import scala.util.Try

object configReaders {

  implicit val inetSocketAddressReader: ConfigReader[InetSocketAddress] =
    ConfigReader[String].emap(parseAddressString)

  private def parseAddressString(
    s: String
  ): Either[FailureReason, InetSocketAddress] =
    Try {
      val host = s.split(":").head
      val port = s.split(":").last.toInt
      new InetSocketAddress(host, port)
    }.fold(e => Left(CannotConvert(s, "InetSocketAddress", e.getMessage)), Right(_))
}
