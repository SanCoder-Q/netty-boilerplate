package com.thoughtworks.sancoder.infrastructure.diagnostics

import argonaut._, Argonaut._
import com.thoughtworks.sancoder.diagnostic.Diagnostic
import com.thoughtworks.sancoder.diagnostic.Diagnostic.{CompletedDiagnostic, DiagnosticCheckDefinition}
import com.thoughtworks.sancoder.AppRuntime.InterpretedRoutes
import org.slf4j.LoggerFactory
import unfiltered.request.Path
import unfiltered.response._

case class DiagnosticLink(name: String, url: String)

class DiagnosticRoutes(val diagnosticConfig: DiagnosticConfig) {

  lazy val controller: DiagnosticController = new DiagnosticController(diagnosticConfig)

  def routes(prefix: String = "/diagnostic"): InterpretedRoutes = {
    case Path(p) if p == s"$prefix/status/heartbeat" => controller.getHeartBeat
    case Path(p) if p == s"$prefix/status/apprunning" => controller.getHeartBeat
    case Path(p) if p == s"$prefix/status/nagios" => controller.getNagios
    case Path(p) if p == s"$prefix/status/diagnosis" => controller.getDiagnosis
    case Path(p) if p == s"$prefix/version" => controller.getVersion
    case Path(p) if p == s"$prefix/host" => controller.getHost
    case Path(p) if p == s"$prefix/" => controller.getLinks(prefix)
    case Path(p) if p == s"$prefix" => controller.getLinks(prefix)
  }
}

case class DiagnosticConfig(version: String,
                            host: String = java.net.InetAddress.getLocalHost.getHostName,
                            heartbeatChecks: Seq[DiagnosticCheckDefinition],
                            diagnosticChecks: Seq[DiagnosticCheckDefinition])

class DiagnosticController(val diagnosticConfig: DiagnosticConfig) {

  val log = LoggerFactory.getLogger(getClass)
  val heartbeatChecks = diagnosticConfig.heartbeatChecks
  val diagnosticChecks = diagnosticConfig.heartbeatChecks ++ diagnosticConfig.diagnosticChecks

  val diagnosticLinks = Seq(
    DiagnosticLink("heartbeat", "status/heartbeat"),
    DiagnosticLink("apprunning", "status/apprunning"),
    DiagnosticLink("nagios", "status/nagios"),
    DiagnosticLink("diagnosis", "status/diagnosis"),
    DiagnosticLink("host", "host"),
    DiagnosticLink("version", "version"))

  def getHeartBeat[T]: ResponseFunction[T] = uncachedCheck(heartbeatChecks, { completedDiagnostic =>
    if (completedDiagnostic.successful) {
      addHeaders("OK")
    } else {
      textOutputter(completedDiagnostic)
    }
  })

  def getVersion[T]: ResponseFunction[T] = addHeaders(diagnosticConfig.version)

  def getHost[T]: ResponseFunction[T] = addHeaders(diagnosticConfig.host)

  def getNagios[T]: ResponseFunction[T] = {
    uncachedCheck(diagnosticChecks, textOutputter)
  }

  def getDiagnosis[T]: ResponseFunction[T] = {
    uncachedCheck(diagnosticChecks, jsonOutputter)
  }

  def getLinks[T](prefix: String): ResponseFunction[T] = {
    implicit def DiagnosticLinkEncodeJson: EncodeJson[DiagnosticLink] =
      EncodeJson((link: DiagnosticLink) =>
        ("path" := s"${prefix}/${link.url}")  ->: ("rel" := link.name) ->:  jEmptyObject)
    val diagnosticArray: JsonArray = diagnosticLinks.map(_.jencode).toList
    ResponseString(diagnosticArray.asJson.spaces2) ~> JsonContent
  }

  private def addHeaders[T](body: String): ResponseFunction[T] =
    PlainTextContent ~> uncached ~> ResponseString(body)

  private def textOutputter[T](completedDiagnostic: CompletedDiagnostic): ResponseFunction[T] = {
    PlainTextContent ~> ResponseString(completedDiagnostic.asText(diagnosticConfig.host))
  }

  private def jsonOutputter[T](completedDiagnostic: CompletedDiagnostic): ResponseFunction[T] = {
    JsonContent ~> ResponseString(completedDiagnostic.asJson(diagnosticConfig.host))
  }

  private def uncachedCheck[T](checks: Seq[DiagnosticCheckDefinition], outputter: CompletedDiagnostic => ResponseFunction[T]): ResponseFunction[T] = {
    val completedCheck = Diagnostic.executeChecks(checks)
    val status = if (completedCheck.successful) {
      Ok
    } else {
      InternalServerError
    }
    status ~> outputter(completedCheck) ~> uncached
  }

  private def uncached[T]: ResponseFunction[T] = CacheControl("max-age=0, no-cache, no-store")
}

