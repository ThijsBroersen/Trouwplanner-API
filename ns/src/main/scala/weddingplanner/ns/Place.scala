package weddingplanner.ns

import lspace.NS.vocab
import lspace.librarian.datatype.{GeopointType, TextType}
import lspace.librarian.structure.OntologyDef
import lspace.librarian.structure.Property
import lspace.librarian.structure.PropertyDef
import lspace.types.vector.Point

object Place
    extends OntologyDef(
      "https://schema.org/Place",
      label = "Place",
      comment = "Entities that have a somewhat fixed, physical extension.",
      `@extends` = () => Generic.Thing.ontology :: Nil
    ) {

  object keys extends Generic.Thing.Properties {
    object review
        extends PropertyDef(vocab.schema + "review",
                            label = "review",
                            comment = "A review of the item.",
                            `@range` = () => Nil) //@range should be https://schema.org/Review
    object agenda
        extends PropertyDef(vocab.Lspace + "/agenda", label = "agenda", `@range` = () => Agenda.ontology :: Nil)
    object geo extends PropertyDef(vocab.schema + "/geo", label = "geo", `@range` = () => GeopointType.datatype :: Nil)
    object address
        extends PropertyDef(vocab.schema + "/address", label = "address", `@range` = () => TextType.datatype :: Nil)
  }
  override lazy val properties
    : List[Property] = keys.review.property :: keys.review.property :: keys.geo.property :: keys.address.property :: Generic.Thing.properties
  trait Properties extends Generic.Thing.Properties {
    lazy val review  = keys.review
    lazy val agenda  = keys.agenda
    lazy val geo     = keys.geo
    lazy val address = keys.address
  }

}

case class Place(id: Option[Int], name: String, description: String, address: String, geo: Point, agenda: Agenda)
