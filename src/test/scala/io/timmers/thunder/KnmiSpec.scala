package io.timmers.thunder

import zio._
import zio.console._
import zio.test._
import zio.test.Assertion._
import zio.test.environment._
import io.timmers.thunder.Knmi.HourlyValues

import scala.io.Source

object KnmiSpec extends DefaultRunnableSpec {
  val testData: Managed[Nothing, String] =
    ZManaged
      .make {
        ZIO.effectTotal(Source.fromResource("test.csv"))
      } { source => ZIO.effectTotal(source.close) }
      .mapM { source =>
        ZIO.effectTotal {
          source.mkString
        }
      }

  val testKnmi: ZLayer[Has[String], Nothing, Knmi.KnmiService] =
    ZLayer.fromService { testData =>
      new Knmi.Service {
        def getData(stations: List[Int]): zio.IO[Throwable, String] =
          UIO(testData)
      }
    }

  val testEnvironment =
    testData.toLayer >>> testKnmi

  def spec = suite("KnmiSpec")(
    testM("TODO") {
      val actual = Knmi.getData(List())
      val expected = HourlyValues(
        370,
        "20200601",
        12,
        50,
        40,
        50,
        80,
        244,
        176,
        65,
        10,
        325,
        0,
        0,
        10216,
        81,
        1,
        31,
        -1,
        5,
        0,
        0,
        0,
        0,
        0
      )
      // TODO assertM(actual.map(_.size))(equalTo(24))
      assertM(actual)(contains(expected))
    }.provideCustomLayerShared(testEnvironment)
  )
}
