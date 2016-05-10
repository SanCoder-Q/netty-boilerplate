package com.thoughtworks.sancoder

import scalaz.Free

package object effects {
  type Script[A] = Free[AppAction, A]

  implicit class ScriptOps[A](aa: Script[A]) {
    def runWith(interpreter: Interpreter): A = aa.go(interpreter(_))
  }
}
