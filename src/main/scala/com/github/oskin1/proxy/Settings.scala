package com.github.oskin1.proxy

import java.net.InetSocketAddress

import scala.concurrent.duration.FiniteDuration

final case class Settings(
  listenAddress: InetSocketAddress,
  serverAddress: InetSocketAddress,
  connectRetryInterval: FiniteDuration,
  epochLength: FiniteDuration,
  keepLastEpochs: Int,
  maxChunkSize: Int
)
