package com.github.oskin1.proxy

import java.net.InetSocketAddress

import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, ContextShift, Resource, Timer}
import io.chrisdavenport.log4cats.Logger
import fs2.io.tcp.{Socket, SocketGroup}
import fs2._
import scodec.Encoder
import scodec.stream.StreamEncoder
import commonCodecs._

import scala.concurrent.duration._

/** Serves clients sending them latest events in aggregated form
  * (candlestick chart) with some interval.
  */
object Server {

  private val connectionTimeout = 10.seconds

  def run[F[_]: Concurrent: ContextShift: Timer: Logger](
    tradesPersistenceRef: Ref[F, TradesPersistence],
    socketGroup: SocketGroup,
    settings: Settings
  ): Stream[F, Unit] =
    startServer(socketGroup, settings.serverAddress).map {
      Stream
        .resource(_)
        .flatMap(socket => handleClient(tradesPersistenceRef, socket, settings))
    }.parJoinUnbounded

  /** Binds to the specified address and provides a connection for,
    * represented as a [[Socket]], for each client that connects to the bound address.
    */
  private def startServer[F[_]: Concurrent: ContextShift: Logger](
    socketGroup: SocketGroup,
    address: InetSocketAddress
  ): Stream[F, Resource[F, Socket[F]]] =
    Stream.eval(Logger[F].info(s"Starting server on $address")) >>
    socketGroup.server[F](address)

  /** Handles incoming connections from clients sending them latest events.
    */
  private def handleClient[F[_]: Concurrent: Timer: Logger](
    tradesPersistenceRef: Ref[F, TradesPersistence],
    socket: Socket[F],
    settings: Settings
  ): Stream[F, Unit] =
    Stream.eval(Logger[F].info(s"New incoming connection.")) >>
    Stream.eval(tradesPersistenceRef.get).flatMap { persistence =>
      writeToSocket(socket, persistence.getAggregated(settings.keepLastEpochs).toList)
    } >>
    Stream(()).repeat
      .covary[F]
      .metered(settings.epochLength)
      .flatMap { _ =>
        Stream.eval(tradesPersistenceRef.get).flatMap { persistence =>
          writeToSocket(socket, persistence.getAggregated(1).toList)
        }
      }
      .handleErrorWith(
        e =>
          Stream.eval(Logger[F].info(s"Connection lost due to: ${e.getMessage}")) >>
          Stream.eval(socket.close)
      )

  private def writeToSocket[F[_]: Concurrent, A](
    socket: Socket[F],
    msg: A
  )(implicit msgEncoder: Encoder[A]): Stream[F, Unit] =
    Stream
      .emit(msg)
      .through(StreamEncoder.many(msgEncoder).toPipe)
      .flatMap(bits => Stream.chunk(Chunk.byteVector(bits.bytes)))
      .through(socket.writes(Some(connectionTimeout)))
}
