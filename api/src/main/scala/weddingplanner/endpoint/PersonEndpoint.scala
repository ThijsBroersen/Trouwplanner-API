package weddingplanner.endpoint

import cats.effect.IO
import io.finch.Endpoint
import lspace.librarian.structure.Graph
import lspace.services.rest.endpoints.LabeledNodeApi
import weddingplanner.ns.Person

case class PersonEndpoint(graph: Graph) extends Endpoint.Module[IO] {
  val service = LabeledNodeApi(Person.ontology)(graph)
  //  val api = service.labeledApi
  val api = Person.ontology.label
    .getOrElse("en", throw new Exception("no label found")) :: service.api
}
