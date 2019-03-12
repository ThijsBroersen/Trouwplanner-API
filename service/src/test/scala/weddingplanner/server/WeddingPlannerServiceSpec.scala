package weddingplanner.server

import com.twitter.finagle.http.Status
import io.finch.Input
import io.finch.Application
import lspace.codec.ActiveContext
import lspace.provider.detached.DetachedGraph
import lspace.services.{LService, LServiceSpec}
import org.scalatest.BeforeAndAfterAll
import weddingplanner.ns.Agenda

class WeddingPlannerServiceSpec extends LServiceSpec with BeforeAndAfterAll {

  implicit val lservice: LService = WeddingPlannerService
  println(WeddingPlannerService.api.toString)

  import lspace.codec.argonaut._
  val encoder: lspace.codec.Encoder = lspace.codec.Encoder(nativeEncoder)
  import encoder._
  import lspace.encode.EncodeJson._
  import lspace.services.codecs.Encode._

  "The Wedding-planner" must {
    WeddingPlannerService.agendaService.service.labeledApiTests
    WeddingPlannerService.appointmentService.service.labeledApiTests
    WeddingPlannerService.personService.service.labeledApiTests
    WeddingPlannerService.placeService.service.labeledApiTests

    val label = WeddingPlannerService.agendaService.service.label
    s"have an $label-api which accepts json" in {
      val node = DetachedGraph.nodes.create(Agenda.ontology)
      node --- Agenda.keys.name --> "Alice"
      import lspace.services.util._
      val input = Input
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
      val res = lservice.service(input.request)

      res.map { response =>
        val headers = response.headerMap
        response.status shouldBe Status.Created
        response.contentType shouldBe Some("application/json")
      }
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

    "return active ontology for path" in {
      import lspace.services.util._
      val input = Input
        .get("/person.jsonld")
        .withHeaders("Accept" -> "application/ld+json")
      val res = WeddingPlannerService.service(input.request)

      res.map { response =>
        response.status shouldBe Status.Ok
      }
    }
  }
}
