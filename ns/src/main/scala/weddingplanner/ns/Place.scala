package weddingplanner.ns

import lspace.client.User
import lspace.structure.PropertyDef
import lspace.types.vector.Point
import weddingplanner.ns.Agenda

case class Place(id: Option[Int], name: String, description: String, address: String, geo: Point, agenda: Agenda)
