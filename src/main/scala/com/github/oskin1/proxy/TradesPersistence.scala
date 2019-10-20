package com.github.oskin1.proxy

import java.sql.Timestamp

import cats.effect.IO
import cats.effect.concurrent.Ref
import com.github.oskin1.proxy.models.{CandlestickItem, Trade}

import scala.collection.immutable.TreeMap

/** In-memory persistence temporarily holding trades aggregated by epochs.
  */
final class TradesPersistence(
  completeEpochTrades: Map[Long, IndexedSeq[Trade]],
  incompleteEpochTrades: IndexedSeq[Trade],
  currentEpochOpt: Option[Long],
  keepEpochs: Int
) {

  /** Select specified number of last epochs and convert them
    * to candlesticks aggregated form.
    */
  def getAggregated(lastEpochs: Int): Seq[CandlestickItem] =
    completeEpochTrades
      .takeRight(lastEpochs)
      .flatMap {
        case (epoch, trades0) =>
          trades0.groupBy(_.ticker).map {
            case (ticker, trades) =>
              val open = trades.head.price
              val close = trades.last.price
              val sortedPrices = trades.map(_.price).sorted
              val low = sortedPrices.head
              val high = sortedPrices.last
              val volume = trades.map(_.volume).sum
              val ts = new Timestamp(epoch).toString
              models.CandlestickItem(ticker, ts, open, high, low, close, volume)
          }
      }
      .toSeq

  /** Add new trade to persistence pruning outdated epoch if one exists.
    */
  def add(trade: Trade): TradesPersistence = {
    val epoch = epochOf(trade)
    val (updatedComplete, updatedIncomplete) =
      currentEpochOpt match {
        case Some(currentEpoch) if currentEpoch != epoch =>
          val minEpoch = epoch - (keepEpochs * 60 * 1000)
          completeEpochTrades
            .filter { case (epoch, _) => epoch >= minEpoch }
            .updated(currentEpoch, incompleteEpochTrades) -> IndexedSeq(trade)
        case _ =>
          completeEpochTrades -> (incompleteEpochTrades :+ trade)
      }
    new TradesPersistence(updatedComplete, updatedIncomplete, Some(epoch), keepEpochs)
  }

  private def epochOf(trade: Trade): Long =
    trade.timestamp - (trade.timestamp % (60 * 1000))
}

object TradesPersistence {

  def empty[F[_]](keepEpochs: Int): IO[Ref[IO, TradesPersistence]] =
    Ref.of[IO, TradesPersistence](
      new TradesPersistence(TreeMap.empty, IndexedSeq.empty, None, keepEpochs)
    )
}
