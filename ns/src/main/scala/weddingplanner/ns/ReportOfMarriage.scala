package weddingplanner.ns

import java.time.Instant

import lspace.ns.vocab
import lspace.structure.{OntologyDef, Property}

object ReportOfMarriage
    extends OntologyDef(
      "https://ns.convenantgemeenten.nl/ReportOfMarriage",
      label = "Report Of Marriage",
      comment =
        "The Report of Marriage is a report in which the expected wedding couple give note of their upcoming union.",
      `@extends` = () => vocab.schema.CreativeWork.ontology :: Nil,
      labels = Map("nl"   -> "Melding voorgenomen huwelijk"),
      comments = Map("nl" -> "Een melding voorgenomen huwelijk ...")
    ) {

  object keys extends vocab.schema.CreativeWork.Properties {}

  override lazy val properties: List[Property] = vocab.schema.CreativeWork.properties
  trait Properties extends vocab.schema.CreativeWork.Properties {}
}

case class ReportOfMarriage(id: Option[Int],
                            name: String,
                            description: String,
                            dateCreated: Instant,
                            dateModified: Option[Instant])
