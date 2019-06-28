package weddingplanner.endpoint

import java.net.{URLDecoder, URLEncoder}
import java.time.Instant

import cats.effect.IO
import io.finch.Endpoint
import lspace._
import Label.D._
import lspace.codec.{jsonld, ActiveContext, ActiveProperty, NativeTypeDecoder, NativeTypeEncoder}
import lspace.decode.{DecodeJson, DecodeJsonLD}
import lspace.encode.{EncodeJson, EncodeJsonLD, EncodeText}
import lspace.ns.vocab.schema
import lspace.provider.detached.DetachedGraph
import lspace.services.LApplication
import monix.eval.Task
import shapeless.{:+:, CNil, HNil}
import weddingplanner.ns.KinsmanTest

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success, Try}

object KinsmanTestEndpoint {
  def apply(graph: Graph, activeContext: ActiveContext = ActiveContext())(
      implicit baseDecoder: lspace.codec.NativeTypeDecoder,
      baseEncoder: lspace.codec.NativeTypeEncoder): KinsmanTestEndpoint =
    new KinsmanTestEndpoint(graph)(baseDecoder, baseEncoder, activeContext)

  lazy val activeContext = ActiveContext(
    `@prefix` = ListMap(
      "person1" -> KinsmanTest.keys.person1.iri,
      "person2" -> KinsmanTest.keys.person2.iri,
      "degree"  -> KinsmanTest.keys.degree.iri
    ),
    definitions = Map(
      KinsmanTest.keys.person1.iri -> ActiveProperty(`@type` = schema.Person :: Nil,
                                                     property = KinsmanTest.keys.person1)(),
      KinsmanTest.keys.person2.iri -> ActiveProperty(`@type` = schema.Person :: Nil,
                                                     property = KinsmanTest.keys.person2)(),
      KinsmanTest.keys.degree.iri -> ActiveProperty(`@type` = `@int` :: Nil, property = KinsmanTest.keys.degree)()
    )
  )
}
class KinsmanTestEndpoint(graph: Graph)(implicit val baseDecoder: lspace.codec.NativeTypeDecoder,
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
    * tests if a kinsman path exists between two persons
    * TODO: update graph with latest (remote) data
    */
  val kinsman: Endpoint[IO, Boolean :+: Boolean :+: CNil] = {
    import shapeless.::
    import io.finch.internal.HttpContent

    implicit val bodyJsonldTyped = DecodeJsonLD
      .bodyJsonldTyped(KinsmanTest.ontology, KinsmanTest.fromNode)

    implicit val jsonToNodeToT = DecodeJson
      .jsonToNodeToT(KinsmanTest.ontology, KinsmanTest.fromNode)

    get(params[String]("id") :: paramOption[Int]("degree"))
      .mapOutputAsync {
        case (List(person1, person2) :: (degree: Option[Int]) :: HNil) if degree.exists(_ < 1) =>
          Task.now(NotAcceptable(new Exception("degree must be > 0"))).to[IO]
        case (List(person1, person2) :: (degree: Option[Int]) :: HNil) =>
          (for {
            p1 <- g.N.hasIri(s"${graph.iri}/person/" + person1).withGraph(graph).headOptionF
            p2 <- g.N.hasIri(s"${graph.iri}/person/" + person2).withGraph(graph).headOptionF
            result <- if (p1.isDefined && p2.isDefined)
              g.N(p1.get)
                .repeat(_.out(schema.parent, schema.children), degree.map(_ - 1).getOrElse(0), true, true)(
                  _.is(P.eqv(p2.get)))
                .head()
                .withGraph(graph)
                .headOptionF
                .map(_.isDefined)
                .map(Ok(_))
            else Task.now(NotAcceptable(new Exception("one or both of the wedding couple could not be found")))
          } yield result).to[IO]
        case _ => Task.now(NotAcceptable(new Exception("invalid parameters"))).to[IO]
      } :+: post(body[Task[KinsmanTest], lspace.services.codecs.Application.JsonLD :+: Application.Json :+: CNil])
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
                      .withGraph(graph)
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
                      .withGraph(graph)
                      .headOptionF
                      .map(_.isDefined)
                } yield Ok(isRelated)
              case _ =>
                Task.now(NotAcceptable(new Exception("invalid parameters")))
            }
            .onErrorHandle { f =>
              println(f.getMessage); InternalServerError(new Exception("unknown error with request input"))
            }
            .to[IO]
      }
  }

  lazy val api = "kinsman" :: kinsman

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
