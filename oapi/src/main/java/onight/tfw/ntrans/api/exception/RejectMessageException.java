package onight.tfw.ntrans.api.exception;

import lombok.extern.slf4j.Slf4j;


/**
 * 消息处理失败，仍然放回到队列中
 * @author brew
 *
 */
@Slf4j
public class RejectMessageException  extends MessageException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RejectMessageException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public RejectMessageException(String message, Throwable cause) {
		super(message, cause);
	}

	public RejectMessageException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public RejectMessageException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
