package com.thoughtworks.sancoder.diagnostic

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import org.junit.runner.RunWith
import org.specs2.matcher.JsonMatchers
import org.specs2.mutable.Specification

import com.thoughtworks.sancoder.diagnostic.Diagnostic._

class DiagnosticSpec extends Specification with JsonMatchers {

  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(3))

  "executeSingleCheck" should {
    "return the check results when the check is happy" in {
      val successfulFutureResult = Future { CheckSucceeded }
      executeSingleCheck(AsyncDiagnosticCheckDefinition("successCheck", Duration("2s"), () => successfulFutureResult)) must haveClass[DiagnosticSuccess]
    }
    "return diagnostic timeout when the check takes too long" in {
      val excessivelyTimePoorResult = Future {
        Thread.sleep(5000)
        CheckSucceeded
      }
      executeSingleCheck(AsyncDiagnosticCheckDefinition("checkName", Duration("200ms"), () => excessivelyTimePoorResult)) must haveClass[DiagnosticTimeout]
    }
    "return diagnostic failure when the check returns a failure" in {
      val failingFutureResult = Future { CheckFailed("failure reason") }
      executeSingleCheck(AsyncDiagnosticCheckDefinition("checkName", Duration("200ms"), () => failingFutureResult)) must haveClass[DiagnosticFailure]
    }
    "return diagnostic failure when the check blows up" in {
      val unexpectedlyFailingFutureResult: Future[CheckResult] =
        Future { throw new RuntimeException("runtime message") }
      executeSingleCheck(AsyncDiagnosticCheckDefinition("checkName", Duration("200ms"), () =>
        unexpectedlyFailingFutureResult)) must haveClass[DiagnosticFailure]
    }
  }
  "CompletedDiagnostic that has no failures" should {
    val successfulDiagnostic = new CompletedDiagnostic(Seq(DiagnosticSuccess("successCheck", Duration.create(2, "s")), DiagnosticSuccess("successCheck", Duration.create(2, "s"))))
    "be successful" in {
      successfulDiagnostic.successful === true
    }
    "have a lovely message" in {
      successfulDiagnostic.asText("hostname") === "OK - the application is functioning correctly on (hostname)"
    }
    "return top level success" in {
      successfulDiagnostic.asJson("hostname") must /("successful" -> true)

    }
    "include the hostname" in {
      successfulDiagnostic.asJson("hostname") must /("host" -> "hostname")
    }
    // Missing check that the detailed output is correct
  }

  "Completed Diagnostic that has failures" should {
    val failedDiagnostic = new CompletedDiagnostic(Seq(DiagnosticSuccess("successCheck", Duration.create(2, "s")), DiagnosticFailure("checkName", "anything", Duration.create(2, "s"))))
    "be unsuccessful" in {
      failedDiagnostic.successful === false
    }
    "have a message that reflects the failure" in {
      failedDiagnostic.asText("hostname") === "ERROR on (hostname) due to\ncheckName failed: anything"
    }
    "should return top level failure" in {
      failedDiagnostic.asJson("hostname") must /("successful" -> false)
      failedDiagnostic.asJson("hostname") must /("host" -> "hostname")
    }
    // Missing check that the detailed output is correct
  }

  "Completed diagnostic that has a timeout" should {
    "have a message that reflects this" in {
      new CompletedDiagnostic(Seq(DiagnosticSuccess("successCheck", Duration.create(2, "s")), DiagnosticTimeout("checkName", Duration.create(2, "s")))).asText("hostname") ===
      "ERROR on (hostname) due to\ncheckName timed out"
    }
    // Missing check that the detailed output is correct
  }

  "executeChecks" should {
    "be able to complete multiple checks" in {
      val check1 = BlockingDiagnosticCheckDefinition("foo", Duration("2s"), () => CheckSucceeded)
      val check2 = BlockingDiagnosticCheckDefinition("foo1", Duration("2s"), () => CheckSucceeded)
      val check3 = AsyncDiagnosticCheckDefinition("foo2", Duration("2s"), () => Future.successful(CheckSucceeded))

      executeChecks(Seq(check1, check2, check3)).successful === true
    }
    "deal with a failing check" in {
      val check1 = new BlockingDiagnosticCheckDefinition("foo", Duration("2s"), () => CheckSucceeded)
      val check2 = new AsyncDiagnosticCheckDefinition("foo1", Duration("2s"), () => Future.successful(CheckSucceeded))
      val check3 = new BlockingDiagnosticCheckDefinition("foo2", Duration("2s"), () => CheckFailed("some reason"))

      executeChecks(Seq(check1, check2, check3)).successful === false
    }

  }

  "AsyncDiagnosticCheckDefinition" should {
    "default the timeout to 3 seconds" in {
      AsyncDiagnosticCheckDefinition(name = "foo", check = (() => Future.successful(CheckSucceeded))).timeout === Duration.create(3, "s")
    }
  }

  "BlockingDiagnosticCheckDefinition" should {
    "default the timeout to 3 seconds" in {
      BlockingDiagnosticCheckDefinition(name = "foo", check = (() => CheckSucceeded)).timeout === Duration.create(3, "s")
    }
  }

}