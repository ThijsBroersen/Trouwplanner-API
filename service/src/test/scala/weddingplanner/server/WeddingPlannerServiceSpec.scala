package weddingplanner.server

import argonaut._
import argonaut.Argonaut._
import com.twitter.finagle.http.Status
import io.finch.Input
import io.finch.Application
import io.finch.argonaut.preserveOrder._
import lspace.codec.argonaut.Encoder
import lspace.librarian.provider.detached.DetachedGraph
import lspace.services.{LService, LServiceSpec}
import org.scalatest.BeforeAndAfterAll
import weddingplanner.ns.Agenda

class WeddingPlannerServiceSpec extends LServiceSpec with BeforeAndAfterAll {

  implicit val lservice: LService = WeddingPlannerService
  println(WeddingPlannerService.api.toString)

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
                property.label("en") -> (edges match {
                  case List(e) => Encoder.fromAny(e.to, Some(e.to.labels.head))(Encoder.getNewActiveContext).json
                })
            }
            .asJson)
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
  }
}
