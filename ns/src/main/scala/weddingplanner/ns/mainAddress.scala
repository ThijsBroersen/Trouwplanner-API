package weddingplanner.ns

import lspace.structure.PropertyDef

object mainAddress
    extends PropertyDef(
      "http://bag.basisregistraties.overheid.nl/def/bag#hoofdadres",
      label = "mainAddress",
      comment = "A main address is the primary address of an addressable object",
      `@extends` = () => lspace.ns.vocab.schema.address.property :: Nil,
      labels = Map("nl"   -> "hoofdadres"),
      comments = Map("nl" -> "Een hoofdadres is het primaire adres van een adresseerbaar object")
    )
