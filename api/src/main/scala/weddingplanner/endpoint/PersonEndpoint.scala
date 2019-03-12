package weddingplanner.endpoint

import cats.effect.IO
import io.finch.Endpoint
import lspace.ns.vocab.schema
import lspace.structure.Graph
import lspace.services.rest.endpoints.LabeledNodeApi

case class PersonEndpoint(graph: Graph)(implicit ndecoder: lspace.codec.NativeTypeDecoder) extends Endpoint.Module[IO] {
  val service = new LabeledNodeApi(schema.Person.ontology, weddingplanner.ns.agenda.property :: Nil)(graph, ndecoder)
  //  val api = service.labeledApi
  val api = service.label :: service.api
}
