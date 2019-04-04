package weddingplanner.server

import lspace.structure.Graph
import monix.eval.Task

object SigmaJsVisualizer {
  import lspace.Implicits.AsyncGuide.guide

  def visualizeGraph(graph: Graph): Task[String] =
    for {
      edges <- lspace.g.E().withGraph(graph).toListF
    } yield {

      import lspace.codec.argonaut.nativeEncoder

      val from = edges.map(_.from)
      val to   = edges.map(_.to)

      import nativeEncoder._
      val nodesJsons = (from ++ to).distinct.map { r =>
        Map(
          "id" -> r.id.asJson,
          "label" -> (r.labels.headOption.flatMap(_.label("en")).getOrElse("") + r.out(
            lspace.ns.vocab.schema.name + lspace.Label.D.`@string`)).asJson
        ).asJson
      }
      val edgesJsons = edges.distinct.map { e =>
        Map("id" -> e.id.asJson, "source" -> e.from.id.asJson, "target" -> e.to.id.asJson).asJson
      }

      Map("nodes" -> nodesJsons.asJson, "edges" -> edgesJsons.asJson).asJson.nospaces
    }
}
