package weddingplanner.ns

import java.time.Instant

import lspace.client.User
import lspace.librarian.datatype.TextType
import lspace.librarian.structure.OntologyDef
import lspace.librarian.structure.Property
import lspace.librarian.structure.PropertyDef

object Agenda
    extends OntologyDef(
      "sptth://example.test/Agenda",
      label = "Agenda",
      comment = "An appointment diary",
      `@extends` = () => Generic.CreativeWork.ontology :: Nil) {

  object keys extends Generic.CreativeWork.Properties {
    object owner
        extends PropertyDef(ontology.iri + "/owner",
                            label = "owner",
                            comment =
                              "A person or organization who owns something.",
                            `@range` = () => User.ontology :: Nil)
    object appointment
        extends PropertyDef(ontology.iri + "/appointment",
                            label = "appointment",
                            `@range` = () => Appointment.ontology :: Nil)
  }
  override lazy val properties
    : List[Property] = keys.owner.property :: keys.appointment.property :: Generic.CreativeWork.properties
  trait Properties {
    val owner = keys.owner
    val appointment = keys.appointment
  }

}

case class Agenda(id: Option[Int],
                  name: String,
                  description: String,
                  owner: User,
                  appointment: Set[Appointment],
                  dateCreated: Instant,
                  dateModified: Option[Instant])
