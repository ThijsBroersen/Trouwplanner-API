package weddingplanner.ns

import java.time.Instant

import lspace.librarian.structure.OntologyDef
import lspace.librarian.structure.Property
import lspace.librarian.structure.PropertyDef

object Place
    extends OntologyDef(
      "https://schema.org/Place",
      label = "Place",
      comment = "Entities that have a somewhat fixed, physical extension.",
      `@extends` = () => Generic.Thing.ontology :: Nil
    ) {

  object keys extends Generic.Thing.Properties {
    object review
        extends PropertyDef(
          "https://schema.org/review",
          label = "review",
          comment = "A review of the item.",
          `@range` = () => Nil) //@range should be https://schema.org/Review
    object agenda
        extends PropertyDef(ontology.iri + "/agenda",
                            label = "agenda",
                            `@range` = () => Agenda.ontology :: Nil)
  }
  override lazy val properties
    : List[Property] = keys.review.property :: keys.review.property :: Generic.Thing.properties
  trait Properties extends Generic.Thing.Properties {
    val review = keys.review
    val agenda = keys.agenda
  }

}

case class Place(id: Option[Int],
                 name: String,
                 description: String,
                 agenda: Agenda)
