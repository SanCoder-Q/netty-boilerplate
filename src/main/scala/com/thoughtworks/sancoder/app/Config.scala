package com.thoughtworks.sancoder

import com.thoughtworks.sancoder.infrastructure.logging.LogConfig
import scala.util.Try
object Config {

  val appName = "netty-boilerplate"
  val host = "www.sancoder.xyz"

  lazy val systemConfig = fromSystem()

  def fromSystem(): Config = {
    def propOr(key: String, default: String): String =
      sys.props.getOrElse(key, default)

    val port = propOr("app.port", "9090").toInt
    val version = propOr("app.version", "DEV")
    val logFile = propOr("log.file", s"/var/log/$appName/application.log")
    val logLevel = propOr("log.level", "info")
    val accessLogFile = propOr("accessLog.file", s"/var/log/$appName/access.log")
    val devMode = Try(propOr("dev.mode", "false").toBoolean).getOrElse(false)

    Config(
      appName = Config.appName,
      port = port,
      version = version,
      devMode = devMode,
      LogConfig(
        logFile = logFile,
        logLevel = logLevel,
        accessLogFile = accessLogFile),
      host
    )
  }
}

case class Config(appName: String, port: Int, version: String, devMode: Boolean,
                  logConfig: LogConfig, host: String)



