package weddingplanner.server

import java.nio.file.Paths

final case class Port(value: Int) extends AnyVal
case class ServicesConfig(port: Port = Port(8080),
                          agendaGraph: GraphConfig,
                          appointmentGraph: GraphConfig,
                          personGraph: GraphConfig,
                          placeGraph: GraphConfig)

object ServicesConfig {
  import pureconfig._
  import pureconfig.generic.auto._

  implicit val memGraphReader: ConfigReader[GraphConfig] = ConfigReader
    .forProduct2("name", "path")(FileGraphConfig(_, _))
    .orElse(ConfigReader.forProduct1("name")(MemGraphConfig(_)))

  val config: ServicesConfig =
    pureconfig
      .loadConfig[ServicesConfig]
      .toOption
      .orElse(Option(System.getenv("WEDDING_CONFIG"))
        .map(iri =>
          pureconfig.loadConfig[ServicesConfig](Paths.get(iri)) match {
            case Right(r) => r
            case Left(e)  => throw new Exception(e.toString)
        }))
      .orElse(pureconfig
        .loadConfig[ServicesConfig](Paths.get(System.getProperty("user.home") + "/weddingplanner.conf")) match {
        case Right(r) => Some(r)
        case Left(e) =>
          scribe.error(e.toString)
          None
      })
      .getOrElse {
        import com.typesafe.config.ConfigFactory
        scribe.warn("no context file found, starting in-memory graphs")
        pureconfig
          .loadConfig[ServicesConfig](
            ConfigFactory.parseString("""
              |{
              |  port : 8080,
              |  agenda-graph : { name : "WeddingPlannerAgendaGraph" },
              |  appointment-graph : { name : "WeddingPlannerAppointmentGraph" },
              |  person-graph : { name : "WeddingPlannerPersonGraph" },
              |  place-graph : { name : "WeddingPlannerPlaceGraph" }
              |}
            """.stripMargin))
          .getOrElse(throw new Exception("could not load any config ..."))
      }
}
