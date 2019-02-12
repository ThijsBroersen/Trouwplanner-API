package weddingplanner.server

import lspace.lgraph.LGraph
import lspace.lgraph.provider.file.FileStoreProvider
import lspace.lgraph.provider.mem.MemIndexProvider
import lspace.librarian.provider.mem.MemGraph

sealed trait GraphConfig

object GraphConfig {
  import lspace.codec.argonaut._

  implicit class WithGraphConfig(config: GraphConfig) {
    def toGraph = config match {
      case conf: MemGraphConfig  => MemGraph(conf.name)
      case conf: FileGraphConfig => LGraph(FileStoreProvider(conf.name, conf.path), new MemIndexProvider())
    }
  }
}

case class MemGraphConfig(name: String)                extends GraphConfig
case class FileGraphConfig(name: String, path: String) extends GraphConfig
