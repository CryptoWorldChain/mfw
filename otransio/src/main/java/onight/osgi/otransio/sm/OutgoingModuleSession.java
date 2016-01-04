package onight.osgi.otransio.sm;

import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.sm.RemoteModuleBean.ModuleBean;
import onight.tfw.outils.serialize.SerializerUtil;

import org.apache.commons.lang3.SerializationUtils;
import org.glassfish.grizzly.Connection;

@Slf4j
public class OutgoingModuleSession extends RemoteModuleSession {

	
	public OutgoingModuleSession(String moduleid, String remoteNodeID, MSessionSets mss) {
		super(moduleid, remoteNodeID, mss);
	}

	public RemoteModuleSession removeConnection(Connection<?> conn) {
		connsPool.removeObject(conn);
		if (connsPool.size() <= 0) {
			log.info("Remove RemoteModule Session:" + module + ",@" + this);
		}
		return this;
	}

	public static void main(String[] args) {
		RemoteModuleBean rmb=new RemoteModuleBean();
		rmb.modules.add(new ModuleBean("aaa", "bbb"));
		rmb.modules.add(new ModuleBean("aaa1", "bbb2"));
		
		Object bo=SerializerUtil.serialize(rmb);
		System.out.println(bo);
		byte bb[]=SerializerUtil.toBytes(SerializerUtil.serialize(rmb));
		System.out.println("bb="+bb.length);
		System.out.println("bmbmap="+SerializerUtil.serialize(rmb));
		RemoteModuleBean trmb=SerializerUtil.deserialize(
				SerializerUtil.fromBytes(bb), RemoteModuleBean.class);
		System.out.println(trmb);
	}
}
