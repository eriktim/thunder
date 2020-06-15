package io.timmers.thunder

import cats.effect._
import zio._
import zio.logging.Logging
import io.timmers.thunder.Knmi

object Main extends App {
  override def run(args: List[String]) = appLogic.exitCode

  val layer = Logging.console((_, str) => str) >>> Knmi.live

  val appLogic = ZIO(()).provideCustomLayer(layer)
}
