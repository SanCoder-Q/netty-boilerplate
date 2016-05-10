package com.thoughtworks.sancoder

import com.thoughtworks.sancoder.effects._

case class TestInterpreter() extends Interpreter {

  def apply[A](searchAction: AppAction[A]): A = searchAction match {
    case LogError(_, next) => next
  }
}
