package weddingplanner.endpoint

import cats.effect.IO
import io.finch.Endpoint
import lspace.librarian.structure.Graph
import lspace.services.rest.endpoints.LabeledNodeApi
import weddingplanner.ns.Agenda

case class AgendaEndpoint(graph: Graph) extends Endpoint.Module[IO] {
  val service = LabeledNodeApi(Agenda.ontology)(graph)
  val api     = service.label :: service.api
}
