package weddingplanner.server

import lspace.codec.ActiveContext
import lspace.encode.EncodeJson
import lspace.structure.Node
import lspace.structure.Property.default.`@id`

object EncodeJson {

  private def _nodeToJsonMap(node: Node)(implicit encoder: lspace.codec.Encoder): encoder.Json = {
    import encoder._
    encoder.mapToJson(
      node
        .outEMap()
        .map {
          case (`@id`, id) =>
            `@id`.iri -> encoder.textToJson(id.head.to.value.toString) //head.to.value.toString.reverse
//                .takeWhile(_ != '/')
//                .reverse) //id.head.to.value.toString.stripPrefix(node.graph.iri + "/").asJson
          case (property, edges) =>
            property.label("en").getOrElse(property.iri) -> (edges match {
              case List(edge) =>
                encoder.fromAny(edge.to, edge.to.labels.headOption)(ActiveContext()).json
              case edges =>
                encoder.listToJson(edges
                  .map(edge => encoder.fromAny(edge.to, edge.to.labels.headOption)(ActiveContext()).json))
            })
        })
  }

  implicit def nodeToJson[T <: Node](implicit encoder: lspace.codec.Encoder) = new EncodeJson[T] {
    val encode = (node: T) => _nodeToJsonMap(node).toString()
  }

  implicit def nodesToJson[T <: Node](implicit encoder: lspace.codec.Encoder) = new EncodeJson[List[T]] {
    def encode: List[T] => String =
      (nodes: List[T]) => encoder.listToJson(nodes.map(_nodeToJsonMap(_).asInstanceOf[encoder.Json])).toString
  }

  implicit val encodeJsonJson = new EncodeJson[String] {
    def encode = (string: String) => string
  }
}
