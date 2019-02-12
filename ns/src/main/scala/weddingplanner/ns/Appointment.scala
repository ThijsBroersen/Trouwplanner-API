package weddingplanner.ns

import java.time.Instant

import lspace.NS.vocab
import lspace.librarian.datatype.DateTimeType
import lspace.librarian.structure.OntologyDef
import lspace.librarian.structure.Property
import lspace.librarian.structure.PropertyDef

object Appointment
    extends OntologyDef(
      "sptth://example.test/Appointment",
      label = "Appointment",
      comment = "An arrangement to meet at a particular time and place.",
      `@extends` = () => Generic.CreativeWork.ontology :: Nil
    ) {

  object keys extends Generic.CreativeWork.Properties {
    object startDate
        extends PropertyDef(
          vocab.schema + "startDate",
          label = "startDate",
          comment = "The start date and time of the item (in ISO 8601 date format).",
          `@range` = () => DateTimeType.datatype :: Nil
        )
    object endDate
        extends PropertyDef(
          vocab.schema + "endDate",
          label = "endDate",
          comment = "The end date and time of the item (in ISO 8601 date format).",
          `@range` = () => DateTimeType.datatype :: Nil
        )
  }
  override lazy val properties
    : List[Property] = keys.startDate.property :: keys.endDate.property :: Generic.CreativeWork.properties
  trait Properties extends Generic.CreativeWork.Properties {
    lazy val startDate = keys.startDate
    lazy val endDate   = keys.endDate
  }
}

case class Appointment(id: Option[Int],
                       name: String,
                       description: String,
                       startDate: Instant,
                       endDate: Instant,
                       dateCreated: Instant,
                       dateModified: Option[Instant])
