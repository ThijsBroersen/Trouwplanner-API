package weddingplanner

import lspace.structure.{Ontology, Property}

package object ns {

  val prepositionName     = Property.properties.getOrCreate("http://ns.convenantgemeenten.nl/prepositionName")
  val ssn                 = Property.properties.getOrCreate("http://ns.convenantgemeente.nl/ssn")
  val streetname          = Property.properties.getOrCreate("http://ns.convenantgemeenten.nl/streetname")
  val houseNumber         = Property.properties.getOrCreate("http://ns.convenantgemeenten.nl/houseNumber")
  val houseLetter         = Property.properties.getOrCreate("http://ns.convenantgemeenten.nl/houseLetter")
  val underLegalRestraint = Property.properties.getOrCreate("http://ns.convenantgemeenten.nl/underLegalRestraint")
  val civilStatus         = Property.properties.getOrCreate("http://ns.convenantgemeente.nl/civilStatus")
  val birthCountry        = Property.properties.getOrCreate("http://ns.convenantgemeenten.nl/birthCountry")
  val spouses             = Property.properties.getOrCreate("http://ns.convenantgemeenten.nl/Marriage/spouses")

  val Marriage       = Ontology.ontologies.getOrCreate("http://ns.convenantgemeenten.nl/Marriage")
  val LegalRestraint = Ontology.ontologies.getOrCreate("http://ns.convenantgemeenten.nl/LegalRestraint")
}
