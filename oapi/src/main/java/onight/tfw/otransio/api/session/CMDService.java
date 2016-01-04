package onight.tfw.otransio.api.session;

import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

public interface CMDService {

	String[] getCmds();

	public void onPacket(FramePacket pack, CompleteHandler handler);

	String getModule();
}
