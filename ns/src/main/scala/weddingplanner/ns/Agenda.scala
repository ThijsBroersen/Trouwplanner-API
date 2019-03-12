package weddingplanner.ns

import java.time.Instant

import lspace.ns.vocab
import lspace.client.User
import lspace.datatype.TextType
import lspace.structure.OntologyDef
import lspace.structure.Property
import lspace.structure.PropertyDef

object Agenda
    extends OntologyDef("https://ns.convenantgemeenten.nl/Agenda",
                        label = "Agenda",
                        comment = "An appointment diary",
                        `@extends` = () => vocab.schema.CreativeWork.ontology :: Nil) {

  object keys extends vocab.schema.CreativeWork.Properties {
    object appointment
        extends PropertyDef(ontology.iri + "/appointment",
                            label = "appointment",
                            `@range` = () => Appointment.ontology :: Nil)
  }
  override lazy val properties: List[Property] = keys.appointment.property :: vocab.schema.CreativeWork.properties
  trait Properties {
    lazy val appointment = keys.appointment
  }

}

case class Agenda(id: Option[Int],
                  name: String,
                  description: String,
                  owner: User,
                  appointment: Set[Appointment],
                  dateCreated: Instant,
                  dateModified: Option[Instant])
