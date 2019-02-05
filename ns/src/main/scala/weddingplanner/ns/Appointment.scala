package weddingplanner.ns

import java.time.Instant

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
    object agenda
        extends PropertyDef(ontology.iri + "/agenda",
                            label = "agenda",
                            `@range` = () => Agenda.ontology :: Nil)
  }
  override lazy val properties
    : List[Property] = keys.agenda.property :: Generic.CreativeWork.properties
  trait Properties extends Generic.CreativeWork.Properties {
    val agenda = keys.agenda
  }
}

case class Appointment(id: Option[Int],
                       agenda: Agenda,
                       name: String,
                       dateCreated: Instant,
                       dateModified: Option[Instant])
