package weddingplanner.endpoint

import java.time.Instant

import cats.effect.IO
import io.finch.{Endpoint, Ok}
import lspace.librarian.process.traversal.{Collection, Traversal}
import lspace.librarian.provider.mem.MemGraph
import lspace.librarian.structure.{ClassType, Graph}
import lspace.services.rest.endpoints.TraversalService
import monix.eval.Task
import shapeless.HList
import scribe._
import weddingplanner.ns.{Agenda, Appointment, Place}

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

  implicit val dateTimeDecoder: DecodeEntity[Instant] =
    DecodeEntity.instance(s =>
      Try(Instant.parse(s)) match {
        case Success(instant) => Right(instant)
        case Failure(error)   => Left(error)
    })

  val traverse: Endpoint[IO, String] = {
    get("planner" :: "place" :: "available" :: paramOption[Instant]("since") :: paramOption[Instant]("until")) {
      (since: Option[Instant], until: Option[Instant]) =>
//        val allGraph = MemGraph("allgraph") ++ agendaGraph ++ personGraph ++ placeGraph ++ appointmentGraph
        val allGraph = MemGraph("allgraph")
        allGraph ++ agendaGraph
        allGraph ++ personGraph
        allGraph ++ placeGraph
        allGraph ++ appointmentGraph

        val placesAndAppointments = allGraph.g.N
          .project(_.hasLabel(Place.ontology),
                   _.out(Place.keys.agenda).out(Agenda.keys.appointment).hasLabel(Appointment.ontology))
          .toList
          .groupBy(_._1)
          .mapValues(_.map(_._2))
//        placesAndAppointments.filter {
//          case (place, appointments) =>
//            appointments.filter(_.out(Appointment.keys.s)
//        }

        Ok("")
    }
  }

//  val api = service.label :: service.api
}
