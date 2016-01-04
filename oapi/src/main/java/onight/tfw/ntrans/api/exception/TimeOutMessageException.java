package onight.tfw.ntrans.api.exception;

/**
 * 放弃消息
 * @author brew
 *
 */
public class TimeOutMessageException extends MessageException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TimeOutMessageException() {
		super();
	}

	
	public TimeOutMessageException(String message, Throwable cause) {
		super(message, cause);
	}

	public TimeOutMessageException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public TimeOutMessageException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
