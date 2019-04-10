package weddingplanner.endpoint

import java.net.{URLDecoder, URLEncoder}
import java.time.Instant

import cats.effect.IO
import io.finch.Endpoint
import lspace._
import lspace.codec.{Decoder, NativeTypeDecoder}
import lspace.decode.{DecodeJson, DecodeJsonLD}
import lspace.ns.vocab.schema
import lspace.provider.detached.DetachedGraph
import lspace.provider.mem.MemGraph
import lspace.services.rest.endpoints.TraversalService
import lspace.structure.{Graph, Node, Ontology, OntologyDef, Property, PropertyDef, TypedProperty}
import monix.eval.Task
import shapeless.{:+:, CNil, HNil}
import weddingplanner.ns.{agenda, Agenda, Appointment, KinsmanTest}

import scala.util.{Failure, Success, Try}

case class KinsmanTestEndpoint(peopleGraph: Graph)(implicit val baseDecoder: lspace.codec.NativeTypeDecoder,
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
  val kinsman: Endpoint[IO, Boolean :+: Boolean :+: CNil] = {
    import shapeless.::
    import io.finch.internal.HttpContent
    implicit val decoder = Decoder(DetachedGraph)
    implicit val d1 = io.finch.Decode
      .instance[Task[KinsmanTest], lspace.services.codecs.Application.JsonLD] { (b, cs) =>
        Right(
          DecodeJsonLD
            .bodyJsonldTyped(KinsmanTest.ontology, KinsmanTest.fromNode)
            .decode(b.asString(cs)))
      }

    implicit val d2 = io.finch.Decode.instance[Task[KinsmanTest], Application.Json] { (b, cs) =>
      Right(
        DecodeJson
          .bodyJsonTyped(KinsmanTest.ontology, KinsmanTest.fromNode)
          .decode(b.asString(cs)))
    }

    get(params[String]("iri") :: paramOption[Int]("degree"))
      .mapOutputAsync {
        case (List(person1, person2) :: (degree: Option[Int]) :: HNil) if degree.exists(_ < 1) =>
          Task.now(NotAcceptable(new Exception("degree must be > 0"))).toIO
        case (List(person1, person2) :: (degree: Option[Int]) :: HNil) =>
          (for {
            p1 <- g.N.hasIri(URLDecoder.decode(person1, "UTF-8")).withGraph(peopleGraph).headOptionF
            p2 <- g.N.hasIri(URLDecoder.decode(person2, "UTF-8")).withGraph(peopleGraph).headOptionF
            result <- if (p1.isDefined && p2.isDefined)
              g.N(p1.get)
                .repeat(_.out(schema.parent, schema.children), degree.map(_ - 1).getOrElse(0), true, true)(
                  _.is(P.eqv(p2.get)))
                .head()
                .withGraph(peopleGraph)
                .headOptionF
                .map(_.isDefined)
                .map(Ok(_))
            else Task.now(NotAcceptable(new Exception("one or both of the wedding couple could not be found")))
          } yield result).toIO
        case _ => Task.now(NotAcceptable(new Exception("invalid parameters"))).toIO
      } :+: post(body[Task[KinsmanTest], lspace.services.codecs.Application.JsonLD /* :+: Application.Json :+: CNil*/ ])
      .mapOutputAsync {
        case task =>
          task
            .flatMap {
              case kinsmanTest: KinsmanTest if !kinsmanTest.degree.exists(_ > 0) =>
                Task.now(NotAcceptable(new Exception("degree must be > 0")))
              case kinsmanTest: KinsmanTest if kinsmanTest.result.isDefined =>
                Task.now(NotAcceptable(new Exception("result should not yet be defined")))
              case kinsmanTest: KinsmanTest =>
                for {
                  isRelated <- if (kinsmanTest.degree.contains(1))
                    g.N
                      .hasIri(kinsmanTest.person1)
                      .out(schema.parent, schema.children)
                      .hasIri(kinsmanTest.person2)
                      .head()
                      .withGraph(peopleGraph)
                      .headOptionF
                      .map(_.isDefined)
                  else
                    g.N
                      .hasIri(kinsmanTest.person1)
                      .repeat(_.out(schema.parent, schema.children),
                              kinsmanTest.degree.map(_ - 1).getOrElse(0),
                              true,
                              true)(_.hasIri(kinsmanTest.person2))
                      .hasIri(kinsmanTest.person2)
                      .head()
                      .withGraph(peopleGraph)
                      .headOptionF
                      .map(_.isDefined)
                } yield Ok(isRelated)
              case _ =>
                Task.now(NotAcceptable(new Exception("invalid parameters")))
            }
            .onErrorHandle { f =>
              println(f.getMessage); InternalServerError(new Exception("unknown error with request input"))
            }
            .toIO
      }
  }

  val api = "kinsman" :: kinsman
}
