package weddingplanner.server

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}

import cats.effect.IO
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.param.Stats
import com.twitter.server.TwitterServer
import io.finch.{Application, Bootstrap, Endpoint}
import lspace.codec.{jsonld, ActiveContext, ActiveProperty, JsonObjectInProgress, NativeTypeEncoder}
import lspace.encode.{EncodeJson, EncodeJsonLD, EncodeText}
import lspace.services.codecs.{Application => LApplication}
import lspace._
import lspace.Label.D._
import lspace.Label.P._
import lspace.ns._
import lspace.ns.vocab.schema
import lspace.services.LService
import lspace.services.rest.endpoints.LabeledNodeApi
import lspace.types.vector.Point
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.schedulers.CanBlock
import monix.reactive.Observable
import shapeless.{:+:, CNil, HNil}
import weddingplanner.endpoint._
import weddingplanner.ns.{AgeTest, Agenda, Appointment, KinsmanTest, PartnerTest, WeddingOfficiant}

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.concurrent.Await
import scala.util.Try

trait WeddingPlannerService extends LService {
  import lspace.codec.argonaut._
  implicit val ec: Scheduler = lspace.Implicits.Scheduler.global

  lazy val defaultContext = ActiveContext(
    `@prefix` = ListMap(
      "schema"           -> lspace.NS.vocab.schema.iri,
      "naam"             -> schema.name.iri,
      "voornaam"         -> schema.givenName.iri,
      "achternaam"       -> schema.familyName.iri,
      "tussenvoegsel"    -> weddingplanner.ns.prepositionName.iri,
      "geboortedatum"    -> schema.birthDate.iri,
      "geboorteplaats"   -> schema.birthPlace.iri,
      "omschrijving"     -> schema.description.iri,
      "adres"            -> schema.address.iri,
      "huisnummer"       -> weddingplanner.ns.houseNumber.iri,
      "huisletter"       -> weddingplanner.ns.houseLetter.iri,
      "Adres"            -> schema.PostalAddress.iri,
      "postcode"         -> schema.postalCode.iri,
      "straatAdres"      -> schema.streetAddress.iri,
      "Plaats"           -> schema.Place.iri,
      "emailadres"       -> schema.email.iri,
      "telefoonnummer"   -> schema.telephone.iri,
      "contactpunt"      -> schema.contactPoint.iri,
      "maxpersonen"      -> schema.maximumAttendeeCapacity.iri,
      "prijs"            -> schema.price.iri,
      "url"              -> schema.url.iri,
      "bsn"              -> weddingplanner.ns.ssn.iri,
      "straatnaam"       -> weddingplanner.ns.streetname.iri,
      "huisletter"       -> weddingplanner.ns.houseLetter.iri,
      "plaats"           -> schema.addressLocality.iri,
      "land"             -> schema.addressCountry.iri,
      "ondercuratele"    -> weddingplanner.ns.underLegalRestraint.iri,
      "foto"             -> schema.image.iri,
      "burgerlijkestand" -> weddingplanner.ns.civilStatus.iri,
      "nationaliteit"    -> schema.nationality.iri,
      "geboorteland"     -> weddingplanner.ns.birthCountry.iri,
      "start"            -> schema.startDate.iri,
      "end"              -> schema.endDate.iri,
//      "partnerIn"        -> weddingplanner.ns.spouses.iri,
      "Huwelijk" -> weddingplanner.ns.Marriage.iri,
      "Babs"     -> weddingplanner.ns.WeddingOfficiant.iri,
      "Agenda"   -> weddingplanner.ns.Agenda.iri,
      "afspraak" -> weddingplanner.ns.Agenda.keys.appointment.iri
    ),
    definitions = Map(
      schema.name.iri       -> ActiveProperty(`@type` = `@string` :: Nil, property = schema.name),
      schema.givenName.iri  -> ActiveProperty(`@type` = `@string` :: Nil, property = schema.givenName),
      schema.familyName.iri -> ActiveProperty(`@type` = `@string` :: Nil, property = schema.familyName),
      weddingplanner.ns.prepositionName.iri -> ActiveProperty(`@type` = `@string` :: Nil,
                                                              property = weddingplanner.ns.prepositionName),
      schema.birthDate.iri    -> ActiveProperty(`@type` = `@date` :: Nil, property = schema.birthDate),
      schema.birthPlace.iri   -> ActiveProperty(`@type` = schema.Place :: Nil, property = schema.birthPlace),
      schema.description.iri  -> ActiveProperty(`@type` = `@string` :: Nil, property = schema.description),
      schema.email.iri        -> ActiveProperty(`@type` = `@string` :: Nil, property = schema.email),
      schema.telephone.iri    -> ActiveProperty(`@type` = `@string` :: Nil, property = schema.telephone),
      schema.contactPoint.iri -> ActiveProperty(`@type` = schema.ContactPoint :: Nil, property = schema.contactPoint),
      schema.maximumAttendeeCapacity.iri -> ActiveProperty(`@type` = `@int` :: Nil,
                                                           property = schema.maximumAttendeeCapacity),
      schema.price.iri          -> ActiveProperty(`@type` = `@double` :: Nil, property = schema.price),
      schema.url.iri            -> ActiveProperty(`@type` = `@string` :: Nil, property = schema.url),
      weddingplanner.ns.ssn.iri -> ActiveProperty(`@type` = `@string` :: Nil, property = weddingplanner.ns.ssn),
      schema.streetAddress.iri  -> ActiveProperty(`@type` = `@string` :: Nil, property = schema.streetAddress),
      schema.postalCode.iri     -> ActiveProperty(`@type` = `@string` :: Nil, property = schema.postalCode),
      weddingplanner.ns.streetname.iri -> ActiveProperty(`@type` = `@string` :: Nil,
                                                         property = weddingplanner.ns.streetname),
      weddingplanner.ns.houseLetter.iri -> ActiveProperty(`@type` = `@string` :: Nil,
                                                          property = weddingplanner.ns.houseLetter),
      weddingplanner.ns.houseNumber.iri -> ActiveProperty(`@type` = `@string` :: Nil,
                                                          property = weddingplanner.ns.houseNumber),
      schema.addressLocality.iri -> ActiveProperty(`@type` = schema.Place :: Nil, property = schema.addressLocality),
      schema.addressCountry.iri  -> ActiveProperty(`@type` = schema.Country :: Nil, property = schema.addressCountry),
      weddingplanner.ns.underLegalRestraint.iri -> ActiveProperty(`@type` = `@boolean` :: Nil,
                                                                  property = weddingplanner.ns.underLegalRestraint),
      schema.image.iri -> ActiveProperty(`@type` = `@string` :: Nil, property = schema.image),
      weddingplanner.ns.civilStatus.iri -> ActiveProperty(`@type` = `@string` :: Nil,
                                                          property = weddingplanner.ns.civilStatus),
      schema.nationality.iri -> ActiveProperty(`@type` = `@string` :: Nil, property = schema.nationality),
      weddingplanner.ns.birthCountry.iri -> ActiveProperty(`@type` = schema.Country :: Nil,
                                                           property = weddingplanner.ns.birthCountry),
      schema.startDate.iri -> ActiveProperty(`@type` = `@date` :: Nil, property = schema.startDate),
      schema.endDate.iri   -> ActiveProperty(`@type` = `@date` :: Nil, property = schema.endDate),
      "partnerIn" -> ActiveProperty(`@type` = schema.Person :: Nil,
                                    `@reverse` = true,
                                    property = weddingplanner.ns.spouses),
      weddingplanner.ns.Agenda.keys.appointment.iri -> ActiveProperty(`@type` = weddingplanner.ns.Appointment :: Nil,
                                                                      property =
                                                                        weddingplanner.ns.Agenda.keys.appointment)
    )
  )

  lazy val graph: Graph              = ServicesConfig.config.graph.toGraph
  lazy val agendaService             = LabeledNodeApi(graph, Agenda.ontology, defaultContext)
  lazy val personService             = LabeledNodeApi(graph, schema.Person, defaultContext)
  lazy val placeService              = LabeledNodeApi(graph, schema.Place, defaultContext)
  lazy val addressService            = LabeledNodeApi(graph, schema.PostalAddress, defaultContext)
  lazy val weddingOfficiantService   = LabeledNodeApi(graph, WeddingOfficiant, defaultContext)
  lazy val appointmentService        = LabeledNodeApi(graph, Appointment, defaultContext)
  lazy val weddingReservationService = WeddingReservationEndpoint(graph, defaultContext)
  lazy val reportToMarriageService   = ReportToMarriageEndpoint(graph, defaultContext)

  lazy val agetestService     = AgeTestEndpoint(graph, AgeTestEndpoint.activeContext)
  lazy val kinsmantestService = KinsmanTestEndpoint(graph, KinsmanTestEndpoint.activeContext)
  lazy val partnertestService = PartnerTestEndpoint(graph, PartnerTestEndpoint.activeContext)

  implicit val encoder: lspace.codec.jsonld.Encoder = lspace.codec.jsonld.Encoder(nativeEncoder)
  import lspace.services.codecs.Encode._
  import EncodeJson._
  import EncodeJsonLD._

  object UtilsApi extends Endpoint.Module[IO] {
    import io.finch._

    def reset(): Task[Unit] =
      for {
//      SampleData.loadSample(graph).forkAndForget.runToFuture(monix.execution.Scheduler.global)
        _ <- { //Babsdataset
          import com.github.tototoshi.csv._
          import scala.io.Source

          val csvIri =
            "https://raw.githubusercontent.com/VNG-Realisatie/convenant-gemeenten/master/Documents/Testdata/Babsdataset_Def.csv"
          val source = Source.fromURL(csvIri)

          implicit object MyFormat extends DefaultCSVFormat {
            override val delimiter = ','
          }
          val reader = CSVReader.open(source)

          val data = reader.allWithHeaders

          val formatter = DateTimeFormatter.ofPattern("MM/dd/YYYY")
          Observable
            .fromIterable(data)
            .map(_.filter(_._2.nonEmpty))
            .mapEval { record =>
              for {
                weddingofficiant <- graph.nodes.create(WeddingOfficiant)
                _ <- (for {
                  postalCode  <- record.get("postcode").filter(_.nonEmpty)
                  houseNumber <- record.get("huisnummer").filter(_.nonEmpty)
                } yield {
                  for {
                    address <- graph.nodes.upsert(
                      s"${graph.iri}/postaladdress/${postalCode.toLowerCase()}_${houseNumber}",
                      schema.PostalAddress)
                    _ <- Task.gather {
                      Seq(
                        address --- schema.postalCode --> postalCode,
                        address --- weddingplanner.ns.houseNumber --> houseNumber
                      ) ++ Seq(
                        record.get("woonplaats").map(address --- schema.addressLocality --> _),
                        record.get("straat").map(address --- weddingplanner.ns.streetname --> _),
                        record
                          .get("straat")
                          .map(street => address --- schema.streetAddress --> (street + " " + houseNumber))
                      ).flatten
                    }
                    _ <- weddingofficiant --- schema.address --> address
                  } yield address
                }).getOrElse(Task.unit)
                _ <- Task.gather {
                  Seq(
                    record.get("voornaam").map(v => weddingofficiant --- schema.givenName --> v),
                    record.get("achternaam").map(v => weddingofficiant --- schema.familyName --> v),
                    record
                      .get("tussenvoegsel")
                      .map(v => weddingofficiant --- weddingplanner.ns.prepositionName --> v),
                    record
                      .get("geboortedatum")
                      .flatMap(v => Try(java.time.LocalDate.parse(v, formatter)).toOption)
                      .map(v => weddingofficiant --- schema.birthDate --> v),
                    record.get("foto").map(v => weddingofficiant --- schema.image --> v),
                    record.get("omschrijving").map(v => weddingofficiant --- schema.description --> v)
                  ).flatten
                }
                _ <- for {
                  contactPointTask <- Task.now(graph.nodes.create(schema.ContactPoint).memoizeOnSuccess)
                  email = record
                    .get("emailadres")
                    .filter(_.nonEmpty)
                  _ <- email
                    .map { v =>
                      for {
                        contactPoint <- contactPointTask
                        _            <- contactPoint --- schema.email --> v
                      } yield contactPoint
                    }
                    .getOrElse(Task.unit)
                  telephone = record
                    .get("telefoonnummer")
                    .filter(_.nonEmpty)
                  _ <- telephone
                    .map { v =>
                      for {
                        contactPoint <- contactPointTask
                        _            <- contactPoint --- schema.telephone --> v
                      } yield contactPoint
                    }
                    .getOrElse(Task.unit)
                  _ <- if (email.isDefined || telephone.isDefined)
                    contactPointTask.flatMap(weddingofficiant --- schema.contactPoint --> _)
                  else Task.unit
                } yield ()
              } yield weddingofficiant
            }
            .onErrorHandle { f =>
              scribe.error(f.getMessage); throw f
            }
            .completedL
        }
        _ <- { //locatiedataset
          import com.github.tototoshi.csv._
          import scala.io.Source

          val csvIri =
            "https://raw.githubusercontent.com/VNG-Realisatie/convenant-gemeenten/master/Documents/Testdata/Locatiedataset_Def.csv"
          val source = Source.fromURL(csvIri)

          implicit object MyFormat extends DefaultCSVFormat {
            override val delimiter = ','
          }
          val reader = CSVReader.open(source)

          val data = reader.allWithHeaders

          Observable
            .fromIterable(data)
            .map(_.filter(_._2.nonEmpty))
            .mapEval { record =>
              for {
                place <- graph.nodes.create(schema.Place)
                _     <- place --- `@id` --> s"${graph.iri}/place/${place.id}"
                _ <- (for {
                  postalCode  <- record.get("postcode").filter(_.nonEmpty)
                  houseNumber <- record.get("huisnummer").filter(_.nonEmpty)
                } yield {
                  for {
                    address <- graph.nodes.upsert(
                      s"${graph.iri}/postaladdress/${postalCode.toLowerCase()}_${houseNumber}",
                      schema.PostalAddress)
                    _ <- Task.gather {
                      Seq(
                        address --- schema.postalCode --> postalCode,
                        address --- weddingplanner.ns.houseNumber --> houseNumber
                      ) ++ Seq(
                        record.get("plaats").map(address --- schema.addressLocality --> _),
                        record.get("straat").map(address --- weddingplanner.ns.streetname --> _),
                        record
                          .get("straat")
                          .map(street => address --- schema.streetAddress --> (street + " " + houseNumber))
                      ).flatten
                    }
                    _ <- place --- schema.address --> address
                  } yield address
                }).getOrElse(Task.unit)
                _ <- Task.gather {
                  Seq(
                    record.get("locatie").map(v => place --- schema.name --> v),
                    record.get("website").map(v => place --- schema.url --> v),
                    record.get("foto").map(v => place --- schema.image --> v),
                    record.get("omschrijving").map(v => place --- schema.description --> v),
                    record.get("maxpersonen").map(v => place --- schema.maximumAttendeeCapacity --> v),
                    record.get("prijs").map(v => place --- schema.price --> v) //schema.UnitPriceSpecification ???
                  ).flatten
                }
                _ <- for {
                  contactPointTask <- Task.now(graph.nodes.create(schema.ContactPoint).memoizeOnSuccess)
                  email = record
                    .get("emailadres")
                    .filter(_.nonEmpty)
                  _ <- email
                    .map { v =>
                      for {
                        contactPoint <- contactPointTask
                        _            <- contactPoint --- schema.email --> v
                      } yield contactPoint
                    }
                    .getOrElse(Task.unit)
                  telephone = record
                    .get("telefoonnummer")
                    .filter(_.nonEmpty)
                  _ <- telephone
                    .map { v =>
                      for {
                        contactPoint <- contactPointTask
                        _            <- contactPoint --- schema.telephone --> v
                      } yield contactPoint
                    }
                    .getOrElse(Task.unit)
                  _ <- if (email.isDefined || telephone.isDefined)
                    contactPointTask.flatMap(place --- schema.contactPoint --> _)
                  else Task.unit
                } yield ()
              } yield place
            }
            .onErrorHandle { f =>
              scribe.error(f.getMessage); throw f
            }
            .completedL
        }
        _ <- { //partnerdataset
          import com.github.tototoshi.csv._
          import scala.io.Source

          val csvIri =
            "https://raw.githubusercontent.com/VNG-Realisatie/convenant-gemeenten/master/Documents/Testdata/Partnerdataset_Def.csv"
          val source = Source.fromURL(csvIri)

          implicit object MyFormat extends DefaultCSVFormat {
            override val delimiter = ','
          }
          val reader = CSVReader.open(source)

          val data = reader.allWithHeaders

          val formatter = DateTimeFormatter.ofPattern("MM/dd/YYYY")

          Observable
            .fromIterable(data)
            .map(_.filter(_._2.nonEmpty))
            .mapEval { record =>
              val ssn = record.get("bsn")
              for {
                person <- graph.nodes.upsert(s"${graph.iri}/person/nl_${ssn.get}", schema.Person)
                _ <- (for {
                  postalCode  <- record.get("postcode").filter(_.nonEmpty)
                  houseNumber <- record.get("huisnummer").filter(_.nonEmpty)
                  houseLetter <- record.get("huisletter").filter(_.nonEmpty)
                } yield {
                  for {
                    address <- graph.nodes.upsert(
                      s"${graph.iri}/postaladdress/${postalCode.toLowerCase()}_${houseNumber}_${houseLetter.toLowerCase()}",
                      schema.PostalAddress)
                    _ <- Task.gather {
                      Seq(
                        address --- schema.postalCode --> postalCode,
                        address --- weddingplanner.ns.houseNumber --> houseNumber,
                        address --- weddingplanner.ns.houseLetter --> houseLetter
                      ) ++ Seq(
                        record.get("woonplaats").map(address --- schema.addressLocality --> _),
                        record.get("straat").map(address --- weddingplanner.ns.streetname --> _),
                        record
                          .get("straat")
                          .map(street => address --- schema.streetAddress --> (street + " " + houseNumber)),
                        record.get("land").map { country =>
                          graph.nodes.upsert(s"${graph.iri}/country/${country}").flatMap { country =>
                            address --- schema.addressCountry --> country
                          }
                        }
                      ).flatten
                    }
                    _ <- person --- schema.address --> address
                  } yield address
                }).getOrElse(Task.unit)
                ssnPartner = record
                  .get("partner_bsn")
                _ <- ssnPartner
                  .map { v =>
                    for {
                      partner <- graph.nodes.upsert(s"${graph.iri}/person/nl_$v", schema.Person)
                      _       <- partner --- weddingplanner.ns.ssn --> v
                    } yield partner
                  }
                  .map { partnerTask =>
                    partnerTask.flatMap { partner =>
                      val start = record
                        .get("datum_verbinding")
                        .flatMap(v => Try(java.time.LocalDate.parse(v, formatter)).toOption)
                      val end = record
                        .get("datum_ontbinding")
                        .flatMap(v => Try(java.time.LocalDate.parse(v, formatter)).toOption)
                      val spousesRefs = List(s"nl_${ssn.get}", s"nl_${ssnPartner.get}")
                      for {
                        marriage <- graph.nodes.upsert(s"${graph.iri}/marriage/${spousesRefs.sorted
                          .mkString("+")}+${start.map(_.toString).getOrElse("")}", weddingplanner.ns.Marriage)
                        _ <- Task.gather(Seq(
                          if (marriage.out(schema.startDate).exists(start.contains(_))) None
                          else
                            start
                              .map(marriage --- schema.startDate --> _),
                          if (marriage.out(schema.endDate).exists(end.contains(_))) None
                          else
                            end
                              .map(marriage --- schema.endDate --> _)
                        ).flatten)
                        _ <- if (marriage.out(weddingplanner.ns.spouses).contains(person)) Task.unit
                        else marriage --- weddingplanner.ns.spouses --> person
                        _ <- if (marriage.out(weddingplanner.ns.spouses).contains(partner)) Task.unit
                        else marriage --- weddingplanner.ns.spouses --> partner
                      } yield marriage
                    }
                  }
                  .getOrElse(Task.unit) //add meta-data 'start/end'
                _ <- Task.gather {
                  Seq(
                    record.get("voornaam").map(v => person --- schema.givenName --> v),
                    record.get("achternaam").map(v => person --- schema.familyName --> v),
                    ssn.map(v => person --- weddingplanner.ns.ssn --> v),
//                  record
//                    .get("burgerlijkestand")
//                    .map(v => node --- weddingplanner.ns.civilStatus --> v),
                    record
                      .get("tussenvoegsel")
                      .map(v => person --- weddingplanner.ns.prepositionName --> v),
                    record
                      .get("geboortedatum")
                      .flatMap(v => Try(java.time.LocalDate.parse(v, formatter)).toOption)
                      .map(v => person --- schema.birthDate --> v),
                    record
                      .get("geboorteplaats")
                      .map(v => person --- schema.birthPlace --> v), //TODO: resolve to real place
                    record
                      .get("geboorteland")
                      .map(v => person --- weddingplanner.ns.birthCountry --> v), //TODO: redundant if birthPlace is linked data object
                    record.get("nationaliteit").map(v => person --- schema.nationality --> v),
                    record.get("foto").map(v => person --- schema.image --> v),
                    record.get("omschrijving").map(v => person --- schema.description --> v),
                    record.get("ondercuratele").map(v => person --- weddingplanner.ns.underLegalRestraint --> v)
                  ).flatten
                }
                _ <- for {
                  contactPointTask <- Task.now(graph.nodes.create(schema.ContactPoint).memoizeOnSuccess)
                  email = record
                    .get("emailadres")
                    .filter(_.nonEmpty)
                  _ <- email
                    .map { v =>
                      for {
                        contactPoint <- contactPointTask
                        _            <- contactPoint --- schema.email --> v
                      } yield contactPoint
                    }
                    .getOrElse(Task.unit)
                  telephone = record
                    .get("telefoonnummer")
                    .filter(_.nonEmpty)
                  _ <- telephone
                    .map { v =>
                      for {
                        contactPoint <- contactPointTask
                        _            <- contactPoint --- schema.telephone --> v
                      } yield contactPoint
                    }
                    .getOrElse(Task.unit)
                  _ <- if (email.isDefined || telephone.isDefined)
                    contactPointTask.flatMap(person --- schema.contactPoint --> _)
                  else Task.unit
                } yield ()
              } yield person
            }
            .onErrorHandle { f =>
              scribe.error(f.getMessage); throw f
            }
            .completedL
        }
      } yield ()

    val resetGraphs: Endpoint[IO, String] = get(path("reset")) {
      (for {
        _ <- purge()
        _ <- reset()
      } yield ()).runToFuture(monix.execution.Scheduler.global)

      Ok("resetting now, building graphs...")
    }

    def purge() = graph.purge
    val clearGraphs: Endpoint[IO, String] = get(path("clear")) {
      purge.forkAndForget
        .runToFuture(monix.execution.Scheduler.global)
      Ok("clearing now")
    }

    val sigmajs: Endpoint[IO, String] = get(path("sigmajs")).mapOutputAsync { hn =>
      (for {
        data <- SigmaJsVisualizer.visualizeGraph(graph)
      } yield Ok(data)).toIO
    }

    val persist: Endpoint[IO, Unit] = get("_persist") {
      scribe.info("persisting all graphs")
      graph.persist
      io.finch.NoContent[Unit]
    }
  }

//  SampleData.loadSample(graph).runSyncUnsafe()(monix.execution.Scheduler.global, CanBlock.permit)
  UtilsApi.reset.runToFuture
//  println(SigmaJsVisualizer.visualizeGraph(graph))

  lazy val service: Service[Request, Response] = {

    import lspace.services.codecs.Encode._
    implicit val encoder       = jsonld.Encoder.apply(nativeEncoder)
    implicit val activeContext = ActiveContext()

    import EncodeText._
    import EncodeJson._
    import EncodeJsonLD._

    Bootstrap
      .configure(enableMethodNotAllowed = true, enableUnsupportedMediaType = true)
      .serve[LApplication.JsonLD :+: Application.Json :+: CNil](agendaService.labeledApi)
      .serve[LApplication.JsonLD :+: Application.Json :+: CNil](appointmentService.labeledApi)
      .serve[LApplication.JsonLD :+: Application.Json :+: CNil](personService.labeledApi)
      .serve[LApplication.JsonLD :+: Application.Json :+: CNil](weddingOfficiantService.labeledApi)
      .serve[LApplication.JsonLD :+: Application.Json :+: CNil](placeService.labeledApi)
      .serve[LApplication.JsonLD :+: Application.Json :+: CNil](addressService.labeledApi)
      .serve[LApplication.JsonLD :+: Application.Json :+: CNil](agetestService.api)
      .serve[LApplication.JsonLD :+: Application.Json :+: CNil](kinsmantestService.api)
      .serve[LApplication.JsonLD :+: Application.Json :+: CNil](partnertestService.api)
      .serve[LApplication.JsonLD :+: Application.Json :+: CNil](reportToMarriageService.api)
      .serve[LApplication.JsonLD :+: Application.Json :+: CNil](weddingReservationService.api)
      .serve[LApplication.JsonLD :+: Application.Json :+: CNil](
        UtilsApi.clearGraphs :+: UtilsApi.resetGraphs :+: UtilsApi.sigmajs :+: UtilsApi.persist)
      .toService
  }
}

object WeddingPlannerService extends WeddingPlannerService with TwitterServer {

  def main(): Unit = {
    val server = Http.server
//      .configured(Stats(statsReceiver))
      .serve(
        s":${ServicesConfig.config.port.value}",
        service
      )

    import scala.concurrent.duration._
    onExit {
      println(s"close wedding-planner-server")
      Await.ready(
        Task
          .sequence(
            Seq(
              Task.gatherUnordered(Seq(
                graph.persist
              )),
              Task.gatherUnordered(Seq(
                graph.close
              ))
            ))
          .runToFuture(monix.execution.Scheduler.global),
        20 seconds
      )

      server.close()
    }

    com.twitter.util.Await.ready(adminHttpServer)
  }
}
