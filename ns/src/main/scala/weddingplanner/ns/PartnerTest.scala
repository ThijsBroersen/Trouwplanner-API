package weddingplanner.ns

import lspace.Label
import lspace.ns.vocab.schema
import lspace.provider.detached.DetachedGraph
import lspace.structure.{Node, OntologyDef, Property, PropertyDef, TypedProperty}
import monix.eval.Task

object PartnerTest
    extends OntologyDef(
      "https://ns.convenantgemeenten.nl/partnertest",
      label = "Partner test",
      comment = "A partner test is an assertion whether two people have an active partner relation.",
      labels = Map("nl" -> "Partner test"),
      comments = Map(
        "nl" -> "Een partner test is een toetst of een persoon een active partner relatie heeft (met een bepaald persoon).")
    ) {
  object keys extends schema.Thing.Properties {
    object person
        extends PropertyDef(ontology.iri + "/person", label = "person", `@range` = () => schema.Person.ontology :: Nil)

    object result
        extends PropertyDef(
          ontology.iri + "/result",
          label = "result",
          `@extends` = () => Property.properties.getOrCreate("https://schema.org/result", Set()) :: Nil,
          `@range` = () => Label.D.`@boolean` :: Nil
        )
    lazy val resultBoolean: TypedProperty[Boolean] = result + Label.D.`@boolean`
  }
  override lazy val properties: List[Property] = keys.person.property :: keys.result.property :: schema.Thing.properties
  trait Properties {
    lazy val person        = keys.person
    lazy val result        = keys.result
    lazy val resultBoolean = keys.resultBoolean
  }

  def fromNode(node: Node): PartnerTest = {
    PartnerTest(
      node.outE(keys.person.property).map(_.to.iri).toSet,
      node.out(keys.resultBoolean).headOption
    )
  }

  implicit def toNode(cc: PartnerTest): Task[Node] = {
    for {
      node   <- DetachedGraph.nodes.create(ontology)
      person <- Task.gather(cc.person.map(iri => DetachedGraph.nodes.upsert(iri, Set[String]())))
      _      <- Task.gather(person.map(person => node --- keys.person.property --> person))
      _      <- cc.result.map(result => node --- keys.result --> result).getOrElse(Task.unit)
    } yield node
  }
}
case class PartnerTest(person: Set[String], result: Option[Boolean] = None) {
  lazy val toNode: Task[Node] = this
}
