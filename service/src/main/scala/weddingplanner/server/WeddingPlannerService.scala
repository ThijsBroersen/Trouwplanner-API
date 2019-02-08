package weddingplanner.server

import cats.effect.IO
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.param.Stats
import com.twitter.server.TwitterServer
import io.finch.{Application, Bootstrap, Endpoint}
import lspace.encode.EncodeJsonLD
import lspace.librarian.structure._
import lspace.services.codecs.{Application => LApplication}
import lspace.ns._
import lspace.services.LService
import monix.eval.Task
import shapeless.{:+:, CNil, HNil}
import weddingplanner.endpoint.{AgendaEndpoint, AppointmentEndpoint, PersonEndpoint, PlaceEndpoint}

import scala.concurrent.Await

trait WeddingPlannerService extends LService {
  lazy val agendaGraph: Graph = ServicesConfig.config.agendaGraph.toGraph
  lazy val agendaService      = AgendaEndpoint(agendaGraph)

  lazy val appointmentGraph: Graph = ServicesConfig.config.appointmentGraph.toGraph
  lazy val appointmentService      = AppointmentEndpoint(appointmentGraph)

  lazy val personGraph: Graph = ServicesConfig.config.personGraph.toGraph
  lazy val personService      = PersonEndpoint(personGraph)

  lazy val placeGraph: Graph = ServicesConfig.config.placeGraph.toGraph
  lazy val placeService      = PlaceEndpoint(placeGraph)

  object utils extends Endpoint.Module[IO] {
    val persist: Endpoint[IO, Unit] = get("_persist") {
      scribe.info("persisting all graphs")
      agendaGraph.persist
      appointmentGraph.persist
      personGraph.persist
      placeGraph.persist
      io.finch.NoContent[Unit]
    }
  }

  val api = agendaService.api :+: appointmentService.api :+: personService.api :+: placeService.api :+: utils.persist

  implicit val encoder = lspace.codec.argonaut.Encoder
  import lspace.services.codecs.Encode._
  import lspace.encode.EncodeJson._
  import lspace.encode.EncodeJsonLD._

  lazy val service: Service[Request, Response] = Bootstrap
    .configure(enableMethodNotAllowed = true, enableUnsupportedMediaType = true)
    .serve[LApplication.JsonLD :+: Application.Json :+: CNil](api)
    .toService
}

object WeddingPlannerService extends WeddingPlannerService with TwitterServer {

  def main(): Unit = {
    val server = Http.server
      .configured(Stats(statsReceiver))
      .serve(
        s":${ServicesConfig.config.port.value}",
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
