package com.thoughtworks.sancoder.effects

import scalaz.Free

object Script {

  def logError(error: String): Script[Unit] = noAction(LogError(error, ()))
  def pure[A](a: A): Script[A] = Free.pure(a)

  private def noAction[A](a: AppAction[A]): Script[A] = Free.liftF[AppAction, A](a)

}
