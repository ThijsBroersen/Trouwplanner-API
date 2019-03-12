package weddingplanner.endpoint

import java.time.Instant

import cats.effect.IO
import io.finch.{Endpoint, Ok}
import lspace._
import lspace.provider.mem.MemGraph
import lspace.structure.{ClassType, Graph}
import lspace.services.rest.endpoints.TraversalService
import monix.eval.Task
import monix.reactive.Observable
import shapeless.HList
import scribe._
import weddingplanner.ns.{agenda, Agenda, Appointment, Place}

import scala.util.{Failure, Success, Try}

case class PlannerEndpoint[Json](agendaGraph: Graph, personGraph: Graph, placeGraph: Graph, appointmentGraph: Graph)(
    implicit ndecoder: lspace.codec.NativeTypeDecoder.Aux[Json],
    nencoder: lspace.codec.NativeTypeEncoder.Aux[Json])
    extends Endpoint.Module[IO] {
  val agendaService      = TraversalService(agendaGraph)
  val personService      = TraversalService(personGraph)
  val placeService       = TraversalService(placeGraph)
  val appointmentService = TraversalService(appointmentGraph)

  implicit val ec = monix.execution.Scheduler.global

  import io.finch._
  import lspace.Implicits.AsyncGuide.guide

  implicit val dateTimeDecoder: DecodeEntity[Instant] =
    DecodeEntity.instance(s =>
      Try(Instant.parse(s)) match {
        case Success(instant) => Right(instant)
        case Failure(error)   => Left(error)
    })

  def allGraph = MemGraph("allgraph") ++ agendaGraph ++ personGraph ++ placeGraph ++ appointmentGraph

  val traverse: Endpoint[IO, String] = {
    get("planner" :: "place" :: "available" :: paramOption[Instant]("since") :: paramOption[Instant]("until")) {
      (since: Option[Instant], until: Option[Instant]) =>
        val placesAndAppointments = g.N
          .hasLabel(Agenda.ontology)
          .project(t => t, _.out(agenda).out(Agenda.keys.appointment).hasLabel(Appointment.ontology))
          .withGraph(allGraph)
          .toListF
          .map(_.groupBy(_._1)
            .mapValues(_.map(_._2)))
          .runToFuture
//        placesAndAppointments.filter {
//          case (place, appointments) =>
//            appointments.filter(_.out(Appointment.keys.s)
//        }

        Ok("")
    }

  }

  val api = "planner" :: traverse
}
