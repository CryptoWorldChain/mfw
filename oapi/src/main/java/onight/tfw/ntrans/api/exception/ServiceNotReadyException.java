package onight.tfw.ntrans.api.exception;

/**
 * 放弃消息
 * @author brew
 *
 */
public class ServiceNotReadyException extends MessageException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ServiceNotReadyException() {
		super();
	}

	
	public ServiceNotReadyException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServiceNotReadyException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public ServiceNotReadyException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
