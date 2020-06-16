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

  case class HourlyValues(
      stn: Int,
      yyyymmdd: String, // datum (YYYY=jaar,MM=maand,DD=dag);
      hh: Int, // tijd (HH=uur, UT.12 UT=13 MET, 14 MEZT. Uurvak 05 loopt van 04.00 UT tot 5.00 UT;
      dd: Int, // Windrichting (in graden) gemiddeld over de laatste 10 minuten van het afgelopen uur (360=noord, 90=oost, 180=zuid, 270=west, 0=windstil 990=veranderlijk. Zie http://www.knmi.nl/kennis-en-datacentrum/achtergrond/klimatologische-brochures-en-boeken;
      fh: Int, // Uurgemiddelde windsnelheid (in 0.1 m/s). Zie http://www.knmi.nl/kennis-en-datacentrum/achtergrond/klimatologische-brochures-en-boeken;
      ff: Int, // Windsnelheid (in 0.1 m/s) gemiddeld over de laatste 10 minuten van het afgelopen uur;
      fx: Int, // Hoogste windstoot (in 0.1 m/s) over het afgelopen uurvak;
      t: Int, // Temperatuur (in 0.1 graden Celsius) op 1.50 m hoogte tijdens de waarneming;
      t10n: Int, // Minimumtemperatuur (in 0.1 graden Celsius) op 10 cm hoogte in de afgelopen 6 uur;
      td: Int, // Dauwpuntstemperatuur (in 0.1 graden Celsius) op 1.50 m hoogte tijdens de waarneming;
      sq: Int, // Duur van de zonneschijn (in 0.1 uren) per uurvak, berekend uit globale straling  (-1 for <0.05 uur);
      q: Int, // Globale straling (in J/cm2) per uurvak;
      dr: Int, // Duur van de neerslag (in 0.1 uur) per uurvak;
      rh: Int, // Uursom van de neerslag (in 0.1 mm) (-1 voor <0.05 mm);
      p: Int, // Luchtdruk (in 0.1 hPa) herleid naar zeeniveau, tijdens de waarneming;
      vv: Int, // Horizontaal zicht tijdens de waarneming (0=minder dan 100m, 1=100-200m, 2=200-300m,..., 49=4900-5000m, 50=5-6km, 56=6-7km, 57=7-8km, ..., 79=29-30km, 80=30-35km, 81=35-40km,..., 89=meer dan 70km);
      n: Int, // Bewolking (bedekkingsgraad van de bovenlucht in achtsten), tijdens de waarneming (9=bovenlucht onzichtbaar);
      u: Int, // Relatieve vochtigheid (in procenten) op 1.50 m hoogte tijdens de waarneming;
      ww: Int, // Weercode (00-99), visueel(WW) of automatisch(WaWa) waargenomen, voor het actuele weer of het weer in het afgelopen uur. Zie http://bibliotheek.knmi.nl/scholierenpdf/weercodes_Nederland;
      ix: Int, // Weercode indicator voor de wijze van waarnemen op een bemand of automatisch station (1=bemand gebruikmakend van code uit visuele waarnemingen, 2,3=bemand en weggelaten (geen belangrijk weersverschijnsel, geen gegevens), 4=automatisch en opgenomen (gebruikmakend van code uit visuele waarnemingen), 5,6=automatisch en weggelaten (geen belangrijk weersverschijnsel, geen gegevens), 7=automatisch gebruikmakend van code uit automatische waarnemingen);
      m: Int, // Mist 0=niet voorgekomen, 1=wel voorgekomen in het voorgaande uur en/of tijdens de waarneming;
      r: Int, // Regen 0=niet voorgekomen, 1=wel voorgekomen in het voorgaande uur en/of tijdens de waarneming;
      s: Int, // Sneeuw 0=niet voorgekomen, 1=wel voorgekomen in het voorgaande uur en/of tijdens de waarneming;
      o: Int, // Onweer 0=niet voorgekomen, 1=wel voorgekomen in het voorgaande uur en/of tijdens de waarneming;
      y: Int // IJsvorming 0=niet voorgekomen, 1=wel voorgekomen in het voorgaande uur en/of tijdens de waarneming;
  )

  type KnmiService = Has[Service]

  def getData(
      stations: List[Int]
  ): ZIO[KnmiService, Throwable, List[HourlyValues]] =
    ZIO.accessM(_.get.getData(stations).map(parseCsv(_)))

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
          sttpClient
            .send(request)
            .map(_.body.getOrElse("TODO"))
        }
      }
    }

  val liveEnvironment: ZLayer[Any, Throwable, KnmiService] =
    AsyncHttpClientZioBackend.layer() >>> Knmi.live

  def parseCsv(csv: String): List[HourlyValues] =
    csv.linesIterator
      .filter(!_.startsWith("#"))
      .map(_.trim.split(", *"))
      .map { row =>
        HourlyValues(
          stn = row(0).toInt,
          yyyymmdd = row(1),
          hh = row(2).toInt,
          dd = row(3).toInt,
          fh = row(4).toInt,
          ff = row(5).toInt,
          fx = row(6).toInt,
          t = row(7).toInt,
          t10n = if (row(8) == "") -1 else row(8).toInt, // FIXME
          td = row(9).toInt,
          sq = row(10).toInt,
          q = row(11).toInt,
          dr = row(12).toInt,
          rh = row(13).toInt,
          p = row(14).toInt,
          vv = row(15).toInt,
          n = row(16).toInt,
          u = row(17).toInt,
          ww = if (row(18) == "") -1 else row(18).toInt, // FIXME
          ix = row(19).toInt,
          m = row(20).toInt,
          r = row(21).toInt,
          s = row(22).toInt,
          o = row(23).toInt,
          y = row(24).toInt
        )
      }
      .toList

}
