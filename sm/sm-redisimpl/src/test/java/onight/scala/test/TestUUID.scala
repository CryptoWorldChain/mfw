package onight.scala.test

import onight.tfw.outils.serialize.UUIDGenerator

object TestUUID {

  def main(args: Array[String]): Unit = {
    println(UUIDGenerator.generate().length())
  }
}