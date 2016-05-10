package com.thoughtworks.sancoder.infrastructure.cors

import com.thoughtworks.sancoder.effects.Script
import com.thoughtworks.sancoder.AppRuntime.{FrameworkRequest, FrameworkResponse}
import unfiltered.response.ResponseHeader
import java.net.URL

class Cors(val hosts: List[String]) {

  def withCorsHeaders(request: FrameworkRequest, response: Script[FrameworkResponse]): Script[FrameworkResponse] = {
    val origin = request.headers("Origin").toStream.headOption
    origin match {
      case Some(a) if isAllowedOrigin(a) => response.map(addCorsHeaders(a))
      case _ => response
    }
  }

  def isAllowedOrigin(origin: String): Boolean = {
    val host = new URL(origin).getHost
    val allowHosts = "localhost" :: hosts
    allowHosts.contains(host)
  }

  private def addCorsHeaders(requestOrigin: String)(response: FrameworkResponse): FrameworkResponse = {
    response ~>
      ResponseHeader("Access-Control-Allow-Origin", Some(requestOrigin)) ~>
      ResponseHeader("Access-Control-Allow-Credentials", Seq("true"))
  }
}
