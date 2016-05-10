package com.thoughtworks.sancoder.effects

import com.thoughtworks.sancoder.Config
import org.slf4j.LoggerFactory

import scala.language.implicitConversions

class AppInterpreter(config: Config) extends Interpreter {
  private val log = LoggerFactory.getLogger("application")

  def apply[A](action: AppAction[A]): A = {
    action match {

      case LogError(error, next) => {
        log.error(error)
        next
      }
    }
  }
}
