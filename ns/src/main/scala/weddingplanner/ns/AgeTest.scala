package weddingplanner.ns

import lspace.Label
import Label.D._
import lspace.ns.vocab.schema
import lspace.provider.detached.DetachedGraph
import lspace.structure.{Node, OntologyDef, Property, PropertyDef, TypedProperty}
import monix.eval.Task

object AgeTest
    extends OntologyDef(
      "https://ns.convenantgemeenten.nl/AgeTest",
      label = "Age test",
      comment = "An age test is an assertion whether a person satisfies certain age constraints.",
      labels = Map("nl"   -> "Partner test"),
      comments = Map("nl" -> "Een leeftijdstest is een toetst of een persoon aan bepaalde leeftijdskenmerken voldoet.")
    ) {
  object keys extends schema.Thing.Properties {
    object person
        extends PropertyDef(ontology.iri + "/person", label = "person", `@range` = () => schema.Person.ontology :: Nil)

    object minimumAge extends PropertyDef(ontology.iri + "/minimumAge", label = "age", `@range` = () => `@int` :: Nil)
    lazy val minimumAgeInt = minimumAge + `@int`

    object result
        extends PropertyDef(
          ontology.iri + "/result",
          label = "result",
          `@extends` = () => Property.properties.getOrCreate("https://schema.org/result", Set()) :: Nil,
          `@range` = () => Label.D.`@boolean` :: Nil
        )
    lazy val resultBoolean: TypedProperty[Boolean] = result + Label.D.`@boolean`
  }
  override lazy val properties
    : List[Property] = keys.person.property :: keys.minimumAge.property :: keys.result.property :: schema.Thing.properties
  trait Properties {
    lazy val person        = keys.person
    lazy val result        = keys.result
    lazy val minimumAge    = keys.minimumAge
    lazy val resultBoolean = keys.resultBoolean
  }

  def fromNode(node: Node): AgeTest = {
    AgeTest(
      node.outE(keys.person.property).head.to.iri,
      node.out(keys.minimumAgeInt).head,
      node.out(keys.resultBoolean).headOption
    )
  }

  implicit def toNode(cc: AgeTest): Task[Node] = {
    for {
      node   <- DetachedGraph.nodes.create(ontology)
      person <- DetachedGraph.nodes.upsert(cc.person, Set[String]())
      _      <- node --- keys.person.property --> person
      _      <- node --- keys.minimumAge.property --> cc.minimumAge
      _      <- cc.result.map(result => node --- keys.result --> result).getOrElse(Task.unit)
    } yield node
  }
}
case class AgeTest(person: String, minimumAge: Int, result: Option[Boolean] = None) {
  lazy val toNode: Task[Node] = this
}
