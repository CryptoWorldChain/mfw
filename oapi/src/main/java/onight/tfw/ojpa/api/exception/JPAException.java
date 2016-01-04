package onight.tfw.ojpa.api.exception;

public class JPAException extends RuntimeException {

	private static final long serialVersionUID = 3129124709055457388L;

	public JPAException(String message) {
		super(message);
	}

	public JPAException(String message, Throwable cause) {
		super(message, cause);
	}

	public JPAException(Throwable cause) {
		super(cause);
	}

}
