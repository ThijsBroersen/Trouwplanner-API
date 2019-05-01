package weddingplanner.endpoint

import cats.effect.IO
import io.finch.{Application, Bootstrap, Endpoint, Ok}
import lspace._
import lspace.codec.{jsonld, ActiveContext, NativeTypeEncoder}
import lspace.encode.{EncodeJson, EncodeJsonLD}
import lspace.services.LApplication
import lspace.services.rest.endpoints.LabeledNodeApi
import shapeless.{:+:, CNil}
import shapeless.tag.@@

object OntologyEndpoint {
  def apply(graph: Graph, ontology: Ontology, activeContext: ActiveContext = ActiveContext())(
      implicit baseDecoder: lspace.codec.NativeTypeDecoder,
      baseEncoder: lspace.codec.NativeTypeEncoder): OntologyEndpoint =
    new OntologyEndpoint(graph, ontology)(baseDecoder, baseEncoder, activeContext)
}
class OntologyEndpoint(graph: Graph, ontology: Ontology)(implicit baseDecoder: lspace.codec.NativeTypeDecoder,
                                                         baseEncoder: lspace.codec.NativeTypeEncoder,
                                                         activeContext: ActiveContext)
    extends Endpoint.Module[IO] {
  val service: LabeledNodeApi = LabeledNodeApi(graph, ontology, activeContext)

  val api = (service.label :: service.api)

  lazy val compiled: Endpoint.Compiled[IO] = {

    import lspace.services.codecs.Encode._
    implicit val encoder: jsonld.Encoder =
      jsonld.Encoder.apply(baseEncoder.asInstanceOf[NativeTypeEncoder.Aux[baseEncoder.Json]])

    import EncodeJson._
    import EncodeJsonLD._

    Bootstrap
      .configure(enableMethodNotAllowed = true, enableUnsupportedMediaType = true)
      .serve[Application.Json :+: LApplication.JsonLD :+: CNil](api)
      .compile
  }
}
