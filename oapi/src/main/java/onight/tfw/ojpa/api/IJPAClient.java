package onight.tfw.ojpa.api;

public interface IJPAClient {

	public abstract void onDaoServiceReady(DomainDaoSupport dao);
	
	public abstract void onDaoServiceAllReady();

}
