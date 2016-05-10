package com.thoughtworks.sancoder.fixtures

import java.io._

import com.thoughtworks.sancoder.AppRuntime.FrameworkRequest

import scala.Option.option2Iterable

case class MockFrameworkRequest(method: String = "GET",
                                body: String = "",
                                protocol: String = "http",
                                uri: String = "",
                                headers: Map[String, String] = Map.empty,
                                params: Map[String, String] = Map.empty) extends FrameworkRequest(null) {

  def inputStream: java.io.InputStream =
    new ByteArrayInputStream(body.getBytes)

  def reader: java.io.Reader = new java.io.InputStreamReader(inputStream)

  def parameterNames: Iterator[String] = params.keysIterator

  def parameterValues(param: String): Seq[String] = params.get(param).toSeq

  def headerNames: Iterator[String] = headers.keysIterator

  def headers(name: String): Iterator[String] = headers.get(name).iterator

  def isSecure = false

  def remoteAddr: String = ""
}
