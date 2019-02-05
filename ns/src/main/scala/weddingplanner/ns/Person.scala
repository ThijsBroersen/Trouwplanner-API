package weddingplanner.ns

import java.time.Instant

import lspace.librarian.datatype.{IriType, TextType}
import lspace.librarian.structure.OntologyDef
import lspace.librarian.structure.Property
import lspace.librarian.structure.PropertyDef

object Person
    extends OntologyDef("https://schema.org/Person",
                        label = "Person",
                        comment =
                          "A person (alive, dead, undead, or fictional).",
                        `@extends` = () => Generic.Thing.ontology :: Nil) {

  object keys extends Generic.Thing.Properties {
    object honorificPrefix
        extends PropertyDef(
          "https://schema.org/honorificPrefix",
          label = "honorificPrefix",
          comment =
            "An honorific prefix preceding a Person's name such as Dr/Mrs/Mr.",
          `@range` = () => TextType.datatype :: Nil
        )
    object worksFor
        extends PropertyDef(
          "https://schema.org/worksFor",
          label = "worksFor",
          comment = "Organizations that the person works for.",
          `@range` = () => TextType.datatype :: Nil) //TODO: implement https://schema.org/Organization
  }
  override lazy val properties
    : List[Property] = keys.honorificPrefix.property :: keys.worksFor.property :: Generic.Thing.properties
  trait Properties extends Generic.Thing.Properties {
    val honorificPrefix = keys.honorificPrefix
    val worksFor = keys.worksFor
  }

}

case class Person(id: Option[Int],
                  name: String,
                  description: String,
                  honorificPrefix: String,
                  worksFor: String)
