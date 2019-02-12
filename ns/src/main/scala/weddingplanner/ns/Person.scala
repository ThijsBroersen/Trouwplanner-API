package weddingplanner.ns

import lspace.NS.vocab

import lspace.librarian.datatype.{IriType, TextType}
import lspace.librarian.structure.OntologyDef
import lspace.librarian.structure.Property
import lspace.librarian.structure.PropertyDef

object Person
    extends OntologyDef("https://schema.org/Person",
                        label = "Person",
                        comment = "A person (alive, dead, undead, or fictional).",
                        `@extends` = () => Generic.Thing.ontology :: Nil) {

  object keys extends Generic.Thing.Properties {
    object honorificPrefix
        extends PropertyDef(
          "https://schema.org/honorificPrefix",
          label = "honorificPrefix",
          comment = "An honorific prefix preceding a Person's name such as Dr/Mrs/Mr.",
          `@range` = () => TextType.datatype :: Nil
        )
    object worksFor
        extends PropertyDef("https://schema.org/worksFor",
                            label = "worksFor",
                            comment = "Organizations that the person works for.",
                            `@range` = () => TextType.datatype :: Nil) //TODO: implement https://schema.org/Organization
    object agenda
        extends PropertyDef(vocab.Lspace + "/agenda", label = "agenda", `@range` = () => Agenda.ontology :: Nil)
  }
  override lazy val properties
    : List[Property] = keys.honorificPrefix.property :: keys.worksFor.property :: keys.agenda.property :: Generic.Thing.properties
  trait Properties extends Generic.Thing.Properties {
    lazy val honorificPrefix = keys.honorificPrefix
    lazy val worksFor        = keys.worksFor
    lazy val agenda          = keys.agenda
  }

}

case class Person(id: Option[Int],
                  name: String,
                  description: String,
                  honorificPrefix: String,
                  worksFor: String,
                  agenda: Agenda)
