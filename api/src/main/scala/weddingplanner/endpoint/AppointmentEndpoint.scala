package weddingplanner.endpoint

import cats.effect.IO
import io.finch.Endpoint
import lspace.structure.Graph
import lspace.services.rest.endpoints.LabeledNodeApi
import weddingplanner.ns.Appointment

case class AppointmentEndpoint(graph: Graph)(implicit ndecoder: lspace.codec.NativeTypeDecoder)
    extends Endpoint.Module[IO] {
  val service =
    new LabeledNodeApi(
      Appointment.ontology,
      Appointment.keys.startDate.property :: Appointment.keys.endDate.property :: Appointment.keys.name.property :: Nil)(
      graph,
      ndecoder)
  //  val api = service.labeledApi
  val api = service.label :: service.api
}
