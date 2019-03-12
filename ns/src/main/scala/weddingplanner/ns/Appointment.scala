package weddingplanner.ns

import java.time.Instant

import lspace.NS
import lspace.ns.vocab
import lspace.datatype.DateTimeType
import lspace.structure.OntologyDef
import lspace.structure.Property
import lspace.structure.PropertyDef

object Appointment
    extends OntologyDef(
      "https://ns.convenantgemeenten.nl/Appointment",
      label = "Appointment",
      comment = "An arrangement to meet at a particular time and place.",
      `@extends` = () => vocab.schema.Event.ontology :: Nil
    ) {

  object keys extends vocab.schema.Event.Properties {
    lazy val agenda = weddingplanner.ns.agenda
  }
  override lazy val properties: List[Property] = vocab.schema.Event.properties
  trait Properties extends vocab.schema.Event.Properties {
    lazy val agenda = weddingplanner.ns.agenda
  }
}

case class Appointment(id: Option[Int],
                       name: String,
                       description: String,
                       startDate: Instant,
                       endDate: Instant,
                       dateCreated: Instant,
                       dateModified: Option[Instant])
