package com.thoughtworks.sancoder.infrastructure.logging

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, LoggerContext}
import ch.qos.logback.core.FileAppender
import org.slf4j.Logger
import org.slf4j.bridge.SLF4JBridgeHandler
import org.slf4j.impl.StaticLoggerBinder


case class LogConfig(logFile: String, logLevel: String, accessLogFile: String)

trait Logging {

  def logConfig: LogConfig

  private def doTo[A](a: A)(fs: A => Unit*): A = {
    fs.foldLeft(a) {(_, f) => f(a); a}
  }

  private def logTo(context: LoggerContext, loggerName: String, fileName: String, pattern: String, level: String,
                    additive: Boolean = true) = {

    doTo(context.getLogger(loggerName))(
      _.addAppender(doTo(new FileAppender[ILoggingEvent])(
        _.setFile(fileName),
        _.setEncoder(doTo(new PatternLayoutEncoder)(
          _.setPattern(pattern),
          _.setContext(context),
          _.start())),
        _.setContext(context),
        _.start())),
      _.setAdditive(additive),
      _.setLevel(Level.toLevel(level)))
  }

  protected def configureLog() = {
    val context = StaticLoggerBinder.getSingleton.getLoggerFactory.asInstanceOf[LoggerContext]

    context.reset()

    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    val LogConfig(logFile, logLevel, accessLogFile) = logConfig

    val pattern = "%date{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %level [%thread] %logger{10} [%file:%line] %msg%n"
    logTo(context, Logger.ROOT_LOGGER_NAME, logFile, pattern, logLevel)
    logTo(context, "access", accessLogFile, "%msg%n", "info", additive = false)
    println(s"Access logs: ${accessLogFile}")
    println(s"Application logs: ${logFile}")
  }

}
