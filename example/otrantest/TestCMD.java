package onight.osgi.otransio.example;

import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.pb.test.Test1;
import onight.tfw.otransio.api.CMDType;
import onight.tfw.otransio.api.OPacket;
import onight.tfw.otransio.api.SocketMessageListener;
import onight.tfw.proxy.ActorProxy;

import org.apache.felix.ipojo.annotations.Provides;

@iPojoBean
@Provides(specifications = { SocketMessageListener.class })
@Slf4j
public class TestCMD extends ActorProxy<Test1.loginpack> {

	@Override
	public CMDType getCMDType() {
		return new CMDType("hello", 0, Test1.loginpack.class);
	}

	@Override
	public OPacket onSocketObject(OPacket pack, Test1.loginpack pbo) {
		log.debug("获取到消息：" + pack);
		return null;
	}

}
