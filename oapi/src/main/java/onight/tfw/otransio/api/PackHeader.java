package onight.tfw.otransio.api;

import onight.tfw.mservice.NodeHelper;

public class PackHeader {
	public final static String EXT_HIDDEN = "_h";

	public final static String Set_COOKIE = "Set-Cookie";

	public final static String EXT_IGNORE = EXT_HIDDEN + "_ign";
	
	public final static String EXT_IGNORE_FORWARD = EXT_IGNORE + "f";
	public final static String EXT_IGNORE_RESPONSE = EXT_IGNORE + "b";

	public final static String PEER_IP = EXT_IGNORE_RESPONSE + "_peerip";
	// public final static String ENC = "enc";
	// public final static String CMD = "cmd";
	// public final static String FROM = "from";

	public static final String CookieDomain = NodeHelper.getPropInstance().get("http.cookie.domain", "tfw.imagetop.cn");
	public static final int CookieExpire = NodeHelper.getPropInstance().get("http.cookie.expire", 24*3600);
	// public final static String SIZE = "size";
	// public final static String TYPE = "type";// iq-->需要回复，message-->不需要回复
	// public final static String PROXY = "proxy";
	// public final static String SUBCMD = "subcmd";

	public final static String TO = "to";
	public final static String CMD_HB = "HBL";

	public final static String REMOTE_LOGIN = "RLG";
	public final static String REMOTE_MODULE = "***";

	public final static String IQ = "iq";
	public final static String MESSAGE = "message";

	// public final static String SESSION = EXT_IGNORE+"SSM";

	public final static String HTTP_PARAM_FIX_HEAD = "fh";
	public final static String HTTP_PARAM_BODY_DATA = "bd";
	public final static String GCMD = "gcmd";

//	public final static String H_IGN_GCMD = EXT_IGNORE_RESPONSE + "_gcmd";

	public final static byte[] EMPTY_BYTES = new byte[] {};

}
