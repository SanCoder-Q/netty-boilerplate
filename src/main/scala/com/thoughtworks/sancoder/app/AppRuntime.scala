package com.thoughtworks.sancoder

import java.nio.file.Path

import com.thoughtworks.sancoder.AppRuntime._
import com.thoughtworks.sancoder.effects._
import com.thoughtworks.sancoder.infrastructure.cors.OptionsRoutes
import com.thoughtworks.sancoder.infrastructure.diagnostics.{DiagnosticConfig, DiagnosticRoutes}
import io.netty.handler.codec.{http => netty}
import unfiltered.netty.ReceivedMessage
import unfiltered.request.HttpRequest
import unfiltered.response.ResponseFunction

import scala.io.Source
import scala.util.Try

object AppRuntime {

  type FrameworkRequest = HttpRequest[ReceivedMessage]
  type FrameworkResponse = ResponseFunction[netty.HttpResponse]

  type Routes = PartialFunction[FrameworkRequest, Script[FrameworkResponse]]

  type InterpretedRoutes = PartialFunction[FrameworkRequest, FrameworkResponse]
}

class AppRuntime(config: Config) {


  lazy val interpreter = new AppInterpreter(config)

  lazy val diagnosticConfig: DiagnosticConfig =
    DiagnosticConfig(version = config.version, heartbeatChecks = Seq(), diagnosticChecks = Seq())

  lazy val routes: InterpretedRoutes = {

    val diagnosticRoutes = new DiagnosticRoutes(diagnosticConfig).routes()
    val optionsRoutes = new OptionsRoutes(config.host).routes

    optionsRoutes orElse diagnosticRoutes orElse NoRoute()
  }

  private def interpretedRoutes(routes: Routes, interpret: Interpreter): InterpretedRoutes = {
    routes.andThen(_.runWith(interpret))
  }

  private def readFile(path: Path): Try[String] = Try(Source.fromFile(path.toFile).mkString)
}
