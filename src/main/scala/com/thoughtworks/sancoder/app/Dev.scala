package com.thoughtworks.sancoder

import scala.io.StdIn

object DevConfig {

  val localDsdb = Map(
    "database.username" -> "root",
    "database.password" -> "",
    "database.url" -> "jdbc:mysql://localhost/sancoder")

  val devProperties = localDsdb ++ Map(
    "language" -> "en",
    "log.level" -> "info",
    "log.file" -> s"/tmp/${Config.appName}/application.log",
    "accessLog.file" -> s"/tmp/${Config.appName}/access.log",
    "app.port" -> "9151",
    "dev.mode" -> "true")
}

object Dev {

  def main(args: Array[String])  {
    sys.props ++= DevConfig.devProperties

    val config = Config.fromSystem()
    val server = Main.startServer(config)

    StdIn.readLine("Press Enter to exit:\n")
    server.stop()
  }
}
