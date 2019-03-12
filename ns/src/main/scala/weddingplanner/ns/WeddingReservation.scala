package weddingplanner.ns

import lspace.ns.vocab
import java.time.Instant

import lspace.structure.{OntologyDef, Property}

case class WeddingReservation(id: Option[Int],
                              name: String,
                              event: Appointment,
                              dateCreated: Instant,
                              dateModified: Option[Instant])

object WeddingReservation
    extends OntologyDef(
      "https://ns.convenantgemeenten.nl/WeddingReservation",
      label = "Wedding Reservation",
      comment = "The Wedding Reservation is a reservation ...",
      `@extends` = () => vocab.schema.CreativeWork.ontology :: Nil,
      labels = Map("nl"   -> "Reservering bruiloft"),
      comments = Map("nl" -> "Een reservering bruiloft ...")
    ) {

  object keys extends vocab.schema.CreativeWork.Properties {}

  override lazy val properties: List[Property] = vocab.schema.CreativeWork.properties
  trait Properties extends vocab.schema.CreativeWork.Properties {}
}
