package com.github.oskin1.proxy

import java.nio.charset.Charset

import com.github.oskin1.proxy.models.CandlestickItem
import io.circe.generic.auto._
import io.circe.syntax._
import scodec.bits.BitVector
import scodec.codecs.{int16, string, variableSizeBytes}
import scodec.{Attempt, Codec, Encoder, SizeBound}

object commonCodecs {

  implicit val candlesticksEncoder: Encoder[List[CandlestickItem]] =
    new Encoder[List[CandlestickItem]] {
      override def encode(value: List[CandlestickItem]): Attempt[BitVector] =
        Attempt.successful(
          BitVector(
            value
              .map(_.asJson.noSpaces)
              .mkString("\n")
              .getBytes(commonCodecs.platform)
          )
        )
      override def sizeBound: SizeBound = SizeBound.unknown
    }

  lazy val platform: Charset = Charset.forName("US-ASCII")

  val ascii16: Codec[String] = string16(platform)

  def string16(implicit charset: Charset): Codec[String] =
    variableSizeBytes(int16, string(charset))
      .withToString(s"string16(${charset.displayName})")
}
