import com.typesafe.sbt.SbtNativePackager.packageArchetype

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

packageArchetype.java_application

EclipseKeys.withSource := true

name := "netty-boilerplate"

version := scala.util.Properties.envOrElse("APP_VERSION", "snapshot")

scalaVersion := "2.11.6"

sbtVersion := "0.13.5"

libraryDependencies ++= Seq(
  "net.databinder"     %% "unfiltered-netty-server" % "0.8.4",
  "net.databinder"     %% "unfiltered-directives"   % "0.8.4",
  "net.databinder"     %% "unfiltered-filter"       % "0.8.4",
  "io.argonaut"        %% "argonaut"                % "6.1-M4",
  "io.argonaut"        %% "argonaut-unfiltered"     % "6.0.4",
  "commons-lang"       %  "commons-lang"            % "2.6",
  "commons-codec"      %  "commons-codec"           % "1.6",
  "org.slf4j"          %  "jul-to-slf4j"            % "1.7.7",
  "ch.qos.logback"     %  "logback-classic"         % "1.1.2",
  "org.specs2"         %% "specs2-core"             % "3.4" % "test",
  "org.specs2"         %% "specs2-matcher-extra"    % "3.4" % "test",
  "org.specs2"         %% "specs2-junit"            % "3.4" % "test",
  "org.specs2"         %% "specs2-html"             % "3.4" % "test",
  "org.specs2"         %% "specs2-scalacheck"       % "3.4" % "test",
  "org.scalacheck"     %% "scalacheck"              % "1.12.2" % "test",
  "net.databinder.dispatch" %% "dispatch-core"      % "0.11.2" % "test"
)

net.virtualvoid.sbt.graph.Plugin.graphSettings

scalacOptions ++= Seq(
  "-Xfatal-warnings", 
  "-deprecation", 
  "-unchecked",
  "-feature",
  "-language:higherKinds")


testOptions in Test ++= Seq(
  Tests.Argument("junitxml", "html", "console", "!pandoc"),
  Tests.Setup( () => System.setProperty("mode", "test"))
)

// Overrides the "mainclass" setting in the "Compile" configuration
mainClass in Compile := Some("com.thoughtworks.sancoder.Main") //Used in Universal packageBin

// Overrides the "mainClass" setting in the "Compile" configuration, only during the "run" task
mainClass in (Compile, run) := Some("com.thoughtworks.sancoder.Dev") //Used from normal sbt

initialCommands += """
    import argonaut._, Argonaut._;
"""
