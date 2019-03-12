package weddingplanner.ns

import lspace.structure.PropertyDef

object agenda
    extends PropertyDef("ns.convenantgemeenten.nl/agenda",
                        label = "agenda",
                        comment = "An agenda ..",
                        `@range` = () => Agenda.ontology :: Nil)
