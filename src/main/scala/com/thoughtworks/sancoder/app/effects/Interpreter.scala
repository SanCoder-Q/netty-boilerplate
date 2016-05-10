package com.thoughtworks.sancoder.effects

trait Interpreter {
  def apply[A](a: AppAction[A]): A
}
