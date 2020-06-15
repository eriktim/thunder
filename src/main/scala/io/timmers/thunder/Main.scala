package io.timmers.thunder

import zio._
import zio.console._
import zio.logging.Logging
import io.timmers.thunder.Knmi

object Main extends App {
  override def run(args: List[String]) =
    appLogic.provideCustomLayer(layer).exitCode

  val layer = Logging.console((_, str) => str) >>> Knmi.live

  val appLogic = for {
    service <- ZIO.access[Has[Knmi.Service]](_.get)
    result <- service.getData(List())
    _ <- putStrLn(s"Got $result")
  } yield result
}
