package weddingplanner.endpoint

import cats.effect.IO
import io.finch.Endpoint
import lspace.librarian.structure.Graph
import lspace.services.rest.endpoints.LabeledNodeApi
import weddingplanner.ns.Person

case class PersonEndpoint(graph: Graph)(implicit ndecoder: lspace.codec.NativeTypeDecoder) extends Endpoint.Module[IO] {
  val service = LabeledNodeApi(Person.ontology)(graph, ndecoder)
  //  val api = service.labeledApi
  val api = service.label :: service.api
}
