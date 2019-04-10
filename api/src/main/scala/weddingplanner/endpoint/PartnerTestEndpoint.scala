package weddingplanner.endpoint

import java.net.URLEncoder
import java.time.Instant

import cats.effect.IO
import io.finch.Endpoint
import lspace._
import lspace.codec.{Decoder, NativeTypeDecoder}
import lspace.datatype.ListType
import lspace.decode.{DecodeJson, DecodeJsonLD}
import lspace.ns.vocab.schema
import lspace.provider.detached.DetachedGraph
import lspace.provider.mem.MemGraph
import lspace.services.rest.endpoints.TraversalService
import lspace.structure.{Graph, Node, Ontology, OntologyDef, Property, PropertyDef, TypedProperty}
import monix.eval.Task
import shapeless.{:+:, CNil, HNil}
import weddingplanner.ns.{agenda, Agenda, Appointment, PartnerTest}

import scala.util.{Failure, Success, Try}

case class PartnerTestEndpoint(peopleGraph: Graph)(implicit val baseDecoder: lspace.codec.NativeTypeDecoder,
                                                   val baseEncoder: lspace.codec.NativeTypeEncoder)
    extends Endpoint.Module[IO] {

  implicit val ec = monix.execution.Scheduler.global

  type Json = baseDecoder.Json
  implicit val bd: NativeTypeDecoder.Aux[Json] = baseDecoder.asInstanceOf[NativeTypeDecoder.Aux[Json]]

  implicit val decoder = lspace.codec.Decoder(DetachedGraph)

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
    * tests if a kinsman path exists between two persons
    */
  val partner: Endpoint[IO, Boolean :+: Boolean :+: CNil] = {
    import shapeless.::
    import io.finch.internal.HttpContent
    implicit val decoder = Decoder(DetachedGraph)
    implicit val d1 = io.finch.Decode
      .instance[Task[PartnerTest], lspace.services.codecs.Application.JsonLD] { (b, cs) =>
        Right(
          DecodeJsonLD
            .bodyJsonldTyped(PartnerTest.ontology, PartnerTest.fromNode)
            .decode(b.asString(cs)))
      }

    implicit val d2 = io.finch.Decode.instance[Task[PartnerTest], Application.Json] { (b, cs) =>
      Right(
        DecodeJson
          .bodyJsonTyped(PartnerTest.ontology, PartnerTest.fromNode)
          .decode(b.asString(cs)))
    }

    get(params[String]("iri"))
      .mapOutputAsync {
        case Nil =>
          Task.now(NotAcceptable(new Exception("a partnertest must have persons as input"))).toIO
        case persons =>
          (for {
            result <- g
              .N()
              .hasIri(persons.toSet)
              .has(schema.spouse)
              .head()
              .withGraph(peopleGraph)
              .headOptionF
              .map(_.isDefined)
              .map(Ok(_))
          } yield result).toIO
        case _ => Task.now(NotAcceptable(new Exception("invalid parameters"))).toIO
      } :+: post(body[Task[PartnerTest], lspace.services.codecs.Application.JsonLD /* :+: Application.Json :+: CNil*/ ])
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
                  .withGraph(peopleGraph)
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
}
