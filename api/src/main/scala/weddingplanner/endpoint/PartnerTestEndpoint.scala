package weddingplanner.endpoint

import java.net.URLEncoder
import java.time.Instant

import cats.effect.IO
import io.finch.Endpoint
import lspace._
import lspace.codec.{jsonld, ActiveContext, ActiveProperty, NativeTypeDecoder, NativeTypeEncoder}
import lspace.decode.{DecodeJson, DecodeJsonLD}
import lspace.encode.{EncodeJson, EncodeJsonLD, EncodeText}
import lspace.ns.vocab.schema
import lspace.provider.detached.DetachedGraph
import lspace.services.LApplication
import monix.eval.Task
import shapeless.{:+:, CNil, HNil}
import weddingplanner.ns.PartnerTest

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success, Try}

object PartnerTestEndpoint {
  def apply(graph: Graph, activeContext: ActiveContext = ActiveContext())(
      implicit baseDecoder: lspace.codec.NativeTypeDecoder,
      baseEncoder: lspace.codec.NativeTypeEncoder): PartnerTestEndpoint =
    new PartnerTestEndpoint(graph)(baseDecoder, baseEncoder, activeContext)

  lazy val activeContext = ActiveContext(
    `@prefix` = ListMap(
      "person" -> PartnerTest.keys.person.iri
    ),
    definitions = Map(
      PartnerTest.keys.person.iri -> ActiveProperty(`@type` = schema.Person :: Nil, property = PartnerTest.keys.person)
    )
  )
}

class PartnerTestEndpoint(graph: Graph)(implicit val baseDecoder: lspace.codec.NativeTypeDecoder,
                                        val baseEncoder: lspace.codec.NativeTypeEncoder,
                                        activeContext: ActiveContext)
    extends Endpoint.Module[IO] {

  implicit val ec = monix.execution.Scheduler.global

  type Json = baseDecoder.Json
  implicit val bd: NativeTypeDecoder.Aux[Json] = baseDecoder.asInstanceOf[NativeTypeDecoder.Aux[Json]]

  import lspace.services.codecs.Decode._
  import DecodeJson._
  import DecodeJsonLD._
  implicit val decoder = lspace.codec.jsonld.Decoder(DetachedGraph)

  import io.finch._
  import lspace.Implicits.AsyncGuide.guide

  implicit val dateTimeDecoder: DecodeEntity[Instant] =
    DecodeEntity.instance(s =>
      Try(Instant.parse(s)) match {
        case Success(instant) => Right(instant)
        case Failure(error)   => Left(error)
    })

  implicit class WithGraphTask(graphTask: Task[Graph]) {
    def ++(graph: Graph) =
      for {
        graph0 <- graphTask
        graph1 <- graph0 ++ graph
      } yield graph1
  }

  import lspace.decode.DecodeJsonLD._

  /**
    * tests if a kinsman path exists between two
    * TODO: update graph with latest (remote) data
    */
  val partner: Endpoint[IO, Boolean :+: Boolean :+: CNil] = {
    import shapeless.::
    import io.finch.internal.HttpContent
    implicit val bodyJsonldTyped = DecodeJsonLD
      .bodyJsonldTyped(PartnerTest.ontology, PartnerTest.fromNode)

    implicit val jsonToNodeToT = DecodeJson
      .jsonToNodeToT(PartnerTest.ontology, PartnerTest.fromNode)

    get(params[String]("id"))
      .mapOutputAsync {
        case Nil =>
          Task.now(NotAcceptable(new Exception("a partnertest must have persons as input"))).toIO
        case persons =>
          (for {
            result <- g
              .N()
              .hasIri(persons.toSet.map(person => s"${graph.iri}/person/" + person))
              .has(schema.spouse)
              .head()
              .withGraph(graph)
              .headOptionF
              .map(_.isDefined)
              .map(Ok(_))
          } yield result).toIO
//        case _ => Task.now(NotAcceptable(new Exception("invalid parameters"))).toIO
      } :+: post(body[Task[PartnerTest], lspace.services.codecs.Application.JsonLD :+: Application.Json :+: CNil])
      .mapOutputAsync {
        case task =>
          task.flatMap {
            case partnerTest: PartnerTest if partnerTest.result.isDefined =>
              Task.now(NotAcceptable(new Exception("result should not yet be defined")))
            case partnerTest: PartnerTest =>
              for {
                isRelated <- g.N
                  .hasIri(partnerTest.person)
                  .has(schema.spouse)
                  .head()
                  .withGraph(graph)
                  .headOptionF
                  .map(_.isDefined)
              } yield {
                Ok(isRelated)
              }
            case _ => Task.now(NotAcceptable(new Exception("invalid parameters")))
          }.toIO
      }
  }

  val api = "partner" :: partner

  lazy val compiled: Endpoint.Compiled[IO] = {
    type Json = baseEncoder.Json
    implicit val be: NativeTypeEncoder.Aux[Json] = baseEncoder.asInstanceOf[NativeTypeEncoder.Aux[Json]]

    import lspace.services.codecs.Encode._
    implicit val encoder = jsonld.Encoder.apply(be)

    import EncodeText._
    import EncodeJson._
    import EncodeJsonLD._

    Bootstrap
      .configure(enableMethodNotAllowed = true, enableUnsupportedMediaType = true)
      .serve[Text.Plain :+: Application.Json :+: LApplication.JsonLD :+: CNil](api)
      .compile
  }
}
