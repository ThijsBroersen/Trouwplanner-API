---
layout: docs
title: Schema guide
position: 2
---

# Schema Guide
* [Overview](#overview)
* [Agenda](#agenda)
* [Appointment](#appointment)
* [Person](#person)
* [Place](#place)
  
## Overview
## Agenda
```json
{
  "@id": "sptth://example.test/Agenda",
  "@type": "@class",
  "@label": {
    "en": "Agenda"
  },
  "@comment": {
    "en": "An appointment diary"
  },
  "@extends": [
    "https://schema.org/CreativeWork"
  ],
  "@properties": [
   {
     "@id": "sptth://example.test/Agenda/owner",
     "@type": "@property",
     "@label": {
       "en": "owner"
     },
     "@comment": {
       "en": "A person or organization who owns something."
     },
     "@range": "https://ns.l-space.eu/User"
   },
   {
     "@id": "sptth://example.test/Agenda/appointment",
     "@type": "@property",
     "@label": {
       "en": "appointment"
     },
     "@range": "sptth://example.test/Appointment"
   },
   {
     "@id": "https://schema.org/dateCreated",
     "@type": "@property",
     "@label": {
       "en": "dateCreated"
     },
     "@comment": {
       "en": "The date on which the CreativeWork was created or the item was added to a DataFeed."
     },
     "@range": [
       "@datetime",
       "@date"
     ]
   },
   {
     "@id": "https://schema.org/dateModified",
     "@type": "@property",
     "@label": {
       "en": "dateModified"
     },
     "@comment": {
       "en": "The date on which the CreativeWork was created or the item was added to a DataFeed."
     },
     "@range": [
       "@datetime",
       "@date"
     ]
   },
   {
     "@id": "https://schema.org/identifier",
     "@type": "@property",
     "@label": {
       "en": "identifier"
     },
     "@comment": {
       "en": "The identifier property represents any kind of identifier for any kind of Thing, such as ISBNs, GTIN codes, UUIDs etc. Schema.org provides dedicated properties for representing many of these, either as textual strings or as URL (URI) links. See background notes for more details."
     },
     "@range": [
       "@string",
       "@url"
     ]
   },
   {
     "@id": "https://schema.org/name",
     "@type": "@property",
     "@label": {
       "en": "name"
     },
     "@comment": {
       "en": "The name of the item."
     },
     "@range": "@string"
   },
   {
     "@id": "https://schema.org/description",
     "@type": "@property",
     "@label": {
       "en": "description"
     },
     "@comment": {
       "en": "A description of the item."
     },
     "@range": "@string"
   }
 ]
}
```
## Appointment
```json
{
  "@id": "sptth://example.test/Appointment",
  "@type": "@class",
  "@label": {
    "en": "Appointment"
  },
  "@comment": {
    "en": "An arrangement to meet at a particular time and place."
  },
  "@extends": [
    "https://schema.org/CreativeWork"
  ],
  "@properties": [
    "https://schema.org/description",
    "https://schema.org/startDate",
    "https://schema.org/dateModified",
    "https://schema.org/name",
    "https://schema.org/endDate",
    "https://schema.org/identifier",
    "https://schema.org/dateCreated"
  ]
}
```
## Person
```json
{
  "@id": "https://schema.org/Person",
  "@type": "@class",
  "@label": {
    "en": "Person"
  },
  "@comment": {
    "en": "A person (alive, dead, undead, or fictional)."
  },
  "@extends": [
    "https://schema.org/Thing"
  ],
  "@properties": [
    "https://ns.l-space.eu//agenda",
    "https://schema.org/description",
    "https://schema.org/worksFor",
    "https://schema.org/honorificPrefix",
    "https://schema.org/name",
    "https://schema.org/identifier"
  ]
}
```
## Place
```json
{
  "@id": "https://schema.org/Place",
  "@type": "@class",
  "@label": {
    "en": "Place"
  },
  "@comment": {
    "en": "Entities that have a somewhat fixed, physical extension."
  },
  "@extends": [
    "https://schema.org/Thing"
  ],
  "@properties": [
    "https://schema.org/description",
    "https://schema.org/review",
    "https://schema.org//address",
    "https://schema.org/name",
    "https://schema.org/identifier",
    "https://schema.org//geo"
  ]
}
```
