package com.thoughtworks.sancoder

import java.util.concurrent.Executors

import com.thoughtworks.sancoder.AppRuntime.{InterpretedRoutes, FrameworkRequest, FrameworkResponse}
import com.thoughtworks.sancoder.infrastructure.logging.{LogConfig, AccessLoggingChannelHandler, Logging}
import org.slf4j.LoggerFactory
import unfiltered.netty.Server
import unfiltered.netty.future.Planify
import unfiltered.netty.future.Plan.Intent
import unfiltered.response.InternalServerError

import scala.concurrent.{Future, ExecutionContext}

trait AppServer {

  val server: Server

  def start(): Unit = server.start()

  def stop(): Unit = server.stop()
}

trait LogConfigAppServer extends AppServer with Logging {
  override def start() = {
    configureLog()
    LoggerFactory.getLogger(getClass).info("About to start the server ...")
    super.start()
  }

  override def stop() = {
    LoggerFactory.getLogger(getClass).info("About to sleep for 1 second ...")
    Thread.sleep(1000)
    LoggerFactory.getLogger(getClass).info("About to stop the server ...")
    super.stop()
    LoggerFactory.getLogger(getClass).info("Finished stopping the server ...")
  }
}

class NettyAppServer(port: Int, routes: InterpretedRoutes,  val logConfig : LogConfig) extends LogConfigAppServer {

  lazy val log = LoggerFactory.getLogger(getClass)
  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(40))

  private def frameworkifyRoutes(routes: InterpretedRoutes)(implicit executionContext: ExecutionContext): Intent = {
    case req if routes.isDefinedAt(req) =>
      Future(routes(req)).recover {

        // TODO find a better place to define error handling
        case e => log.error(s"Failure processing request: ${req.uri}", e)
          InternalServerError
      }
  }

  override val server: Server = unfiltered.netty.Server.http(port)
    .makePlan(AccessLoggingChannelHandler).plan(Planify(frameworkifyRoutes(routes)))
}
