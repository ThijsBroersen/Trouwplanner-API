package weddingplanner.endpoint

import cats.effect.IO
import io.finch.Endpoint
import lspace.librarian.structure.Graph
import lspace.services.rest.endpoints.LabeledNodeApi
import weddingplanner.ns.Place

case class PlaceEndpoint(graph: Graph) extends Endpoint.Module[IO] {
  val service = LabeledNodeApi(Place.ontology)(graph)
//  val api = service.labeledApi
  val api = Place.ontology.label
    .getOrElse("en", throw new Exception("no label found")) :: service.api
}
