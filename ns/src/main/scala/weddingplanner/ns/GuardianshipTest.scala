package weddingplanner.ns

import java.time.LocalDate

import lspace.Label
import Label.D._
import lspace.ns.vocab.schema
import lspace.provider.detached.DetachedGraph
import lspace.structure.{Node, OntologyDef, Property, PropertyDef, TypedProperty}
import monix.eval.Task

object GuardianshipTest
    extends OntologyDef(
      "https://ns.convenantgemeenten.nl/GuardianshipTest",
      label = "Guardianship test",
      comment = "An guardianship test is an assertion whether a person is under guardianship at a certain date.",
      labels = Map("nl" -> "Guardianship test"),
      comments =
        Map("nl" -> "Een curatele-test is een toetst of een persoon onder curatele staat op een bepaalde datum.")
    ) {
  object keys extends schema.Thing.Properties {
    object person
        extends PropertyDef(ontology.iri + "/person", label = "person", `@range` = () => schema.Person.ontology :: Nil)

    object targetDate
        extends PropertyDef("https://ns.convenantgemeenten.nl/targetDate",
                            label = "targetDate",
                            `@range` = () => `@date` :: Nil)
    lazy val targetDateDate = targetDate as `@date`

    object result
        extends PropertyDef(
          ontology.iri + "/result",
          label = "result",
          `@extends` = () => Property.properties.getOrCreate("https://schema.org/result", Set()) :: Nil,
          `@range` = () => Label.D.`@boolean` :: Nil
        )
    lazy val resultBoolean: TypedProperty[Boolean] = result as Label.D.`@boolean`
  }
  override lazy val properties: List[Property] = keys.person.property :: keys.result.property :: schema.Thing.properties
  trait Properties {
    lazy val person         = keys.person
    lazy val targetDate     = keys.targetDate
    lazy val targetDateDate = keys.targetDateDate
    lazy val result         = keys.result
    lazy val resultBoolean  = keys.resultBoolean
  }

  def fromNode(node: Node): GuardianshipTest = {
    GuardianshipTest(
      node.outE(keys.person.property).head.to.iri,
      node.out(keys.targetDateDate).headOption,
      node.out(keys.resultBoolean).headOption
    )
  }

  implicit def toNode(cc: GuardianshipTest): Task[Node] = {
    for {
      node   <- DetachedGraph.nodes.create(ontology)
      person <- DetachedGraph.nodes.upsert(cc.person, Set[String]())
      _      <- node --- keys.person.property --> person
      _      <- cc.targetDate.map(result => node --- keys.targetDate --> result).getOrElse(Task.unit)
      _      <- cc.result.map(result => node --- keys.result --> result).getOrElse(Task.unit)
    } yield node
  }
}

case class GuardianshipTest(person: String, targetDate: Option[LocalDate] = None, result: Option[Boolean] = None) {
  lazy val toNode: Task[Node] = this
}
