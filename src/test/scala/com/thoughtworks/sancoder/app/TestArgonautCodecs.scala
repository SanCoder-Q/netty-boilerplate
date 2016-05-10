package com.thoughtworks.sancoder

import argonaut._, Argonaut._
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments

trait TestArgonautCodecs {
  this: Specification =>

  def testDecoded[A: DecodeJson](json: String)(jsonChecks: A => Fragments): Fragments = {
    json.decodeWithMessage(jsonChecks, { msg =>
      "Json decoding failed" >> { failure(msg) }
    })
  }
}
