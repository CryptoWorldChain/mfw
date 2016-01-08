package onight.oapi.scala.traits

import com.google.protobuf.Message
import onight.tfw.ntrans.api.PBActor

abstract class OPBActor extends PBActor[Message] with OLog {

} 