package com.github.oskin1.proxy

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import fs2.io.tcp.SocketGroup
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import configReaders._

object App extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    program.as(ExitCode.Success)

  private def program: IO[Unit] =
    loadSettings.flatMap { settings =>
      Slf4jLogger.create[IO].flatMap { implicit logger =>
        Blocker[IO].use { blocker =>
          SocketGroup[IO](blocker).use { socketGroup =>
            TradesPersistence
              .empty[IO](settings.epochLength.toMillis, settings.keepLastEpochs)
              .flatMap { ref =>
                TradesListener
                  .run(ref, socketGroup, settings)
                  .concurrently(Server.run(ref, socketGroup, settings))
                  .compile
                  .drain
              }
          }
        }
      }
    }

  private def loadSettings: IO[Settings] =
    IO.delay(ConfigSource.default.load[Settings]).flatMap {
      _.fold[IO[Settings]](
        fs =>
          IO.raiseError(
            new RuntimeException(
              s"Failed to read config:\n" +
              s"${(fs.head +: fs.tail).map(_.description).mkString(";\n")}"
            )
        ),
        _.pure[IO]
      )
    }
}
