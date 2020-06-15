package io.timmers.thunder

import sttp.client._
import sttp.client.circe._
import sttp.client.asynchttpclient.zio._
import sttp.client.asynchttpclient.WebSocketHandler
import zio._
import zio.logging._

object Knmi {
  val resourceUri =
    uri"http://projects.knmi.nl/klimatologie/uurgegevens/getdata_uur.cgi"

  trait Service {
    def getData(stations: List[Int]): IO[Throwable, String]
  }

  type KnmiService = Has[Service]

  val live: ZLayer[SttpClient, Nothing, KnmiService] =
    ZLayer.fromService { sttpClient =>
      new Service {
        def getData(stations: List[Int]): Task[String] = {
          val day = "20200615"
          val stns = stations.map(_.toString()).mkString(":")
          val body =
            Map("start" -> s"${day}01", "end" -> s"${day}24", "stns" -> stns)
          val request = basicRequest
            .post(resourceUri)
            .body(body)
          sttpClient.send(request).map(_.body.getOrElse("TODO"))
        }
      }
    }

  val liveEnvironment: ZLayer[Any, Throwable, KnmiService] =
    AsyncHttpClientZioBackend.layer() >>> Knmi.live
}
