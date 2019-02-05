package weddingplanner.server

import argonaut.{EncodeJson, Json, PrettyParams}
import cats.effect.IO
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.param.Stats
import com.twitter.io.Buf
import com.twitter.server.TwitterServer
import io.finch
import io.finch.{Bootstrap, Endpoint, Ok, Output}
import lspace.encode.EncodeJsonLD
import lspace.librarian.provider.mem.MemGraph
import lspace.librarian.structure._
import lspace.parse.{ActiveContext, JsonLD}
import lspace.ns._
import lspace.services.LService
import lspace.services.rest.endpoints.{JsonLDModule, PagedResult}
import monix.eval.Task
import shapeless.{:+:, CNil}
import weddingplanner.endpoint.{AgendaEndpoint, AppointmentEndpoint, PersonEndpoint, PlaceEndpoint}

import scala.concurrent.Await

trait WeddingPlannerService extends LService {
  lazy val agendaGraph: Graph = MemGraph("https://demo.vng.nl/weddingplanner/agenda")
  lazy val agendaService      = AgendaEndpoint(agendaGraph)

  lazy val appointmentGraph: Graph = MemGraph("https://demo.vng.nl/weddingplanner/appointment")
  lazy val appointmentService      = AppointmentEndpoint(appointmentGraph)

  lazy val personGraph: Graph = MemGraph("https://demo.vng.nl/weddingplanner/person")
  lazy val personService      = PersonEndpoint(personGraph)

  lazy val placeGraph: Graph = MemGraph("https://demo.vng.nl/weddingplanner/place")
  lazy val placeService      = PlaceEndpoint(placeGraph)

  val api = agendaService.api :+: appointmentService.api :+: personService.api :+: placeService.api

  import JsonLDModule.Encode._
  import EncodeJsonLD._
  import lspace.encode.EncodeJsonLD._

  implicit def pagedResultToJsonLD =
    new EncodeJsonLD[PagedResult] {
      val encode: PagedResult => Json = {
        case pr: PagedResult =>
          Json.jObject(
            JsonLD.detached.encode
              .fromAny(pr.result)(ActiveContext())
              .withContext)
      }
    }

  lazy val service: Service[Request, Response] = Bootstrap
    .configure(enableMethodNotAllowed = true, enableUnsupportedMediaType = true)
    .serve[JsonLDModule.JsonLD :+: CNil](api)
    .toService
}
object WeddingPlannerService extends WeddingPlannerService with TwitterServer {

  lazy val port: Int = 8080 //TODO: from Config

  def main(): Unit = {
    val server = Http.server
//      .configured(Stats(statsReceiver))
      .serve(
        s":$port",
        service
      )

    import scala.concurrent.duration._
    onExit {
      println(s"close wedding-planner-server")
      Await.ready(
        Task
          .sequence(Seq(
            Task.gatherUnordered(Seq(
              Task.fromFuture(agendaGraph.persist),
              Task.fromFuture(appointmentGraph.persist),
              Task.fromFuture(personGraph.persist),
              Task.fromFuture(placeGraph.persist),
            )),
            Task.gatherUnordered(
              Seq(
                Task.fromFuture(agendaGraph.close),
                Task.fromFuture(appointmentGraph.close),
                Task.fromFuture(personGraph.close),
                Task.fromFuture(placeGraph.close),
              ))
          ))
          .runToFuture(monix.execution.Scheduler.global),
        20 seconds
      )

      server.close()
    }

    com.twitter.util.Await.ready(adminHttpServer)
  }
}
