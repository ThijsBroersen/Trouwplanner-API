package weddingplanner.endpoint

import cats.effect.IO
import io.finch.{Application, Bootstrap, Endpoint, Text}
import lspace.{Graph, Ontology}
import lspace.codec.{jsonld, ActiveContext, NativeTypeEncoder}
import lspace.encode.{EncodeJson, EncodeJsonLD, EncodeText}
import lspace.services.LApplication
import lspace.services.rest.endpoints.LabeledNodeApi
import shapeless.{:+:, CNil}
import weddingplanner.ns.ReportOfMarriage

object ReportToMarriageEndpoint {
  def apply(graph: Graph, activeContext: ActiveContext = ActiveContext())(
      implicit baseDecoder: lspace.codec.NativeTypeDecoder,
      baseEncoder: lspace.codec.NativeTypeEncoder): ReportToMarriageEndpoint =
    new ReportToMarriageEndpoint(graph)(baseDecoder, baseEncoder, activeContext)
}

class ReportToMarriageEndpoint(graph: Graph)(implicit baseDecoder: lspace.codec.NativeTypeDecoder,
                                             baseEncoder: lspace.codec.NativeTypeEncoder,
                                             activeContext: ActiveContext)
    extends Endpoint.Module[IO] {
  val service: LabeledNodeApi = LabeledNodeApi(graph, ReportOfMarriage.ontology, activeContext)

  val api = service.label :: service.api

  lazy val compiled: Endpoint.Compiled[IO] = {
    type Json = baseEncoder.Json
    implicit val be: NativeTypeEncoder.Aux[Json] = baseEncoder.asInstanceOf[NativeTypeEncoder.Aux[Json]]

    import lspace.services.codecs.Encode._
    implicit val encoder = jsonld.Encoder.apply(be)

    import EncodeJson._
    import EncodeJsonLD._

    Bootstrap
      .configure(enableMethodNotAllowed = true, enableUnsupportedMediaType = true)
      .serve[Application.Json :+: LApplication.JsonLD :+: CNil](api)
      .compile
  }
}
