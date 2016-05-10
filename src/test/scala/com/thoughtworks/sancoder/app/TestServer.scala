package com.thoughtworks.sancoder

import com.thoughtworks.sancoder.infrastructure.logging.LogConfig
import dispatch.Defaults._
import dispatch._


class TestServer(name: String, appPort: Int) {


  val config = Config(
    appName = name,
    port = appPort,
    version = s"test $name",
    devMode = true,
    LogConfig(
      logFile = s"/tmp/$name/application.log",
      logLevel = "info",
      accessLogFile = s"/tmp/$name/access.log"),
    "www.sancoder.xyz"
  )

  val server = Main.startServer(config)

  def stopServer() = {
    server.stop()
  }


  def unauthenticatedHttp(request:Req): Future[Res] = {
    Http(request)
  }

  def toFuture[A](maybe: Option[A], error: String): Future[A] = {
    maybe match {
      case None => Future.failed(new Exception(error))
      case Some(value) => Future.successful(value)
    }
  }

  def request = host("localhost", config.port)

}
