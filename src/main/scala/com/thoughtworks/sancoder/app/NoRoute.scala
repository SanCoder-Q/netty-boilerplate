package com.thoughtworks.sancoder

import com.thoughtworks.sancoder.AppRuntime.InterpretedRoutes
import org.slf4j.LoggerFactory
import unfiltered.request.Path
import unfiltered.response.{HtmlContent, NotFound, ResponseString}

object NoRoute {

  lazy val log = LoggerFactory.getLogger(getClass)

  def apply(responseContent: String = "page not found"): InterpretedRoutes = {
    case Path(path) =>
    NotFound ~> HtmlContent ~> ResponseString(responseContent)
  }
}