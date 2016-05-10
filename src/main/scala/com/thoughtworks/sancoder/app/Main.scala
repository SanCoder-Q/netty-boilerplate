package com.thoughtworks.sancoder

object Main {

  def main(args: Array[String]) = {
    val config = Config.fromSystem()
    val server = startServer(config)
  }

  def startServer(config: Config) = {
    lazy val routes = new AppRuntime(config).routes
    val server = new NettyAppServer(config.port, routes, config.logConfig)
    server.start()

    println(s"Server started... diagnostic URL here: \nhttp://localhost:${config.port}/diagnostic")
    server
  }
}
