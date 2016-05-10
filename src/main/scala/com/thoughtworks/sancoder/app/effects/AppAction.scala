package com.thoughtworks.sancoder.effects

import scalaz._

/**
 * We use AppAction objects to represent effects in our application.  The real effects can
 * be performed later (or not) by an Interpreter.
 *
 * By plugging AppAction into Free, we turn our individual instructions into a composable script,
 * that can be defined with "for { a < doStuff; b <- doOtherStuff } yield (a,b)" syntax.
 *
 * AppActions are the mechanism we use to represent:
 * <ul>
 *   <li> Authentication
 *   <li> Database calls
 *   <li> Network calls
 *   <li> Dependency injection
 *   <li> Logging
 * </ul>
 *
 * To add a new action, corresponding to, say, side-effect {{{def digPotato(shovel: Shovel): Potato}}}}}:
 * <ol>
 *   <li> Add a new case class. eg
 *     {{{
 *       case class DigPotato[A](shovel: Shovel, onResult: Potato => A) extends AppAction[A]
 *     }}}
 *     <ul>
 *       <li>"Input" to the effect, if any, (such as the {{{Shovel}}}) should be provided as fields.
 *       <li>"Output" from the effect, if any, (such as a {{{Potato}}}) should be provided as a
 *       callback for the interpreter to plug the result into, resulting in the final result "A".
 *     </ul>
 *   <li> Add a case to the AppAction#map method, composing f with the {{{onResult}}} callback.
 *   <li> Add a construction method for your type to the Script object, for instance:
 *     {{{
 *       def digPotato(shovel: Shovel): Script[Potato] = liftIntoFree(DigPotato(shovel, identity))
 *     }}}
 *     If you have a callback function, pass in the identity function. If there is a just an "A" value, pass in {{{()}}} (pronounced "unit").
 *     {{{Script}}} is a type alias for {{{Free[AppAction, _]}}}, which is a structure that turns our individual
 *   instructions into a composable script.
 *
 *   <li> When testing, create a test Interpreter that interprets your action in a predictable way. You
 *   can run actions with {{{myActions runWith myInterpreter}}}.
 * </ol>
 *
 * This is an established pattern in FP called "Free monads"; further recommended reading:
 *  <ul>
 *    <li> The second half of http://functionaltalks.org/2013/06/17/runar-oli-bjarnason-dead-simple-dependency-injection/
 *    <li> http://timperrett.com/2013/11/21/free-monads-part-1/
 *    <li> http://www.slideshare.net/kenbot/running-free-with-the-monads
 * <p>
 *
 * @tparam A The type of the result obtained by interpreting the AppAction.
 */
sealed trait AppAction[+A] {
  def map[B](f: A => B): AppAction[B] = this match {
    case LogError(error, next) => LogError(error, f(next))
  }
}

case class LogError[A](error: String, next: A) extends AppAction[A]

object AppAction {
  // For Free[AppAction, _] to support flatMap, A Functor must be defined for AppAction; it just uses "map".
  implicit val appActionFunctor: Functor[AppAction] = new Functor[AppAction] {
    override def map[A, B](fa: AppAction[A])(f: A => B): AppAction[B] = fa.map(f)
  }
}
