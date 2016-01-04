package onight.tfw.ojpa.api;

public interface CASExecutor {
	public Object doInTransaction();

	public Object lockBeforeExec();
}
