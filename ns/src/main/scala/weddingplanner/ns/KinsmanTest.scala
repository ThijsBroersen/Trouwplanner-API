package weddingplanner.ns

import lspace.{g, Label}
import lspace.ns.vocab.schema
import lspace.provider.detached.DetachedGraph
import lspace.structure.{Node, OntologyDef, Property, PropertyDef, TypedProperty}
import monix.eval.Task

object KinsmanTest
    extends OntologyDef(
      "https://ns.convenantgemeenten.nl/kinsmantest",
      label = "Kinsman test",
      comment = "A kinsman test is an assertion whether two people are related to some extend",
      labels = Map("nl" -> "Bloedverwantschapsproef"),
      comments = Map(
        "nl" -> ("Een bloedverwantschapsproef toets of twee mensen familie zijn tot op bepaalde hoogte " +
          "(ouders of kinderen -> broers of zussen -> grootouders of kleinkinderen -> neven of nichten)."))
    ) {
  object keys extends schema.Thing.Properties {
    object person1
        extends PropertyDef(ontology.iri + "/person1",
                            label = "person1",
                            `@range` = () => schema.Person.ontology :: Nil)
    object person2
        extends PropertyDef(ontology.iri + "/person2",
                            label = "person2",
                            `@range` = () => schema.Person.ontology :: Nil)
    object degree
        extends PropertyDef(ontology.iri + "/degree", label = "degree", `@range` = () => Label.D.`@int` :: Nil)
    lazy val degreeInt: TypedProperty[Int] = degree + Label.D.`@int`

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
    : List[Property] = keys.person1.property :: keys.person2.property :: keys.degree.property :: keys.result.property :: schema.Thing.properties
  trait Properties {
    lazy val person1       = keys.person1
    lazy val person2       = keys.person2
    lazy val degree        = keys.degree
    lazy val degreeInt     = keys.degreeInt
    lazy val result        = keys.result
    lazy val resultBoolean = keys.resultBoolean
  }

  def fromNode(node: Node): KinsmanTest = {
    KinsmanTest(
      node.outE(keys.person1.property).head.to.iri,
      node.outE(keys.person2.property).head.to.iri,
      node.out(keys.degreeInt).headOption,
      node.out(keys.resultBoolean).headOption
    )
  }

  implicit def toNode(cc: KinsmanTest): Task[Node] = {
    for {
      node    <- DetachedGraph.nodes.create(ontology)
      person1 <- DetachedGraph.nodes.upsert(cc.person1)
      _       <- node --- keys.person1.property --> person1
      person2 <- DetachedGraph.nodes.upsert(cc.person2)
      _       <- node --- keys.person2.property --> person2
      _       <- cc.degree.map(degree => node --- keys.degree --> degree).getOrElse(Task.unit)
      _       <- cc.result.map(result => node --- keys.result --> result).getOrElse(Task.unit)
    } yield node
  }

}
case class KinsmanTest(person1: String, person2: String, degree: Option[Int], result: Option[Boolean] = None) {
  lazy val toNode: Task[Node] = this

  def toLibrarian =
    g.N
      .hasIri(person1)
      .repeat(_.out(schema.parent, schema.children), degree.map(_ - 1).getOrElse(0), true, true)(_.hasIri(person2))
      .hasIri(person2)

  //  def toSPARQL
  //  def toSQL

}
