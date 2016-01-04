package onight.oapi.scala.traits

import onight.tfw.ntrans.api.PBActor
import com.google.protobuf.Message

abstract class OPBActor extends PBActor[Message] with OLog {

}