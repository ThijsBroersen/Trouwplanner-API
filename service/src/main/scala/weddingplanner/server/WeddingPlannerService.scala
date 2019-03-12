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
import shapeless.{:+:, CNil, HNil}
import weddingplanner.endpoint._

import scala.collection.mutable
import scala.concurrent.Await

trait WeddingPlannerService extends LService {
  import lspace.codec.argonaut._

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

    def sampleGraphs: Endpoint[IO, String] = get(path("reset")) {
      Task {
        val places = new {
          val SanJosédeMaipo = new {
            val place       = graph + schema.Place
            val id          = place --- Property.default.`@id` --> (graph.iri + "/place/123")
            val name        = place --- schema.name --> "San José de Maipo"
            val description = place --- "name" --> "San José de Maipo"
            val geo         = place --- schema.geo --> Point(72.0403, 60.90879)
          }
          val CrystalSprings = new {
            val place = graph + schema.Place
            place --- Property.default.`@id` --> (graph.iri + "/place/12345")
            val name = place --- schema.name --> "Crystal Springs"
            val geo  = place --- schema.geo --> Point(-48.4046, 175.87173)
          }
          val Haridwar = new {
            val place = graph + schema.Place
            val id    = place --- Property.default.`@id` --> (graph.iri + "/place/345")
            val name  = place --- schema.name --> "Haridwar"
            val geo   = place --- schema.geo --> Point(89.45136, 88.01204)
          }
          val Talca = new {
            val place = graph + schema.Place
            val id    = place --- Property.default.`@id` --> (graph.iri + "/place/34567")
            val name  = place --- schema.name --> List("Talca", "Tal Ca")
            val geo   = place --- schema.geo --> Point(74.32746, -45.06438)
          }
        }

        val addresses = new {
          val gemeenteHoorn = new {
            val address       = graph + schema.PostalAddress
            val id            = address --- Label.P.`@id` --> (graph.iri + "/address/1")
            val postalCode    = address --- schema.postalCode --> "1625HV"
            val streetAddress = address --- schema.streetAddress --> "1"
          }
          val gemeenteHoorn2 = new {
            val address       = graph + schema.PostalAddress
            val id            = address --- Label.P.`@id` --> (graph.iri + "/address/2")
            val postalCode    = address --- schema.postalCode --> "1625HV"
            val streetAddress = address --- schema.streetAddress --> "2"
          }
          val gemeenteHaarlem = new {
            val address       = graph + schema.PostalAddress
            val id            = address --- Label.P.`@id` --> (graph.iri + "/address/3")
            val postalCode    = address --- schema.postalCode --> "2011VB"
            val streetAddress = address --- schema.streetAddress --> "39"
          }
          val gemeenteHeerenveen = new {
            val address       = graph + schema.PostalAddress
            val id            = address --- Label.P.`@id` --> (graph.iri + "/address/4")
            val postalCode    = address --- schema.postalCode --> "8441ES"
            val streetAddress = address --- schema.streetAddress --> "2"
          }
        }

        val persons = new {
          val Yoshio = new {
            val person     = graph + schema.Person
            val id         = person --- Property.default.`@id` --> (graph.iri + "/person/123")
            val name       = person --- schema.name --> "Yoshio" //relation can be a string
            val birthdate  = person --- schema.birthDate --> LocalDate.parse("1996-08-18")
            val birthPlace = person --- schema.birthPlace --> places.CrystalSprings.place
            val address    = person --- schema.address --> addresses.gemeenteHoorn.address
          }
          val Levi = new {
            val person     = graph + schema.Person
            val id         = person --- Property.default.`@id` --> (graph.iri + "/person/12345")
            val name       = person --- schema.name --> "Levi" //relation can be a Property-object
            val birthdate  = person --- schema.birthDate --> LocalDate.parse("2008-12-20")
            val birthPlace = person --- schema.birthPlace --> places.CrystalSprings.place
            val address    = person --- schema.address --> addresses.gemeenteHoorn2.address
          }
          val Gray = new {
            val person     = graph + schema.Person
            val id         = person --- Property.default.`@id` --> (graph.iri + "/person/345")
            val name       = person --- schema.name --> "Gray"
            val birthdate  = person --- schema.birthDate --> LocalDate.parse("1997-04-10")
            val birthPlace = person --- schema.birthPlace --> places.Haridwar.place
            val address    = person --- schema.address --> addresses.gemeenteHeerenveen.address
          }
          val Kevin = new {
            val person     = graph + schema.Person
            val id         = person --- Property.default.`@id` --> (graph.iri + "/person/34567")
            val name       = person --- schema.name --> "Kevin"
            val birthdate  = person --- schema.birthDate --> LocalDate.parse("2008-11-30")
            val birthPlace = person --- schema.birthPlace --> places.SanJosédeMaipo.place
            val address    = person --- schema.address --> addresses.gemeenteHaarlem.address
          }
          val Stan = new {
            val person     = graph + schema.Person
            val id         = person --- Property.default.`@id` --> (graph.iri + "/person/567")
            val name       = person --- schema.name --> "Stan"
            val birthdate  = person --- schema.birthDate --> LocalDate.parse("2002-06-13")
            val birthPlace = person --- schema.birthPlace --> places.SanJosédeMaipo.place
            val address    = person --- schema.address --> addresses.gemeenteHaarlem.address
          }
          val Garrison = new {
            val person     = graph + schema.Person
            val id         = person --- Property.default.`@id` --> (graph.iri + "/person/56789")
            val name       = person --- schema.name --> "Garrison"
            val birthdate  = person --- schema.birthDate --> LocalDate.parse("1994-06-18")
            val birthPlace = person --- schema.birthPlace --> places.Talca.place
          }

        }

        val agendas = new {
          import weddingplanner.ns.Agenda
          val a1 = new {
            val agenda = graph + Agenda
            val id     = agenda --- Label.P.`@id` --> (graph.iri + "/agenda/1")
            val name   = agenda --- schema.name --> "Agenda Yoshio"
          }
          val a2 = new {
            val agenda = graph + Agenda
            val id     = agenda --- Label.P.`@id` --> (graph.iri + "/agenda/2")
            val name   = agenda --- schema.name --> "Agenda Garrison"
          }
          val a3 = new {
            val agenda = graph + Agenda
            val id     = agenda --- Label.P.`@id` --> (graph.iri + "/agenda/3")
            val name   = agenda --- schema.name --> "Agenda Levi"
          }
        }
        val appointments = new {
          import weddingplanner.ns.Appointment
          val ap1 = new {
            val appointment = graph + Appointment
            val id          = appointment --- Label.P.`@id` --> (graph.iri + "/appointment/1")
            val agenda      = appointment --- schema.isPartOf --> agendas.a1.agenda
            val startDate   = appointment --- Appointment.keys.startDate --> Instant.parse("2019-04-03T10:15:00.00Z")
          }
          val ap2 = new {
            val appointment = graph + Appointment
            val id          = appointment --- Label.P.`@id` --> (graph.iri + "/appointment/1")
            val agenda      = appointment --- schema.isPartOf --> agendas.a2.agenda
            val startDate   = appointment --- Appointment.keys.startDate --> Instant.parse("2019-05-15T14:00:00.00Z")
          }
        }

        persons.Garrison.person --- schema.owns --> agendas.a1.agenda
        persons.Yoshio.person --- schema.owns --> agendas.a1.agenda
        persons.Levi.person --- schema.owns --> agendas.a1.agenda

//          val GarrissonKnownStan  = Garrison.person --- schema.knows --- Stan.person
//          val GarrissonKnownKevin = Garrison.person --- schema.knows --- Kevin.person
//          val KevinKnownStan      = Kevin.person --- schema.knows --- Stan.person
//          val KevinKnownGray      = Kevin.person --- schema.knows --- Gray.person
//          val GrayKnowsLevi       = Gray.person --- schema.knows --- Levi.person
//          val LeviKnowsYoshio     = Levi.person --- schema.knows --- Yoshio.person

      }.forkAndForget.runToFuture(monix.execution.Scheduler.global)
      Ok("resetting now, building graphs...")
    }

    def clearGraphs: Endpoint[IO, String] = get(path("clear")) {
      Task {}.forkAndForget.runToFuture(monix.execution.Scheduler.global)
      Ok("clearing now")
    }
  }

  val labels = LabelApi.getResource(agendaService.service.ontology) :+: LabelApi.getResource(
    appointmentService.service.ontology) :+: LabelApi.getResource(personService.service.ontology) :+: LabelApi
    .getResource(placeService.service.ontology) :+: LabelApi.getResource(weddingReservationService.service.ontology) :+: LabelApi
    .getResource(reportToMarriageService.service.ontology)

  val api = labels :+: agendaService.api :+: appointmentService.api :+: personService.api :+: placeService.api :+: utils.persist

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
                Task.fromFuture(graph.persist)
              )),
              Task.gatherUnordered(Seq(
                Task.fromFuture(graph.close)
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
