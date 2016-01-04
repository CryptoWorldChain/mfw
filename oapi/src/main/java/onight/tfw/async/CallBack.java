package onight.tfw.async;

public interface CallBack<V> {

	public void onSuccess(V v);

	public void onFailed(Exception e, V v);
	
	
}
