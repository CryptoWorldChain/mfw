package onight.tfw.ntrans.osgi;

import java.util.ArrayList;
import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.ActWrapper;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.FilterManager;
import onight.tfw.otransio.api.PacketFilter;
import onight.tfw.otransio.api.beans.FramePacket;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.felix.ipojo.annotations.Unbind;

@iPojoBean
@Instantiate(name = "filterManager")
@Provides(specifications = ActorService.class, properties = { @StaticServiceProperty(name = "name", value = "filterManager", type = "java.lang.String") })
@Slf4j
public class FilterManagerImpl implements FilterManager, ActorService {
	HashMap<String, ArrayList<PacketFilter>> filterByModule = new HashMap<String, ArrayList<PacketFilter>>();

	// ArrayList<PacketFilter> listeners = new ArrayList<>();

	@Bind(aggregate = true, optional = true)
	public void bindProc(PacketFilter pl) {
		for (String module : pl.modules()) {
			ArrayList<PacketFilter> list = filterByModule.get(module);
			if (list == null) {
				synchronized (filterByModule) {
					list = filterByModule.get(module);
					if (list == null) {
						list = new ArrayList<PacketFilter>();
						filterByModule.put(module, list);
					}
				}
			}
			if (!list.contains(pl)) {
				list.add(pl);
				log.info("Register PacketListern::" + pl + ": module:" + module);
			}
		}
	}

	@Unbind(aggregate = true, optional = true)
	public void unbindProc(PacketFilter pl) {
		if(pl!=null&&pl.modules()!=null)
		for (String module : pl.modules()) {
			ArrayList<PacketFilter> list = filterByModule.get(module);
			if (list != null && list.remove(pl)) {
				log.info("Remove PacketListern::" + pl + ": module:" + module);
			}
		}
	}

	@Override
	public boolean preRouteListner(ActWrapper actor, FramePacket pack, CompleteHandler handler) {
		String module = actor.getModule();
		ArrayList<PacketFilter> listeners = filterByModule.get(module);
		if (listeners != null) {
			for (PacketFilter pl : listeners) {
				if (pl.preRoute(actor, pack, handler)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean postRouteListner(ActWrapper actor, FramePacket pack, CompleteHandler handler) {
		String module = actor.getModule();
		ArrayList<PacketFilter> listeners = filterByModule.get(module);
		if (listeners != null) {
			for (PacketFilter pl : listeners) {
				if (pl.postRoute(actor, pack, handler)) {
					return true;
				}
			}
		}
		return false;
	}
}
