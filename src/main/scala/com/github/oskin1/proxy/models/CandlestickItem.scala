package com.github.oskin1.proxy.models

final case class CandlestickItem(
  ticker: String,
  timestamp: String,
  open: Double,
  high: Double,
  low: Double,
  close: Double,
  volume: Int
)
