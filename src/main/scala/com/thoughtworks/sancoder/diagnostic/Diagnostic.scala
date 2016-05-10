package com.thoughtworks.sancoder.diagnostic

import java.util.concurrent.TimeoutException
import scala.concurrent.Await
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import org.slf4j.LoggerFactory
import org.slf4j.Logger

object Diagnostic {

  sealed trait CheckResult
  object CheckSucceeded extends CheckResult
  case class CheckFailed(failureMessage: String) extends CheckResult

  sealed abstract class DiagnosticResult(checkName: String, val successful: Boolean, duration: Duration)
  case class DiagnosticSuccess(checkName: String, duration: Duration) extends DiagnosticResult(checkName, true, duration)
  case class DiagnosticFailure(checkName: String, failureReason: String, duration: Duration) extends DiagnosticResult(checkName, false, duration)
  case class DiagnosticTimeout(checkName: String, duration: Duration) extends DiagnosticResult(checkName, false, duration)

  class StopWatch() {
    val startTime: Long = System.currentTimeMillis()
    lazy val elapsed: Duration = Duration.create(System.currentTimeMillis() - startTime, "ms")
  }

  lazy val log: Logger = LoggerFactory.getLogger(getClass)

  sealed trait DiagnosticCheckDefinition {
    def name: String
    def timeout: Duration
  }

  val defaultTimeoutDuration = Duration.create(3, "s");

  case class BlockingDiagnosticCheckDefinition(name: String, timeout: Duration = defaultTimeoutDuration, check: () => CheckResult)
    extends DiagnosticCheckDefinition

  case class AsyncDiagnosticCheckDefinition(name: String, timeout: Duration = defaultTimeoutDuration, check: () => Future[CheckResult])
    extends DiagnosticCheckDefinition

  class CompletedDiagnostic(results: Seq[DiagnosticResult]) {
    lazy val successful: Boolean = results.forall(_.successful)
    def asText(hostname: String): String = {
      if (successful) {
        "OK - the application is functioning correctly on (" + hostname + ")"
      } else {
        val failuresAsString = results.collect {
          case DiagnosticFailure(checkname, failureReason, _) => s"$checkname failed: $failureReason"
          case DiagnosticTimeout(checkname, _) => s"$checkname timed out"
        }
        s"ERROR on ($hostname) due to\n" + failuresAsString.mkString("\n")
      }
    }
    def asJson(hostname: String): String = {
      val arrayOfJson = results.map {
        case DiagnosticFailure(checkname, failureReason, duration) => s"""{"check" : "$checkname", "successful": false, "failure" : "$failureReason", "durationMs": ${duration.toMillis}}"""
        case DiagnosticTimeout(checkname, duration) => s"""{"check" : "$checkname", "successful": false, "failure" : "timeout", "durationMs": ${duration.toMillis}}"""
        case DiagnosticSuccess(checkname, duration) => s"""{"check" : "$checkname", "successful": true, "durationMs": ${duration.toMillis}}"""
      }.mkString(",")

      s"""{ "successful": $successful, "host": "$hostname", "checks": [ $arrayOfJson ] }"""
    }
  }

  def executeChecks(checks: Seq[DiagnosticCheckDefinition]): CompletedDiagnostic = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val completedResults = checks.par.map(executeSingleCheck)
    new CompletedDiagnostic(completedResults.seq)
  }

  def executeSingleCheck(check: DiagnosticCheckDefinition)(implicit executionContext: ExecutionContextExecutor): DiagnosticResult  = {
    val stopWatch = new StopWatch()
    try {
        val result = Await.result(execute(check), check.timeout)
        result match {
          case CheckSucceeded => DiagnosticSuccess(check.name, stopWatch.elapsed)
          case CheckFailed(message) => DiagnosticFailure(check.name, message, stopWatch.elapsed)
        }
      } catch {
        case e: TimeoutException =>
          log.error(s"Timeout checking ${check.name}, waited for ${stopWatch.elapsed}")
          DiagnosticTimeout(check.name, stopWatch.elapsed)
        case e: Exception =>
          log.error(s"Failure checking ${check.name} after ${stopWatch.elapsed}", e)
          DiagnosticFailure(check.name, e.getMessage, stopWatch.elapsed)
      }
  }

  def execute(checkDefinition: DiagnosticCheckDefinition)(implicit executionContext: ExecutionContextExecutor)
  : Future[CheckResult] = checkDefinition match {
    case BlockingDiagnosticCheckDefinition(_, _ , blockingCheck) => Future { blockingCheck() }
    case AsyncDiagnosticCheckDefinition(_, _, asyncCheck) => asyncCheck()
  }

}