package onight.zippo.oparam.etcd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;
import onight.tfw.oparam.api.OTreeValue;

public class FutureWP implements Future<OTreeValue> {

	EtcdResponsePromise<EtcdKeysResponse> promise;
	boolean isCancelled = false;

	public FutureWP(EtcdResponsePromise<EtcdKeysResponse> promise) {
		super();
		this.promise = promise;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return promise.getNettyPromise().cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return promise.getNettyPromise().isCancelled();
	}

	@Override
	public boolean isDone() {
		return promise.getNettyPromise().isDone();
	}

	public static List<OTreeValue> getTrees(List<EtcdNode> nodes){
		if(nodes!=null&&nodes.size()>0){
			List<OTreeValue> tnodes=new ArrayList<>();
 			for(EtcdNode node:nodes){
				OTreeValue tree=new OTreeValue(node.key,node.value,getTrees(node.nodes),0,0);
				tnodes.add(tree);
			}
 			return tnodes;
		}
		return null;
	}
	@Override
	public OTreeValue get() throws InterruptedException, ExecutionException {
		try {
			EtcdKeysResponse response=promise.get();
			return new OTreeValue(response.getNode().key,response.getNode().value,getTrees(response.getNode().nodes),response.getNode().modifiedIndex,response.getNode().createdIndex);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public OTreeValue get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		EtcdKeysResponse response=promise.getNettyPromise().get(timeout, unit);
		return new OTreeValue(response.getNode().key,response.getNode().value,getTrees(response.getNode().nodes),response.getNode().modifiedIndex,response.getNode().createdIndex);

	}

}
