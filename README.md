[![Build Status](https://travis-ci.com/ThijsBroersen/Weddingplanner-API.svg)](https://travis-ci.com/ThijsBroersen/Weddingplanner-API)
[![codecov](https://codecov.io/gh/ThijsBroersen/Weddingplanner-API/branch/master/graph/badge.svg)](https://codecov.io/gh/ThijsBroersen/Weddingplanner-API)

# The Weddingplanner API


## Getting started

### Modules

- `weddingplanner-ns`: namespace definitions (ontologies and properties), Semantic schema repository
- `weddingplanner-api`: rest-api's for ...
- `weddingplanner-service`: services to coordinate a wedding planning process

Weddingplanner modules are available for Scala 2.12.x. 
To include `weddingplanner-xx` add the following to your `build.sbt`:
```
libraryDependencies += "nl.thijsbroersen" %% "weddingplanner-{xx}" % "{version}"
```

## Examples
Run local:
```
sbt weddingplannerService/run
```
or with docker:
```
cd examples
docker-compose up
```
