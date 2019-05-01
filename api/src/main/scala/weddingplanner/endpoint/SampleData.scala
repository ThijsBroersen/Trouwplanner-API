package weddingplanner.endpoint

import java.time.{Instant, LocalDate}

import lspace.Label
import lspace.ns.vocab.schema
import lspace.structure.{Graph, Property}
import lspace.types.vector.Point
import monix.eval.Task
import weddingplanner.ns.Appointment
import weddingplanner.ns.Agenda

object SampleData {
  def loadSample(graph: Graph) =
    for {
      _ <- graph.purge
      _places <- for {
        _GemeentehuisHoorn <- for {
          _place       <- graph + schema.Place
          _id          <- _place --- Property.default.`@id` --> (graph.iri + "/place/123")
          _name        <- _place --- schema.name --> "Gemeentehuis Hoorn"
          _description <- _place --- schema.description --> "Gemeentehuis Hoorn .. omschrijving .."
          _geo         <- _place --- schema.geo --> Point(72.0403, 60.90879)
        } yield
          new {
            val place       = _place
            val id          = _id
            val name        = _name
            val description = _description
          }
        _GemeentehuisHeerenveen <- for {
          _place <- graph + schema.Place
          _id    <- _place --- Property.default.`@id` --> (graph.iri + "/place/12345")
          _name  <- _place --- schema.name --> "Gemeentehuis Heerenveen"
          _geo   <- _place --- schema.geo --> Point(-48.4046, 175.87173)
        } yield
          new {
            val place = _place
            val id    = _id
            val name  = _name
          }
        _GemeentehuisHaarlem <- for {
          _place <- graph + schema.Place
          _id    <- _place --- Property.default.`@id` --> (graph.iri + "/place/345")
          _name  <- _place --- schema.name --> "Gemeentehuis Haarlem"
          _geo   <- _place --- schema.geo --> Point(89.45136, 88.01204)
        } yield
          new {
            val place = _place
            val id    = _id
            val name  = _name
          }
        _GemeentehuisUtrecht <- for {
          _place <- graph + schema.Place
          _id    <- _place --- Property.default.`@id` --> (graph.iri + "/place/34567")
          _name  <- _place --- schema.name --> "Gemeentehuis Utrecht"
          _geo   <- _place --- schema.geo --> Point(74.32746, -45.06438)
        } yield
          new {
            val place = _place
            val id    = _id
            val name  = _name
          }
      } yield
        new {
          val GemeentehuisHoorn      = _GemeentehuisHoorn
          val GemeentehuisHeerenveen = _GemeentehuisHeerenveen
          val GemeentehuisHaarlem    = _GemeentehuisHaarlem
          val GemeentehuisUtrecht    = _GemeentehuisUtrecht
        }
      _addresses <- for {
        _gemeenteHoorn <- for {
          _address       <- graph + schema.PostalAddress
          _id            <- _address --- Label.P.`@id` --> (graph.iri + "/address/1")
          _postalCode    <- _address --- schema.postalCode --> "1625HV"
          _streetAddress <- _address --- schema.streetAddress --> "1"
        } yield
          new {
            val address       = _address
            val id            = _id
            val postalCode    = _postalCode
            val streetAddress = _streetAddress
          }
        _gemeenteHoorn2 <- for {
          _address       <- graph + schema.PostalAddress
          _id            <- _address --- Label.P.`@id` --> (graph.iri + "/address/2")
          _postalCode    <- _address --- schema.postalCode --> "1625HV"
          _streetAddress <- _address --- schema.streetAddress --> "2"
        } yield
          new {
            val address       = _address
            val id            = _id
            val postalCode    = _postalCode
            val streetAddress = _streetAddress
          }
        _gemeenteHaarlem <- for {
          _address       <- graph + schema.PostalAddress
          _id            <- _address --- Label.P.`@id` --> (graph.iri + "/address/3")
          _postalCode    <- _address --- schema.postalCode --> "2011VB"
          _streetAddress <- _address --- schema.streetAddress --> "39"
        } yield
          new {
            val address       = _address
            val id            = _id
            val postalCode    = _postalCode
            val streetAddress = _streetAddress
          }
        _gemeenteHeerenveen <- for {
          _address       <- graph + schema.PostalAddress
          _id            <- _address --- Label.P.`@id` --> (graph.iri + "/address/4")
          _postalCode    <- _address --- schema.postalCode --> "8441ES"
          _streetAddress <- _address --- schema.streetAddress --> "2"
        } yield
          new {
            val address       = _address
            val id            = _id
            val postalCode    = _postalCode
            val streetAddress = _streetAddress
          }
      } yield
        new {
          val gemeenteHoorn      = _gemeenteHoorn
          val gemeenteHoorn2     = _gemeenteHoorn2
          val gemeenteHaarlem    = _gemeenteHaarlem
          val gemeenteHeerenveen = _gemeenteHeerenveen
        }
      _persons <- for {
        _Thijs <- for {
          _person     <- graph + schema.Person
          _id         <- _person --- Property.default.`@id` --> (graph.iri + "/person/123")
          _name       <- _person --- schema.name --> "Thijs" //relation can be a string
          _birthdate  <- _person --- schema.birthDate --> LocalDate.parse("1996-08-18")
          _birthPlace <- _person --- schema.birthPlace --> _places.GemeentehuisHeerenveen.place
          _address    <- _person --- schema.address --> _addresses.gemeenteHoorn.address
        } yield
          new {
            val person     = _person
            val id         = _id
            val name       = _name
            val birthdate  = _birthdate
            val birthPlace = _birthPlace
            val address    = _address
          }
        _Bas <- for {
          _person     <- graph + schema.Person
          _id         <- _person --- Property.default.`@id` --> (graph.iri + "/person/12345")
          _name       <- _person --- schema.name --> "Bas" //relation can be a Property-object
          _birthdate  <- _person --- schema.birthDate --> LocalDate.parse("2008-12-20")
          _birthPlace <- _person --- schema.birthPlace --> _places.GemeentehuisHeerenveen.place
          _address    <- _person --- schema.address --> _addresses.gemeenteHoorn2.address
        } yield
          new {
            val person     = _person
            val id         = _id
            val name       = _name
            val birthdate  = _birthdate
            val birthPlace = _birthPlace
            val address    = _address
          }
        _Anna <- for {
          _person     <- graph + schema.Person
          _id         <- _person --- Property.default.`@id` --> (graph.iri + "/person/345")
          _name       <- _person --- schema.name --> "Anna"
          _birthdate  <- _person --- schema.birthDate --> LocalDate.parse("1997-04-10")
          _birthPlace <- _person --- schema.birthPlace --> _places.GemeentehuisHaarlem.place
          _address    <- _person --- schema.address --> _addresses.gemeenteHeerenveen.address
        } yield
          new {
            val person     = _person
            val id         = _id
            val name       = _name
            val birthdate  = _birthdate
            val birthPlace = _birthPlace
            val address    = _address
          }
        _Stefan <- for {
          _person     <- graph + schema.Person
          _id         <- _person --- Property.default.`@id` --> (graph.iri + "/person/34567")
          _name       <- _person --- schema.name --> "Stefan"
          _birthdate  <- _person --- schema.birthDate --> LocalDate.parse("2008-11-30")
          _birthPlace <- _person --- schema.birthPlace --> _places.GemeentehuisHoorn.place
          _address    <- _person --- schema.address --> _addresses.gemeenteHaarlem.address
        } yield
          new {
            val person     = _person
            val id         = _id
            val name       = _name
            val birthdate  = _birthdate
            val birthPlace = _birthPlace
            val address    = _address
          }
        _Kim <- for {
          _person     <- graph + schema.Person
          _id         <- _person --- Property.default.`@id` --> (graph.iri + "/person/567")
          _name       <- _person --- schema.name --> "Kim"
          _birthdate  <- _person --- schema.birthDate --> LocalDate.parse("2002-06-13")
          _birthPlace <- _person --- schema.birthPlace --> _places.GemeentehuisHoorn.place
          _address    <- _person --- schema.address --> _addresses.gemeenteHaarlem.address
        } yield
          new {
            val person     = _person
            val id         = _id
            val name       = _name
            val birthdate  = _birthdate
            val birthPlace = _birthPlace
            val address    = _address
          }
        _Rik <- for {
          _person     <- graph + schema.Person
          _id         <- _person --- Property.default.`@id` --> (graph.iri + "/person/56789")
          _name       <- _person --- schema.name --> "Rik"
          _birthdate  <- _person --- schema.birthDate --> LocalDate.parse("1994-06-18")
          _birthPlace <- _person --- schema.birthPlace --> _places.GemeentehuisUtrecht.place
        } yield
          new {
            val person     = _person
            val id         = _id
            val name       = _name
            val birthdate  = _birthdate
            val birthPlace = _birthPlace
          }
      } yield
        new {
          val Thijs  = _Thijs
          val Bas    = _Bas
          val Anna   = _Anna
          val Stefan = _Stefan
          val Kim    = _Kim
          val Rik    = _Rik
        }

      _agendas <- for {

        _a1 <- for {
          _agenda <- graph + Agenda
          _id     <- _agenda --- Label.P.`@id` --> (graph.iri + "/agenda/1")
          _name   <- _agenda --- schema.name --> "Agenda Thijs"
        } yield
          new {
            val agenda = _agenda
            val id     = _id
            val name   = _name
          }
        _a2 <- for {
          _agenda <- graph + Agenda
          _id     <- _agenda --- Label.P.`@id` --> (graph.iri + "/agenda/2")
          _name   <- _agenda --- schema.name --> "Agenda Anna"
        } yield
          new {
            val agenda = _agenda
            val id     = _id
            val name   = _name
          }
        _a3 <- for {
          _agenda <- graph + Agenda
          _id     <- _agenda --- Label.P.`@id` --> (graph.iri + "/agenda/3")
          _name   <- _agenda --- schema.name --> "Agenda Bas"
        } yield
          new {
            val agenda = _agenda
            val id     = _id
            val name   = _name
          }
      } yield
        new {
          val a1 = _a1
          val a2 = _a2
          val a3 = _a3
        }
      _appointments <- for {
        _ap1 <- for {
          _appointment <- graph + Appointment
          _id          <- _appointment --- Label.P.`@id` --> (graph.iri + "/appointment/1")
          _agenda      <- _appointment --- schema.isPartOf --> _agendas.a1.agenda
          _startDate   <- _appointment --- schema.startDate --> Instant.parse("2019-04-03T10:15:00.00Z")
        } yield
          new {
            val appointment = _appointment
            val id          = _id
            val agenda      = _agenda
            val startDate   = _startDate
          }
        _ap2 <- for {
          _appointment <- graph + Appointment
          _id          <- _appointment --- Label.P.`@id` --> (graph.iri + "/appointment/1")
          _agenda      <- _appointment --- schema.isPartOf --> _agendas.a2.agenda
          _startDate   <- _appointment --- schema.startDate --> Instant.parse("2019-05-15T14:00:00.00Z")
        } yield
          new {
            val appointment = _appointment
            val id          = _id
            val agenda      = _agenda
            val startDate   = _startDate
          }
      } yield
        new {
          val ap1 = _ap1
          val ap2 = _ap2
        }

      _ <- _persons.Rik.person --- schema.owns --> _agendas.a1.agenda
      _ <- _persons.Thijs.person --- schema.owns --> _agendas.a1.agenda
      _ <- _persons.Bas.person --- schema.owns --> _agendas.a1.agenda
    } yield
      new {
        val places       = _places
        val persons      = _persons
        val agendas      = _agendas
        val appointments = _appointments
      }
}
