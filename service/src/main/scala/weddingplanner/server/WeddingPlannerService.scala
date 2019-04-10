package weddingplanner.server

import java.time.{Instant, LocalDate}

import cats.effect.IO
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.param.Stats
import com.twitter.server.TwitterServer
import io.finch.{Application, Bootstrap, Endpoint}
import lspace.codec.{ActiveContext, JsonObjectInProgress}
import lspace.encode.EncodeJsonLD
import lspace.structure._
import lspace.services.codecs.{Application => LApplication}
import lspace._
import lspace.ns._
import lspace.ns.vocab.schema
import lspace.services.LService
import lspace.types.vector.Point
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.schedulers.CanBlock
import shapeless.{:+:, CNil, HNil}
import weddingplanner.endpoint._

import scala.collection.mutable
import scala.concurrent.Await

trait WeddingPlannerService extends LService {
  import lspace.codec.argonaut._
  implicit val ec: Scheduler = lspace.Implicits.Scheduler.global

  lazy val graph: Graph              = ServicesConfig.config.graph.toGraph
  lazy val agendaService             = AgendaEndpoint(graph)
  lazy val appointmentService        = AppointmentEndpoint(graph)
  lazy val personService             = PersonEndpoint(graph)
  lazy val placeService              = PlaceEndpoint(graph)
  lazy val weddingReservationService = WeddingReservationEndpoint(graph)
  lazy val reportToMarriageService   = ReportToMarriageEndpoint(graph)

  object utils extends Endpoint.Module[IO] {
    val persist: Endpoint[IO, Unit] = get("_persist") {
      scribe.info("persisting all graphs")
      graph.persist
      io.finch.NoContent[Unit]
    }
  }

  implicit val encoder: lspace.codec.Encoder = lspace.codec.Encoder(nativeEncoder)
  import lspace.services.codecs.Encode._
  //  import lspace.encode.EncodeJson._
  import EncodeJson._
  import lspace.encode.EncodeJsonLD._
  import weddingplanner.ns.Appointment
  import weddingplanner.ns.Agenda

  object LabelApi extends Endpoint.Module[IO] {
    import io.finch._
    val cache = mutable.HashMap[String, mutable.HashMap[String, String]]()

    def getResource(ontology: Ontology): Endpoint[IO, String] =
      get(path(ontology.iri.reverse.takeWhile(_ != '/').reverse.toLowerCase() + ".jsonld")) {
        cache
          .get(ontology.iri)
          .flatMap(_.get("application/ld+json"))
          .orElse(Some(new JsonObjectInProgress.WithJsonObjectInProgress(
            encoder.fromOntology(ontology)(ActiveContext()))(nativeEncoder
            .asInstanceOf[lspace.codec.NativeTypeEncoder.Aux[WeddingPlannerService.this.encoder.Json]]).withContext match {
            case json =>
              cache += (ontology.iri)                                                                -> (cache
                .getOrElse(ontology.iri, mutable.HashMap[String, String]()) += "application/ld+json" -> json.toString)
              json.toString
          }))
          .map(Ok)
          .map(_.withHeader("Content-Type", "application/ld+json"))
          .getOrElse(io.finch.NotFound(new Exception("unknown path")))
      }

    val resetGraphs: Endpoint[IO, String] = get(path("reset")) {
      SampleData.loadSample(graph).forkAndForget.runToFuture(monix.execution.Scheduler.global)
      Ok("resetting now, building graphs...")
    }

    val clearGraphs: Endpoint[IO, String] = get(path("clear")) {
      graph.purge
        .onErrorHandle { f =>
          println(f.getMessage); throw f
        }
        .forkAndForget
        .runToFuture(monix.execution.Scheduler.global)
      Ok("clearing now")
    }

    val sigmajs: Endpoint[IO, String] = get(path("sigmajs")).mapOutputAsync { hn =>
      (for {
        data <- SigmaJsVisualizer.visualizeGraph(graph)
      } yield Ok(data)).toIO
    }

  }

  val labels = LabelApi.getResource(agendaService.service.ontology) :+: LabelApi.getResource(
    appointmentService.service.ontology) :+: LabelApi.getResource(personService.service.ontology) :+: LabelApi
    .getResource(placeService.service.ontology) :+: LabelApi.getResource(weddingReservationService.service.ontology) :+: LabelApi
    .getResource(reportToMarriageService.service.ontology)

  val api = labels :+: agendaService.api :+: appointmentService.api :+: personService.api :+: placeService.api :+:
    reportToMarriageService.api :+: weddingReservationService.api :+: utils.persist :+:
    LabelApi.clearGraphs :+: LabelApi.resetGraphs :+: LabelApi.sigmajs

  SampleData.loadSample(graph).runSyncUnsafe()(monix.execution.Scheduler.global, CanBlock.permit)
//  println(SigmaJsVisualizer.visualizeGraph(graph))

  lazy val service: Service[Request, Response] = Bootstrap
    .configure(enableMethodNotAllowed = true, enableUnsupportedMediaType = true)
    .serve[LApplication.JsonLD :+: Application.Json :+: CNil](api)
    .toService
}

object WeddingPlannerService extends WeddingPlannerService with TwitterServer {

  def main(): Unit = {
    val server = Http.server
//      .configured(Stats(statsReceiver))
      .serve(
        s":${ServicesConfig.config.port.value}",
        service
      )

    import scala.concurrent.duration._
    onExit {
      println(s"close wedding-planner-server")
      Await.ready(
        Task
          .sequence(
            Seq(
              Task.gatherUnordered(Seq(
                graph.persist
              )),
              Task.gatherUnordered(Seq(
                graph.close
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
