package weddingplanner.endpoint

import cats.effect.IO
import io.finch.Endpoint
import lspace.services.rest.endpoints.LabeledNodeApi
import lspace.structure.Graph
import weddingplanner.ns.WeddingReservation

case class WeddingReservationEndpoint(graph: Graph)(implicit ndecoder: lspace.codec.NativeTypeDecoder)
    extends Endpoint.Module[IO] {
  val service = new LabeledNodeApi(WeddingReservation.ontology)(graph, ndecoder)

  val api = service.label :: service.api
}
