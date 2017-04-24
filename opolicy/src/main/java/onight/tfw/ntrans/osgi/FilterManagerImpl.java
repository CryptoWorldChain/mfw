package onight.tfw.ntrans.osgi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.felix.ipojo.annotations.Unbind;
import org.fc.zippo.filter.FilterConfig;
import org.osgi.framework.BundleContext;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.ActWrapper;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.FilterManager;
import onight.tfw.otransio.api.PacketFilter;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;

@Component
@Instantiate(name = "filterManager")
@Provides(specifications = ActorService.class, properties = {
		@StaticServiceProperty(name = "name", value = "filterManager", type = "java.lang.String") })
@Slf4j
public class FilterManagerImpl implements FilterManager, ActorService {

	BundleContext btx;
	PropHelper helper;

	public FilterManagerImpl(BundleContext btx) {
		this.btx = btx;
		helper = new PropHelper(btx);
	}

	HashMap<String, ArrayList<PacketFilter>> filterByModule = new HashMap<String, ArrayList<PacketFilter>>();
	ArrayList<PacketFilter> globalFilters = new ArrayList<>();

	@Bind(aggregate = true, optional = true)
	public void bindProc(PacketFilter pl) {
		if (pl.modules() == null) {
			if (!globalFilters.contains(pl)) {
				synchronized (globalFilters) {
					pl.init(new FilterConfig(btx, helper));
					globalFilters.add(pl);
					Collections.sort(globalFilters, new Comparator<PacketFilter>() {

						@Override
						public int compare(PacketFilter o1, PacketFilter o2) {
							return o1.getPriority() - o2.getPriority();
						}

					});
					log.info("Register Global PacketListern::" + pl);
				}
			}
		} else {
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
					Collections.sort(list, new Comparator<PacketFilter>() {

						@Override
						public int compare(PacketFilter o1, PacketFilter o2) {
							return o1.getPriority() - o2.getPriority();
						}

					});
					log.info("Register PacketListern::" + pl + ": module:" + module);
				}
			}
		}
	}

	@Unbind(aggregate = true, optional = true)
	public void unbindProc(PacketFilter pl) {
		if (pl != null && pl.modules() != null) {
			for (String module : pl.modules()) {
				ArrayList<PacketFilter> list = filterByModule.get(module);
				if (list != null && list.remove(pl)) {
					log.info("Remove PacketListern::" + pl + ": module:" + module);
				}
			}
		} else {
			if (pl != null) {
				globalFilters.remove(pl);
				pl.destroy(new FilterConfig(btx, helper));
			}
		}
	}

	@Override
	public boolean preRouteListner(ActWrapper actor, FramePacket pack, CompleteHandler handler) {
		String module = actor.getModule();
		ArrayList<PacketFilter> listeners = filterByModule.get(module);
		if (listeners != null) {
			for (PacketFilter pl : listeners) {
				if (!pl.preRoute(actor, pack, handler)) {
					return false;
				}
			}
		}
		for (PacketFilter pl : globalFilters) {
			if (!pl.preRoute(actor, pack, handler)) {
				return false;
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
				pl.postRoute(actor, pack, handler) ;
			}
		}
		for (PacketFilter pl : globalFilters) {
			pl.postRoute(actor, pack, handler);
		}

		return false;
	}
}
