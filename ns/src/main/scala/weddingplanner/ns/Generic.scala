package weddingplanner.ns

import lspace.NS.vocab
import lspace.librarian.datatype._
import lspace.librarian.structure.{OntologyDef, Property, PropertyDef}

object Generic {

  object Thing
      extends OntologyDef("https://schema.org/Thing", label = "Thing", comment = "The most generic type of item.") {
    object keys {
      object id
          extends PropertyDef(
            vocab.schema + "identifier",
            label = "identifier",
            comment =
              "The identifier property represents any kind of identifier for any kind of Thing, " +
                "such as ISBNs, GTIN codes, UUIDs etc. " +
                "Schema.org provides dedicated properties for representing many of these, " +
                "either as textual strings or as URL (URI) links. See background notes for more details.",
            `@range` = () => TextType.datatype :: IriType.datatype :: Nil //why not LongType first?
          )
      object name
          extends PropertyDef(vocab.schema + "name",
                              label = "name",
                              comment = "The name of the item.",
                              `@range` = () => TextType.datatype :: Nil)
      object description
          extends PropertyDef(vocab.schema + "description",
                              label = "description",
                              comment = "A description of the item.",
                              `@range` = () => TextType.datatype :: Nil)
    }

    override lazy val properties
      : List[Property] = keys.id.property :: keys.name.property :: keys.description.property :: Nil
    trait Properties {
      val id          = keys.id
      val name        = keys.name.property
      val description = keys.description.property
    }
  }
  object CreativeWork
      extends OntologyDef(
        vocab.schema + "CreativeWork",
        label = "CreativeWork",
        comment =
          "The most generic kind of creative work, including books, movies, photographs, software programs, etc.",
        `@extends` = () => Thing.ontology :: Nil
      ) {
    object keys extends Thing.Properties {
      object dateCreated
          extends PropertyDef(
            vocab.schema + "dateCreated",
            label = "dateCreated",
            comment = "The date on which the CreativeWork was created or the item was added to a DataFeed.",
            `@range` = () => DateTimeType.datatype :: LocalDateType.datatype :: Nil
          )
      object dateModified
          extends PropertyDef(
            vocab.schema + "dateModified",
            label = "dateModified",
            comment = "The date on which the CreativeWork was created or the item was added to a DataFeed.",
            `@range` = () => DateTimeType.datatype :: LocalDateType.datatype :: Nil
          )
    }
    override lazy val properties
      : List[Property] = keys.dateCreated.property :: keys.dateModified.property :: Thing.properties
    trait Properties extends Thing.Properties {
      val dateCreated  = keys.dateCreated
      val dateModified = keys.dateModified
    }
  }
}
