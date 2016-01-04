package onight.oapi.scala.traits

import org.slf4j.LoggerFactory
import org.slf4j.Logger

trait OLog {
  lazy val log = LoggerFactory.getLogger(getClass)
  implicit def logging2Logger(anything: OLog): Logger = anything.log
}