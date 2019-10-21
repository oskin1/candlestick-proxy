package com.github.oskin1.proxy

import java.net.{InetSocketAddress, SocketException}

import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, ContextShift, Timer}
import com.github.oskin1.proxy.models.Trade
import io.chrisdavenport.log4cats.Logger
import fs2._
import fs2.io.tcp.{Socket, SocketGroup}
import scodec.Decoder
import scodec.stream.StreamDecoder

/** Listens to an upstream of events and persists them.
  */
object TradesListener {

  def run[F[_]: Concurrent: ContextShift: Timer: Logger](
    tradesPersistenceRef: Ref[F, TradesPersistence],
    socketGroup: SocketGroup,
    settings: Settings
  ): Stream[F, Unit] =
    connect(socketGroup, settings.listenAddress)
      .flatMap { socket =>
        Stream.eval(Logger[F].info(s"Connected.")) >>
        repeatRead(socket, settings.maxChunkSize).evalMap(
          trade => tradesPersistenceRef.update(_.add(trade))
        )
      }
      .handleErrorWith {
        case e: SocketException =>
          val retryIn = settings.connectRetryInterval
          Stream.eval(Logger[F].info(s"${e.getMessage}. Retrying in $retryIn")) >>
          run(tradesPersistenceRef, socketGroup, settings).delayBy(retryIn)
        case e =>
          Stream.raiseError[F](e)
      }

  /** Opens a connection to the specified server represented as a [[Socket]].
    * The connection is closed when the resource is released.
    */
  private def connect[F[_]: Concurrent: ContextShift: Logger](
    socketGroup: SocketGroup,
    address: InetSocketAddress
  ): Stream[F, Socket[F]] =
    Stream.eval(Logger[F].info(s"Connecting to $address")) >>
    Stream.resource(socketGroup.client[F](address))

  /** Repeatedly reads chunks of bytes of specified size `maxChunkSize` from a socket
    * decoding them into trades.
    */
  private def repeatRead[F[_]: Concurrent](
    socket: Socket[F],
    maxChunkSize: Int
  )(implicit eventDecoder: Decoder[Trade]): Stream[F, Trade] =
    Stream
      .repeatEval(socket.read(maxBytes = maxChunkSize))
      .flatMap {
        case Some(value) => Stream.chunk(value)
        case None        => Stream.raiseError[F](new SocketException("Connection lost"))
      }
      .through(StreamDecoder.many(eventDecoder).toPipeByte[F])
}
