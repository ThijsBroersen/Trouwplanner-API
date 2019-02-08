package weddingplanner.endpoint

import cats.effect.IO
import io.finch.Endpoint
import lspace.librarian.structure.Graph
import lspace.services.rest.endpoints.LabeledNodeApi
import weddingplanner.ns.Appointment

case class AppointmentEndpoint(graph: Graph) extends Endpoint.Module[IO] {
  val service = LabeledNodeApi(Appointment.ontology)(graph)
  //  val api = service.labeledApi
  val api = service.label :: service.api
}
