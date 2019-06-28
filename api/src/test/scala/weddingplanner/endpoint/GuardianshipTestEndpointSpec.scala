package weddingplanner.endpoint

import java.time.LocalDate

import com.twitter.finagle.http.Status
import io.finch.Input
import lspace.ns.vocab.schema
import lspace.provider.mem.MemGraph
import lspace.services.codecs.{Application => LApplication}
import lspace.structure.Graph
import lspace.util.SampleGraph
import monix.eval.Task
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, FutureOutcome, Matchers}
import weddingplanner.ns.{GuardianshipTest, PartnerTest}

class GuardianshipTestEndpointSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  import lspace.Implicits.Scheduler.global
  import lspace.encode.EncodeJsonLD._
  import lspace.services.codecs.Encode._

  lazy val sampleGraph: Graph     = MemGraph("ApiServiceSpec")
  implicit val nencoder           = lspace.codec.argonaut.NativeTypeEncoder
  implicit val encoder            = lspace.codec.jsonld.Encoder(nencoder)
  implicit val ndecoder           = lspace.codec.argonaut.NativeTypeDecoder
  implicit lazy val activeContext = PartnerTestEndpoint.activeContext

  val guardianshipService = GuardianshipTestEndpoint(sampleGraph)

  lazy val initTask = (for {
    sample <- SampleGraph.loadSocial(sampleGraph)
    _ <- for {
      curatele  <- sampleGraph.nodes.upsert("curatele-gray", weddingplanner.ns.LegalRestraint)
      _         <- sample.persons.Gray.person --- weddingplanner.ns.underLegalRestraint --> curatele
      _         <- curatele --- schema.startDate --> LocalDate.parse("2016-01-02")
      _         <- curatele --- schema.endDate --> LocalDate.parse("2016-12-12")
      curatele2 <- sampleGraph.nodes.upsert("curatele-yoshio", weddingplanner.ns.LegalRestraint)
      _         <- sample.persons.Yoshio.person --- weddingplanner.ns.underLegalRestraint --> curatele2
      _         <- curatele2 --- schema.startDate --> LocalDate.parse("2018-01-02")
    } yield ()
  } yield sample).memoizeOnSuccess

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    new FutureOutcome(initTask.runToFuture flatMap { result =>
      super.withFixture(test).toFuture
    })
  }
  "A PartnerEndpoint" should {
    "test positive for a LegalConstraint for Gray" in {
      (for {
        sample <- initTask
        gray = sample.persons.Gray.person
        test = GuardianshipTest(gray.iri, Some(LocalDate.parse("2016-12-01")))
        node <- test.toNode
      } yield {
        val input = Input
          .post("/guardianship")
          .withBody[LApplication.JsonLD](node)
        guardianshipService
          .guardianship(input)
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
    "test positive for a LegalConstraint for Yoshio" in {
      (for {
        sample <- initTask
        test = GuardianshipTest(sample.persons.Yoshio.person.iri)
        node <- test.toNode
      } yield {
        val input = Input
          .post("/guardianship")
          .withBody[LApplication.JsonLD](node)
        guardianshipService
          .guardianship(input)
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
    "test negative for a LegalConstraint for Stan" in {
      (for {
        sample <- initTask
        stan = sample.persons.Stan.person
        test = GuardianshipTest(stan.iri)
        node <- test.toNode
      } yield {
        val input = Input
          .post("/guardianship")
          .withBody[LApplication.JsonLD](node)
        guardianshipService
          .guardianship(input)
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
    "test negative for a LegalConstraint for Yoshio on 2015-01-01" in {
      (for {
        sample <- initTask
        test = GuardianshipTest(sample.persons.Yoshio.person.iri, Some(LocalDate.parse("2015-01-01")))
        node <- test.toNode
      } yield {
        val input = Input
          .post("/guardianship")
          .withBody[LApplication.JsonLD](node)
        guardianshipService
          .guardianship(input)
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
  }
}
