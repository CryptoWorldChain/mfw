package onight.zippo.oparam.etcd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import onight.tfw.oparam.api.OTreeValue;
import onight.tfw.outils.serialize.JsonSerializer;

public class FutureBrew implements Future<OTreeValue> {

	boolean isCancelled = false;

	MEtcdKeysResponse response;

	public FutureBrew(String result) {
		super();
		if (result != null) {
			try {
				response = JsonSerializer.getInstance().deserialize(result, MEtcdKeysResponse.class);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	public static List<OTreeValue> getTrees(List<MEtcdNode> nodes) {
		if (nodes != null && nodes.size() > 0) {
			List<OTreeValue> tnodes = new ArrayList<>();
			for (MEtcdNode node : nodes) {
				OTreeValue tree = new OTreeValue(node.key, node.value, getTrees(node.nodes),node.getModifiedIndex(),node.getCreatedIndex());
				tnodes.add(tree);
			}
			return tnodes;
		}
		return null;
	}

	@Override
	public OTreeValue get() throws InterruptedException, ExecutionException {
		try {
			if (response != null)
				return new OTreeValue(response.getNode().key, response.getNode().value,
						getTrees(response.getNode().nodes),response.getNode().getModifiedIndex(),response.getNode().getCreatedIndex());
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public OTreeValue get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return get();
	}

}
