package weddingplanner.ns

import lspace.ns.vocab
import lspace.structure.{OntologyDef, Property}

object WeddingOfficiant
    extends OntologyDef(
      "https://ns.convenantgemeenten.nl/WeddingOfficiant",
      label = "Wedding Officiant",
      comment = "A wedding officiant ...",
      `@extends` = () => vocab.schema.Person.ontology :: Nil
    ) {

  object keys extends vocab.schema.Person.Properties
  override lazy val properties: List[Property] = vocab.schema.Person.properties
  trait Properties extends vocab.schema.Person.Properties

}
