package org.fc.hzq.orcl.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CallBack;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.oparam.api.OPFace;
import onight.tfw.oparam.api.OTreeValue;

@Slf4j
public class WatchableHashParam implements OPFace {

	OTreeValue root = new OTreeValue("", "", new ArrayList<OTreeValue>(), 0, 0);

	@AllArgsConstructor
	public class WatchInfo {
		CallBack<OTreeValue> cb;
		boolean watchOnce = false;
	};

	HashMap<String, List<WatchInfo>> wathKeys = new HashMap<>();

	@Override
	public void setDaosupport(DomainDaoSupport support) {

	}

	@Override
	public DomainDaoSupport getDaosupport() {
		return null;
	}

	ServiceSpec ss = new ServiceSpec("localparam");

	@Override
	public ServiceSpec getServiceSpec() {
		return ss;
	}

	@Override
	public String getDomainName() {
		return "localparam";
	}

	@Override
	public Class<?> getDomainClazz() {
		return String.class;
	}

	@Override
	public String getHealth() {
		return "OK";
	}

	public void notifyWatchers(String key, OTreeValue v) {

		String fullpath = "";
		for (String path : key.replaceFirst("/", "").split("/")) {
			fullpath = fullpath + "/" + path;
			List<WatchInfo> lst = wathKeys.get(fullpath);
			if (lst == null) {
				lst = wathKeys.get(fullpath + "/");
			}
			if (lst != null) {
				log.debug("notifyWatchers::" + key + "," + lst);
				for (WatchInfo i : lst) {
					i.cb.onSuccess(v);
				}
			}

		}
		// System.out.println("notifyWatchers::" + key);

	}

	@Override
	public synchronized Future<OTreeValue> put(String key, String value) throws IOException {
		return put(key, value, root);
	}

	public synchronized Future<OTreeValue> put(String key, String value, OTreeValue tree) throws IOException {

		String[] paths = key.replaceFirst("/", "").split("/");
		OTreeValue subtree = null;
		if (tree.getNodes() == null) {
			tree.setNodes(new ArrayList<OTreeValue>());

		}
		for (OTreeValue checknode : tree.getNodes()) {
			if (StringUtils.equals(checknode.getKey(), tree.getKey() + "/" + paths[0])) {
				subtree = checknode;
				break;
			}
		}
		if (subtree == null) {
			subtree = new OTreeValue();
			subtree.setKey(tree.getKey() + "/" + paths[0]);
			tree.getNodes().add(subtree);
		}

		if (paths.length > 1) {
			Future<OTreeValue> ret = put(key.substring(paths[0].length() + 1), value, subtree);
			notifyWatchers(subtree.getKey(), subtree);
			return ret;
		} else {
			log.debug("put tree:" + subtree.getKey() + ",value=" + value);
			subtree.setValue(value);
		}
		notifyWatchers(subtree.getKey(), subtree);
		return ConcurrentUtils.constantFuture(new OTreeValue(key, value, null, 0, 0));
	}

	@Override
	public synchronized Future<OTreeValue> compareAndSwap(String key, String value, String compareValue) throws IOException {
		// synchronized (map) {
		// String v = map.get(key);
		// if (StringUtils.equals(v, compareValue))
		// return put(key, value);
		// }
		return null;
	}

	@Override
	public Future<OTreeValue> compareAndDelete(String key, String compareValue) throws IOException {
		// synchronized (map) {
		// String v = map.get(key);
		// if (StringUtils.equals(v, compareValue)) {
		// map.remove(key);
		// notifyWatchers(key, new OTreeValue(key, v, null, 0, 0));
		// return ConcurrentUtils.constantFuture(new OTreeValue(key, v, null, 0,
		// 0));
		// }
		// }
		return null;
	}

	@Override
	public Future<OTreeValue> putDir(String dir) throws IOException {
		return put(dir, "");
	}

	@Override
	public Future<OTreeValue> post(String key, String value) throws IOException {
		return put(key, value);
	}

	@Override
	public Future<OTreeValue> delete(String key) throws IOException {
		// synchronized (map) {
		// String v = map.get(key);
		// if (v != null) {
		// map.remove(key);
		// notifyWatchers(key, new OTreeValue(key, v, null, 0, 0));
		// return ConcurrentUtils.constantFuture(new OTreeValue(key, v, null, 0,
		// 0));
		// }
		// }
		return null;
	}

	@Override
	public Future<OTreeValue> deleteDir(String dir) throws IOException {
		return delete(dir);
	}

	public synchronized Future<OTreeValue> get(String key, OTreeValue tree) throws IOException {
		String[] paths = key.replaceFirst("/", "").split("/");
		// System.out.println("get:" + key + ",path=" + paths[0] + ",@" + tree);
		if (paths.length == 1 || paths[1].length() == 0) {
			if (tree.getNodes() != null) {
				if (key.endsWith("/")) {
					key = key.substring(0, key.length() - 1);
				}
				for (OTreeValue tv : tree.getNodes()) {
					if (tv.getKey().endsWith(key)) {
						// found!
						return ConcurrentUtils.constantFuture(tv);
					}
				}
			}
			return null;
		} else {
			if (tree.getNodes() != null) {
				for (OTreeValue tv : tree.getNodes()) {
					if (tv.getKey().endsWith(paths[0])) {
						// found!!
						return get(key.substring(paths[0].length() + 1), tv);
					}
				}
			}
			return null;
		}
	}

	@Override
	public synchronized Future<OTreeValue> get(String key) throws IOException {
		return get(key, root);
		// synchronized (map) {
		// String v = map.get(key);
		// if (v != null) {
		// return ConcurrentUtils.constantFuture(new OTreeValue(key, v, null, 0,
		// 0));
		// }
		// }
	}

	@Override
	public Future<OTreeValue> getDir(String dir) throws IOException {
		return get(dir);
	}

	@Override
	public Future<OTreeValue> getAll() throws IOException {
		throw new RuntimeException("NOT_SUPPORT");
	}

	@Override
	public void watchOnce(String key, CallBack<OTreeValue> cb) {
		watch(key, cb, false);
	}

	@Override
	public synchronized void watch(String key, CallBack<OTreeValue> cb, boolean always) {
		// throw new RuntimeException("NOT_SUPPORT");
		List<WatchInfo> watchlist = wathKeys.get(key);
		if (watchlist == null) {
			watchlist = new ArrayList<>();
			wathKeys.put(key, watchlist);
		}
		watchlist.add(new WatchInfo(cb, always));
	}

	public static void main(String[] args) {
		WatchableHashParam p = new WatchableHashParam();

		p.watch("/zippo/member", new CallBack<OTreeValue>() {

			@Override
			public void onFailed(Exception arg0, OTreeValue arg1) {
				System.out.println("faile:" + arg0);
			}

			@Override
			public void onSuccess(OTreeValue arg0) {
				System.out.println("onSuccess:" + arg0);
			}

		}, true);
		System.out.println("wathKeys=" + p.wathKeys);
		try {
			p.put("/zippo/members/vws", "abcf123s");
			p.put("/zippo/nds/vws", "1112344");
			System.out.println("root=" + p.root);
			Future<OTreeValue> v = p.get("/zippo/members/");
			if (v != null && v.get() != null) {
				System.out.println("v==" + v.get());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
