package weddingplanner.endpoint

import cats.effect.IO
import io.finch.{Endpoint, Ok}
import lspace.structure.Graph
import lspace.services.rest.endpoints.LabeledNodeApi
import weddingplanner.ns.{Agenda, ReportOfMarriage, WeddingReservation}

import scala.collection.mutable

case class AgendaEndpoint(graph: Graph)(implicit ndecoder: lspace.codec.NativeTypeDecoder) extends Endpoint.Module[IO] {
  val service = new LabeledNodeApi(Agenda.ontology)(graph, ndecoder)

  val api = service.label :: service.api
}
