package com.github.oskin1.proxy.models

import com.github.oskin1.proxy.commonCodecs
import scodec.codecs.{int16, int32, _}
import scodec.{Codec, _}

final case class Trade(
  len: Int,
  timestamp: Long,
  ticker: String,
  price: Double,
  volume: Int
)

object Trade {

  import commonCodecs._

  implicit val codec: Codec[Trade] =
    (int16 :: int64 :: ascii16 :: double :: int32).as[Trade]
}
