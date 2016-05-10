package com.thoughtworks.sancoder

import unfiltered.netty.ResponseBinding
import unfiltered.response.{ResponseFunction, HttpResponse}
import io.netty.handler.codec.{http => netty}
import java.nio.charset.Charset

trait TestUnfilteredResponse {

  private def newResponse = new netty.DefaultFullHttpResponse(netty.HttpVersion.HTTP_1_1, netty.HttpResponseStatus.OK)
  private def bind(res: netty.DefaultFullHttpResponse) = new ResponseBinding(res)
  private def applyResp(r: ResponseFunction[netty.HttpResponse]) = r(bind(newResponse))

  private def b(r: HttpResponse[netty.DefaultFullHttpResponse]) = r.underlying.content().toString(Charset.defaultCharset())
  private def s(r: HttpResponse[netty.HttpResponse]) = r.underlying.getStatus.code
  private def h(name: String)(r: HttpResponse[netty.DefaultFullHttpResponse]) = r.underlying.headers.get(name)

  def body: ResponseFunction[netty.HttpResponse] => String = applyResp _ andThen b
  def statusCode: ResponseFunction[netty.HttpResponse] => Int = applyResp _ andThen s
  def header(name: String): ResponseFunction[netty.HttpResponse] => String = applyResp _ andThen h(name)
}
