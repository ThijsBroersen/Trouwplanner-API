package weddingplanner.server

import com.twitter.finagle.http.Status
import io.finch.Input
import lspace.librarian.structure.Ontology
import lspace.services.{LService, LServiceSpec}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import weddingplanner.ns.{Agenda, Appointment, Person, Place}

import scala.concurrent.Future

class WeddingPlannerServiceSpec extends LServiceSpec with BeforeAndAfterAll {

  implicit val lservice: LService = WeddingPlannerService
  println(WeddingPlannerService.api.toString)

  "The Wedding-planner" must {
    WeddingPlannerService.agendaService.service.labeledApiTests
    WeddingPlannerService.appointmentService.service.labeledApiTests
    WeddingPlannerService.personService.service.labeledApiTests
    WeddingPlannerService.placeService.service.labeledApiTests

//    "have an Agenda-api" in {
//      val input = Input
//        .get("/Agenda/")
//        .withHeaders("Accept" -> "application/ld+json")
//      val res = WeddingPlannerServer.api(input.request)
//
//      res.map { response =>
//        val headers = response.headerMap
//        response.status shouldBe Status.Ok
//        response.contentType shouldBe Some("application/ld+json")
//      }
//    }
//    "have an Appointment-api" in {
//      val input = Input
//        .get("/Appointment/")
//        .withHeaders("Accept" -> "application/ld+json")
//      val res = WeddingPlannerServer.api(input.request)
//
//      res.map { response =>
//        response.status shouldBe Status.Ok
//        response.contentType shouldBe Some("application/ld+json")
//      }
//    }
//    "have an Person-api" in {
//      val input = Input
//        .get("/Appointment/")
//        .withHeaders("Accept" -> "application/ld+json")
//      val res = WeddingPlannerServer.api(input.request)
//
//      res.map { response =>
//        response.status shouldBe Status.Ok
//        response.contentType shouldBe Some("application/ld+json")
//      }
//    }
//    "have an Place-api" in {
//      val input = Input
//        .get("/Appointment/")
//        .withHeaders("Accept" -> "application/ld+json")
//      val res = WeddingPlannerServer.api(input.request)
//
//      res.map { response =>
//        response.status shouldBe Status.Ok
//        response.contentType shouldBe Some("application/ld+json")
//      }
//    }
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
