package onight.tfw.otransio.api.session;

import lombok.Data;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

@Data
public class PSession {

	String mmid;

	/**
	 * 
	 * @param pack消息
	 * @param handler
	 * @return 后处理
	 */
	public void onPacket(FramePacket pack,CompleteHandler handler) {
		handler.onFinished(pack);
	}

	
}
