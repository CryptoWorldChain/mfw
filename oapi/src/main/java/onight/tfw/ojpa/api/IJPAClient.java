package onight.tfw.ojpa.api;

public interface IJPAClient {

	public abstract void onDaoServiceReady(OJpaDAO<?> dao);
	
	public abstract void onDaoServiceAllReady();

}
