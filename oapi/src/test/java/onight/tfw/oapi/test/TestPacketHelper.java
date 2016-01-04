package onight.tfw.oapi.test;

import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.beans.TimeOutExceptionBody;


public class TestPacketHelper {

	public static void main(String[] args) {
		TimeOutExceptionBody body=new TimeOutExceptionBody("ooooo", null);
		FramePacket pack=PacketHelper.genSyncPack("ABC", "BBB", body);
		
		String json=PacketHelper.toJsonString(pack);
		System.out.println("pack="+pack);

		System.out.println("json="+json);
		
		FramePacket pack2=PacketHelper.buildPacketFromJson(json.getBytes(),TimeOutExceptionBody.class);
		System.out.println("pack2="+pack2);
		
	}
}
