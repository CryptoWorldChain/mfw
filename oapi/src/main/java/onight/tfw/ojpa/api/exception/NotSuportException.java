package onight.tfw.ojpa.api.exception;

public class NotSuportException extends JPAException {


	/**
	 * 
	 */
	private static final long serialVersionUID = 6138888455843147859L;

	public NotSuportException(String message) {
		super(message);
	}

	public NotSuportException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotSuportException(Throwable cause) {
		super(cause);
	}

}
