package weddingplanner.endpoint

import java.time.{Instant, LocalDate}

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
import shapeless.{:+:, ::, CNil, HNil}
import weddingplanner.ns.GuardianshipTest

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success, Try}

object GuardianshipTestEndpoint {
  def apply(graph: Graph, activeContext: ActiveContext = ActiveContext())(
      implicit baseDecoder: lspace.codec.NativeTypeDecoder,
      baseEncoder: lspace.codec.NativeTypeEncoder): GuardianshipTestEndpoint =
    new GuardianshipTestEndpoint(graph)(baseDecoder, baseEncoder, activeContext)

  lazy val activeContext = ActiveContext(
    `@prefix` = ListMap(
      "person"     -> GuardianshipTest.keys.person.iri,
      "targetDate" -> GuardianshipTest.keys.targetDate.iri
    ),
    definitions = Map(
      GuardianshipTest.keys.person.iri -> ActiveProperty(`@type` = schema.Person :: Nil,
                                                         property = GuardianshipTest.keys.person)(),
      GuardianshipTest.keys.targetDate.iri -> ActiveProperty(`@type` = `@date` :: Nil,
                                                             property = GuardianshipTest.keys.targetDate)()
    )
  )
}

class GuardianshipTestEndpoint(graph: Graph)(implicit val baseDecoder: lspace.codec.NativeTypeDecoder,
                                             val baseEncoder: lspace.codec.NativeTypeEncoder,
                                             activeContext: ActiveContext)
    extends Endpoint.Module[IO] {

  implicit val ec = monix.execution.Scheduler.global

  type Json = baseDecoder.Json
  implicit val bd: NativeTypeDecoder.Aux[Json] = baseDecoder.asInstanceOf[NativeTypeDecoder.Aux[Json]]

  import lspace.services.codecs.Decode._
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

  /**
    * tests if a kinsman path exists between two
    * TODO: update graph with latest (remote) data
    */
  val guardianship: Endpoint[IO, Boolean :+: Boolean :+: CNil] = {
    implicit val bodyJsonldTyped = DecodeJsonLD
      .bodyJsonldTyped(GuardianshipTest.ontology, GuardianshipTest.fromNode)

    implicit val jsonToNodeToT = DecodeJson
      .jsonToNodeToT(GuardianshipTest.ontology, GuardianshipTest.fromNode)

    implicit val decodeDate: DecodeEntity[LocalDate] = new DecodeEntity[LocalDate] {
      def apply(s: String): Either[Throwable, LocalDate] =
        try {
          Right(LocalDate.parse(s))
        } catch {
          case e: Throwable => Left(e)
        }
    }

    get(param[String]("id") :: paramOption[LocalDate]("targetDate"))
      .mapOutputAsync {
        case person :: targetDate :: HNil =>
          val targetDate0 = targetDate.getOrElse(LocalDate.now())
          (for {
            result <- g
              .N()
              .hasIri(s"${graph.iri}/person/" + person)
              .where(
                _.out(weddingplanner.ns.underLegalRestraint)
                  .hasLabel(weddingplanner.ns.LegalRestraint)
                  .has(schema.startDate, P.lt(targetDate0))
                  .or(
                    _.has(schema.endDate, P.gt(targetDate0)),
                    _.hasNot(schema.endDate)
                  ))
              .head()
              .withGraph(graph)
              .headOptionF
              .map(_.isDefined)
              .map(Ok(_))
          } yield result).to[IO]
//        case _ => Task.now(NotAcceptable(new Exception("invalid parameters"))).to[IO]
      } :+: post(body[Task[GuardianshipTest], lspace.services.codecs.Application.JsonLD :+: Application.Json :+: CNil])
      .mapOutputAsync {
        case task =>
          task
            .flatMap {
              case guardianshipTest: GuardianshipTest if guardianshipTest.result.isDefined =>
                Task.now(NotAcceptable(new Exception("result should not yet be defined")))
              case guardianshipTest: GuardianshipTest =>
                val targetDate = guardianshipTest.targetDate.getOrElse(LocalDate.now())
                for {
                  isRelated <- g.N
                    .hasIri(guardianshipTest.person)
                    .where(
                      _.out(weddingplanner.ns.underLegalRestraint)
                        .hasLabel(weddingplanner.ns.LegalRestraint)
                        .has(schema.startDate, P.lt(targetDate))
                        .or(
                          _.has(schema.endDate, P.gt(targetDate)),
                          _.hasNot(schema.endDate)
                        ))
                    .head()
                    .withGraph(graph)
                    .headOptionF
                    .map(_.isDefined)
                } yield {
                  Ok(isRelated)
                }
              case _ => Task.now(NotAcceptable(new Exception("invalid parameters")))
            }
            .to[IO]
      }
  }

  val api = "guardianship" :: guardianship

  lazy val compiled: Endpoint.Compiled[IO] = {
    type Json = baseEncoder.Json
    implicit val be: NativeTypeEncoder.Aux[Json] = baseEncoder.asInstanceOf[NativeTypeEncoder.Aux[Json]]

    import lspace.services.codecs.Encode._
    implicit val encoder = jsonld.Encoder.apply(be)

    import EncodeJson._
    import EncodeJsonLD._
    import EncodeText._

    Bootstrap
      .configure(enableMethodNotAllowed = true, enableUnsupportedMediaType = true)
      .serve[Text.Plain :+: Application.Json :+: LApplication.JsonLD :+: CNil](api)
      .compile
  }
}
