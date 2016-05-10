package com.thoughtworks.sancoder.infrastructure.cors

import com.thoughtworks.sancoder.AppRuntime._
import unfiltered.request.OPTIONS
import unfiltered.response._

class OptionsRoutes(hosts: String*) {

  def checkOrigin(request: FrameworkRequest): Boolean = {
    val origin = request.headers("Origin").toStream.headOption
    origin.exists(new Cors(hosts.toList).isAllowedOrigin)
  }

  def routes: InterpretedRoutes = {
     case OPTIONS(a) => {
       if (checkOrigin(a)) {
           ResponseHeader("Access-Control-Allow-Origin", a.headers("Origin").toStream.headOption) ~>
           ResponseHeader("Access-Control-Allow-Credentials", Seq("true")) ~>
           ResponseHeader("Access-Control-Allow-Headers", Seq("Token, Origin, Accept, Content-Type")) ~>
           ResponseHeader("Access-Control-Allow-Methods", Seq("OPTIONS, POST, GET, PUT, HEAD"))
       } else {
         Unauthorized
       }
     }
  }

}
