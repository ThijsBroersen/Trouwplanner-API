package weddingplanner.endpoint

import java.net.URLEncoder
import java.time.Instant

import cats.effect.IO
import io.finch.Endpoint
import lspace._
import lspace.ns.vocab.schema
import lspace.provider.mem.MemGraph
import lspace.services.rest.endpoints.TraversalService
import lspace.structure.{Graph, OntologyDef, Property, PropertyDef}
import monix.eval.Task
import shapeless.HNil
import weddingplanner.ns.{agenda, Agenda, Appointment}

import scala.util.{Failure, Success, Try}

case class KinsmanEndpoint[Json](peopleGraph: Graph)(implicit ndecoder: lspace.codec.NativeTypeDecoder.Aux[Json],
                                                     nencoder: lspace.codec.NativeTypeEncoder.Aux[Json])
    extends Endpoint.Module[IO] {

  implicit val ec = monix.execution.Scheduler.global

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

  object KinsmanTest
      extends OntologyDef(
        "http://ns.convenantgemeenten.nl/kinsmantest",
        label = "Kinsman test",
        comment = "A kinsman test is an assertion whether two people are related to some extend"
      ) {
    object keys extends schema.Thing.Properties {
      object person1
          extends PropertyDef(ontology.iri + "/person1",
                              label = "person1",
                              `@range` = () => schema.Person.ontology :: Nil)
      object person2
          extends PropertyDef(ontology.iri + "/person2",
                              label = "person2",
                              `@range` = () => schema.Person.ontology :: Nil)
    }
    override lazy val properties
      : List[Property] = keys.person1.property :: keys.person2.property :: schema.Thing.properties
    trait Properties {
      lazy val person1 = keys.person1
      lazy val person2 = keys.person2
    }
  }
  case class KinsmanTest(person1: String, person2: String, degree: Option[Int])

  import lspace.decode.DecodeJsonLD._

  /**
    * tests if a kinsman path exists between two persons
    */
  val related: Endpoint[IO, Boolean] = {
    import shapeless.::
    get(params[String]("iri") :: paramOption[Int]("degree"))
      .mapOutputAsync {
        case (List(person1, person2) :: (degree: Option[Int]) :: HNil) if degree.exists(_ < 1) =>
          Task.now(NotAcceptable(new Exception("degree must be > 0"))).toIO
        case (List(person1, person2) :: (degree: Option[Int]) :: HNil) =>
          (for {
            isRelated <- g.N
              .hasIri(URLEncoder.encode(person1, "UTF-8"))
              .repeat(_.out(schema.parent, schema.children), degree.getOrElse(1), true, true)(
                _.hasIri(URLEncoder.encode(person2, "UTF-8")))
              .head()
              .withGraph(peopleGraph)
              .headOptionF
              .map(_.isDefined)
          } yield {
            Ok(isRelated)
          }).toIO
        case _ => Task.now(NotAcceptable(new Exception("invalid parameters"))).toIO
      }
//    :+: post(bodyJsonldTyped[KinsmanTest](KinsmanTest.ontology, ))
//      .mapOutputAsync {
//        case kt: KinsmanTest if kt.degree.exists(_ < 1) =>
//          Task.now(NotAcceptable(new Exception("degree must be > 0"))).toIO
//        case kt: KinsmanTest =>
//          (for {
//            isRelated <- g.N
//              .hasIri(URLEncoder.encode(kt.person1, "UTF-8"))
//              .repeat(_.out(schema.parent, schema.children), kt.degree.getOrElse(1), true, true)(
//                _.hasIri(URLEncoder.encode(kt.person2, "UTF-8")))
//              .head()
//              .withGraph(peopleGraph)
//              .headOptionF
//              .map(_.isDefined)
//          } yield {
//            Ok(isRelated)
//          }).toIO
//        case _ => Task.now(NotAcceptable(new Exception("invalid parameters"))).toIO
//      }

  }

  val api = "kinsman" :: related
}
