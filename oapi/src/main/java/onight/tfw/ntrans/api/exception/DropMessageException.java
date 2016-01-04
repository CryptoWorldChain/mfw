package onight.tfw.ntrans.api.exception;

/**
 * 放弃消息
 * @author brew
 *
 */
public class DropMessageException extends MessageException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DropMessageException() {
		super();
	}

	
	public DropMessageException(String message, Throwable cause) {
		super(message, cause);
	}

	public DropMessageException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public DropMessageException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
