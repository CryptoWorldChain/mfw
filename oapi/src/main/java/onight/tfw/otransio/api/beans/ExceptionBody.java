package onight.tfw.otransio.api.beans;

import javax.servlet.http.HttpServletResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ExceptionBody extends FrameBody {

	private String errCode;
	private String errMsg;

	public ExceptionBody(String errCode, String errMsg) {
		super();
		this.errCode = errCode;
		this.errMsg = errMsg;
	}

	public static String EC_NOBODYRETURN = HttpServletResponse.SC_NO_CONTENT + "";// "204";
	public static String EC_UNKNOW_SERAILTYPE = HttpServletResponse.SC_BAD_REQUEST + "";
	public static String EC_UNAUTHORIZED = HttpServletResponse.SC_UNAUTHORIZED + "";
	public static String EC_SERVICE_EXCEPTION = HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "";
	public static String EC_FILTER_EXCEPTION = HttpServletResponse.SC_NOT_ACCEPTABLE + "";
	public static String EC_EXPECTATION_FAILED = HttpServletResponse.SC_EXPECTATION_FAILED + "";

}
