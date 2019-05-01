package weddingplanner.endpoint

import com.twitter.finagle.http.Status
import io.finch.Input
import lspace.codec.ActiveContext
import lspace.ns.vocab.schema
import lspace.provider.mem.MemGraph
import lspace.services.codecs.{Application => LApplication}
import lspace.structure.Graph
import lspace.util.SampleGraph
import monix.eval.Task
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, FutureOutcome, Matchers}
import weddingplanner.ns.KinsmanTest

class KinsmanTestEndpointSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  import lspace.Implicits.Scheduler.global
  import argonaut._, Argonaut._
  import lspace.encode.EncodeJsonLD._
  import lspace.services.codecs.Encode._

  lazy val sampleGraph: Graph = MemGraph("ApiServiceSpec")
  implicit val nencoder       = lspace.codec.argonaut.NativeTypeEncoder
  implicit val encoder        = lspace.codec.jsonld.Encoder(nencoder)
  implicit val ndecoder       = lspace.codec.argonaut.NativeTypeDecoder
  implicit val activeContext  = ActiveContext()

  val kinsmanService = KinsmanTestEndpoint(sampleGraph)

  lazy val initTask = (for {
    sample <- SampleGraph.loadSocial(sampleGraph)
    _ <- for {
      _ <- sample.persons.Gray.person --- schema.parent --> sample.persons.Levi.person
      _ <- sample.persons.Levi.person --- schema.parent --> sample.persons.Garrison.person
    } yield ()
  } yield sample).memoizeOnSuccess

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    new FutureOutcome(initTask.runToFuture flatMap { result =>
      super.withFixture(test).toFuture
    })
  }
  import lspace.Implicits.AsyncGuide.guide
  "A KinsmanEndpoint" should {
    "test positive for a family-relation between Gray and Levi" in {
      (for {
        sample <- initTask
        gray = sample.persons.Gray.person
        levi = sample.persons.Levi.person
        test = KinsmanTest(gray.iri, levi.iri, Some(2))
        node <- test.toNode
      } yield {
        val input = Input
          .post("/kinsman")
          .withBody[LApplication.JsonLD](node)
        kinsmanService
          .kinsman(input)
          .awaitOutput()
          .map { output =>
            output.isRight shouldBe true
            val response = output.right.get
            response.status shouldBe Status.Ok
            response.value.tail.get.head.get shouldBe true
          }
          .getOrElse(fail("endpoint does not match"))
      }).runToFuture
    }
    "test negative for a family-relation between Gray and Stan" in {
      (for {
        sample <- initTask
        gray = sample.persons.Gray.person
        stan = sample.persons.Stan.person
        test = KinsmanTest(gray.iri, stan.iri, Some(2))
        node <- test.toNode
      } yield {
        val input = Input
          .post("/kinsman")
          .withBody[LApplication.JsonLD](node)
        kinsmanService
          .kinsman(input)
          .awaitOutput()
          .map { output =>
            output.isRight shouldBe true
            val response = output.right.get
            response.status shouldBe Status.Ok
            response.value.tail.get.head.get shouldBe false
          }
          .getOrElse(fail("endpoint does not match"))
      }).runToFuture
    }
    "test negative for a family-relation between Gray and Garrison (via Levi) with degree 1" in {
      (for {
        sample <- initTask
        gray     = sample.persons.Gray.person
        garrison = sample.persons.Garrison.person
        test     = KinsmanTest(gray.iri, garrison.iri, Some(1))
        node <- test.toNode
      } yield {
        val input = Input
          .post("/kinsman")
          .withBody[LApplication.JsonLD](node)
        kinsmanService
          .kinsman(input)
          .awaitOutput()
          .map { output =>
            output.isRight shouldBe true
            val response = output.right.get
            response.status shouldBe Status.Ok
            response.value.tail.get.head.get shouldBe false
          }
          .getOrElse(fail("endpoint does not match"))
      }).runToFuture
    }
    "test positive for a family-relation between Gray and Garrison (via Levi) with degree 2" in {
      (for {
        sample <- initTask
        gray     = sample.persons.Gray.person
        garrison = sample.persons.Garrison.person
        test     = KinsmanTest(gray.iri, garrison.iri, Some(2))
        node <- test.toNode
      } yield {
        val input = Input
          .post("/kinsman")
          .withBody[LApplication.JsonLD](node)
        kinsmanService
          .kinsman(input)
          .awaitOutput()
          .map { output =>
            output.isRight shouldBe true
            val response = output.right.get
            response.status shouldBe Status.Ok
            response.value.tail.get.head.get shouldBe true
          }
          .getOrElse(fail("endpoint does not match"))
      }).runToFuture
    }
  }
  "A compiled KinsmanEndpoint" should {
    "test positive for a family-relation between Gray and Levi" in {
      (for {
        sample <- initTask
        gray = sample.persons.Gray.person
        levi = sample.persons.Levi.person
        test = KinsmanTest(gray.iri, levi.iri, Some(2))
        node <- test.toNode
        input = Input
          .post("/kinsman")
          .withBody[LApplication.JsonLD](node)
        _ <- Task.fromIO(
          kinsmanService
            .compiled(input.request)
            .map {
              case (t, Left(e)) => fail()
              case (t, Right(r)) =>
                r.status shouldBe Status.Ok
                r.contentString shouldBe "true"
            })
      } yield succeed).runToFuture
    }
    "test negative for a family-relation between Gray and Stan" in {
      (for {
        sample <- initTask
        gray = sample.persons.Gray.person
        stan = sample.persons.Stan.person
        test = KinsmanTest(gray.iri, stan.iri, Some(2))
        node <- test.toNode
        input = Input
          .post("/kinsman")
          .withBody[LApplication.JsonLD](node)
        _ <- Task.fromIO(
          kinsmanService
            .compiled(input.request)
            .map {
              case (t, Left(e)) => fail()
              case (t, Right(r)) =>
                r.status shouldBe Status.Ok
                r.contentString shouldBe "false"
            })
      } yield succeed).runToFuture
    }
    "test negative for a family-relation between Gray and Garrison (via Levi) with degree 1" in {
      (for {
        sample <- initTask
        gray     = sample.persons.Gray.person
        garrison = sample.persons.Garrison.person
        test     = KinsmanTest(gray.iri, garrison.iri, Some(1))
        node <- test.toNode
        input = Input
          .post("/kinsman")
          .withBody[LApplication.JsonLD](node)
        _ <- Task.fromIO(
          kinsmanService
            .compiled(input.request)
            .map {
              case (t, Left(e)) => fail()
              case (t, Right(r)) =>
                r.status shouldBe Status.Ok
                r.contentString shouldBe "false"
            })
      } yield succeed).runToFuture
    }
    "test positive for a family-relation between Gray and Garrison (via Levi) with degree 2" in {
      (for {
        sample <- initTask
        gray     = sample.persons.Gray.person
        garrison = sample.persons.Garrison.person
        test     = KinsmanTest(gray.iri, garrison.iri, Some(2))
        node <- test.toNode
        input = Input
          .post("/kinsman")
          .withBody[LApplication.JsonLD](node)
        _ <- Task.fromIO(
          kinsmanService
            .compiled(input.request)
            .map {
              case (t, Left(e)) => fail()
              case (t, Right(r)) =>
                r.status shouldBe Status.Ok
                r.contentString shouldBe "true"
            })
      } yield succeed).runToFuture
    }
  }
}
