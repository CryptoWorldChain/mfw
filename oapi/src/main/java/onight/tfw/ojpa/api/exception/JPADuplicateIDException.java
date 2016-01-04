package onight.tfw.ojpa.api.exception;

public class JPADuplicateIDException extends RuntimeException {

	private static final long serialVersionUID = 3129124709055457388L;

	public JPADuplicateIDException(String message) {
		super(message);
	}

	public JPADuplicateIDException(String message, Throwable cause) {
		super(message, cause);
	}

	public JPADuplicateIDException(Throwable cause) {
		super(cause);
	}

}
