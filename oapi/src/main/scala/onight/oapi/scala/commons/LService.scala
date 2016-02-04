package onight.oapi.scala.commons

import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.beans.FramePacket
import com.google.protobuf.Message

trait LService[T] {
  def onPBPacket(pack: FramePacket, pbo: T, handler: CompleteHandler);
  def cmd: String = "___";
}

object NoneLService extends LService[Message] {
  override def onPBPacket(pack: FramePacket, pbo: Message, handler: CompleteHandler) = {
    handler.onFinished(pack);
  }
  override def cmd: String = "___";

}
//object LService {
//  def apply() = new NoneLService();
//
//}