package io.timmers.thunder

import org.http4s._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.blaze._
import org.http4s.implicits._
import zio._
import zio.interop.catz._
import zio.logging._

object Knmi {
  trait Service {
    def getData(stations: List[Int]): IO[Throwable, String]
  }

  val live = {
    val managedClient = ZIO
      .runtime[Any]
      .map { implicit rt =>
        val exec = rt.platform.executor.asEC
        catsIOResourceSyntax(
          BlazeClientBuilder[Task](exec).resource
        ).toManaged
      }
      .toManaged_
      .flatten

    ZLayer.fromManaged {
      for {
        logger <- ZIO.access[Logging](_.get).toManaged_
        client <- managedClient
      } yield new Knmi(client, logger).service
    }
  }
}

final class Knmi(val client: Client[Task], val logger: Logger[_]) {
  import Knmi._

  val service = new Knmi.Service() {
    override def getData(
        stations: List[Int]
    ): IO[Throwable, String] = {
      IO("big ass csv")
      //   val path = s"/repos/$fullName/contributors"
      //   val request =
      //     Request[Task](
      //       Method.GET,
      //       baseUrl.addPath(path),
      //       headers = headers(token)
      //     )
      //   client
      //     .expect[List[Contributor]](request)
      //     .flatMapError {
      //       case UnexpectedStatus(status) =>
      //         logger
      //           .error(s"$path returned $status")
      //           .map(_ => GithubApiRequestFailed)
      //       case error =>
      //         logger
      //           .error(s"$path errored: ${error.getMessage()}")
      //           .map(_ => GithubApiRequestFailed)
      //     }
    }
  }
}
