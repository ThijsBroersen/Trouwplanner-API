package weddingplanner.server

import com.twitter.finagle.http.Status
import io.finch.Input
import io.finch.Application
import lspace.codec.ActiveContext
import lspace.ns.vocab.schema
import lspace.provider.detached.DetachedGraph
import lspace.services.{LService, LServiceSpec}
import monix.eval.Task
import org.scalatest.BeforeAndAfterAll
import weddingplanner.ns.Agenda

class WeddingPlannerServiceSpec extends LServiceSpec with BeforeAndAfterAll {

  implicit val lservice: LService = WeddingPlannerService

  import lspace.Implicits.Scheduler.global
  import lspace.codec.argonaut._
  val encoder: lspace.codec.jsonld.Encoder = lspace.codec.jsonld.Encoder(nativeEncoder)
  import encoder._
  import lspace.encode.EncodeJson._
  import lspace.services.codecs.Encode._

  "The Wedding-planner" must {
    WeddingPlannerService.agendaService.labeledApiTests
    WeddingPlannerService.appointmentService.labeledApiTests
    WeddingPlannerService.personService.labeledApiTests
    WeddingPlannerService.placeService.labeledApiTests
    WeddingPlannerService.reportToMarriageService.service.labeledApiTests
    WeddingPlannerService.weddingReservationService.service.labeledApiTests

    val label = WeddingPlannerService.agendaService.label
    s"have an $label-api which accepts json" in {
      implicit val activeContext = ActiveContext()
      import lspace.services.util._
      (for {
        node <- DetachedGraph.nodes.create(Agenda.ontology)
        _    <- node --- schema.name --> "Alice"
        input = Input
          .post(s"/agenda/")
          .withBody[Application.Json](
            node
              .outEMap()
              .map {
                case (property, edges) =>
                  property.label("en").get -> (edges match {
                    case List(e) => encoder.fromAny(e.to, Some(e.to.labels.head))(ActiveContext()).json
                  })
              }
              .asJson
              .noSpaces)
          .withHeaders("Accept" -> "application/json")
        res = lservice.service(input.request)
        _ <- Task.deferFuture {
          res.map { response =>
            val headers = response.headerMap
            response.status shouldBe Status.Created
            response.contentType shouldBe Some("application/json")
          }
        }
//        _ <- Task.deferFuture {
//          lservice.service(Input.get(s"/agenda/"))
//        }
      } yield succeed).runToFuture
    }

    "not have an unknown IDONOTEXIST-api" in {
      import lspace.services.util._
      val input = Input
        .get("/IDONOTEXIST/")
        .withHeaders("Accept" -> "application/ld+json")
      val res = WeddingPlannerService.service(input.request)

      res.map { response =>
        response.status shouldBe Status.NotFound
      }
    }

    "return active context for path /person/context" in {
      import lspace.services.util._
      val input = Input
        .get("/person/context")
        .withHeaders("Accept" -> "application/ld+json")
      val res = WeddingPlannerService.service(input.request)

      res.map { response =>
        response.status shouldBe Status.Ok
      }
    }
    "return active context for path /agenda/context" in {
      import lspace.services.util._
      val input = Input
        .get("/agenda/context")
        .withHeaders("Accept" -> "application/ld+json")
      val res = WeddingPlannerService.service(input.request)

      res.map { response =>
        response.status shouldBe Status.Ok
//        response.contentString shouldBe """@context: {}"""
      }
    }

    "reset the graph on /clear" in {
      import lspace.services.util._
      val input = Input
        .get("/clear")
        .withHeaders("Accept" -> "application/json")
      val res = WeddingPlannerService.service(input.request)

      res.flatMap { response =>
        Thread.sleep(2000)
        val input = Input
          .get("/person")
          .withHeaders("Accept" -> "application/json")
        val res = WeddingPlannerService.service(input.request)

        res.map { response =>
          response.contentString shouldBe "[]"
          response.status shouldBe Status.Ok
        }
      }
    }
  }
}
