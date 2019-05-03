---
layout: docs
title: Endpoint guide
position: 2
---

# Endpoint Guide
* [Overview](#overview)
* [Data services](#data-services)
  * [Agenda](#agenda)
  * [Appointment](#appointment)
  * [Person](#person)
  * [Place](#place)
* [Process services](#process-services)
  * [Wedding reservation](#wedding-reservation)
  * [Report of marriage](#report-of-marriage)
* [Knowledge services](#knowledge-services)
  * [Kinsman](#kinsman-service)
  
## Overview
There are multiple endpoints, each has its own purpose. 
## Data services
Data services are the main storage for data. 
They are support a traditional rest-api to allow for all crud-operations (if authorized).
All api's are dynamic. The active context is used to deconstruct url-paths. The api's use the following structures:
GET```http://example.org/{label}/{id}``` or
GET```http://example.org/{label}/{id}/{property}``` or
GET```http://example.org/{label}/{id}/{property}/{property}```
The active context determined where the property-name resolved to and whether is an out-going or an in-coming relation to the object.

### Agenda
The agenda-endpoint is an service which registers agenda's.
Active context: [agenda/context](http://convenantgemeenten.nl/agenda/context)

### Appointment
Active context: [appointment/context](http://convenantgemeenten.nl/appointment/context)

### Person
Active context: [person/context](http://convenantgemeenten.nl/person/context)

### Place
Active context: [place/context](http://convenantgemeenten.nl/place/context)

## Process services
Process services which support stateful resource operations. 
The available operations depend on the state of the resource (including remote state of linked objects). 

### Wedding reservation
A wedding service mediates in planning a wedding.  
Active context: [weddingreservation/context](http://convenantgemeenten.nl/weddingreservation/context)

### Report of marriage
A report of marriage is a official notification from the expected wedding couple to the local authorities.
Active context: [reportofmarriage/context](http://convenantgemeenten.nl/reportofmarriage/context)

## Knowledge services
Knowlegde services can be queried or asked for information. They are a hotspot or interchange for information. 
A knowledge service can execute federated queries, this means that the query contains parts to be executed on other services. 
This can be the result of a query or some sort of assertion.

### Kinsman
This service can test whether two people are related to some extend. 
It returns a boolean (```Content-Type: text/boolean```)
Request structure:
GET```/kinsman?id={id1},{id2}&degree={degree}```  
or  
GET```/kinsman?id={id1}&id={id2}&degree={degree}```  
or  
POST ```/kinsman```  
body ```{"person2":{"@id":"http://$host/person/$id1"},"person1":{"@id":"http://$host/person/$id2"},"degree":$degree}```

### Partner
This service can test whether either person have a partner (active marriage).
It returns a boolean (```Content-Type: text/boolean```)
Request structure:
GET```/partner?id={id1},{id2}```  
or  
GET```/partner?id={id1}&id={id2}```  
or  
POST ```/partner```  
body ```{"person":[{"@id":"http://$host/person/$id1"},{"@id":"http://$host/person/$id2"}]}```

### Age
This service can test if a persons satisfies certain age constraints.
It returns a boolean (```Content-Type: text/boolean```)
Request structure:
GET```/age?id={id}&minimumAge={age}```  
or  
POST ```/age```  
body ```{"person":{"@id":"http://$host/person/$id1"},"minimumAge":18}```