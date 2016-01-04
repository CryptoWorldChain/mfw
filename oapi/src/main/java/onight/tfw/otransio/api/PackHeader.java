package onight.tfw.otransio.api;

public class PackHeader {
	public final static String PEER_IP = "peerip";
	// public final static String ENC = "enc";
	// public final static String CMD = "cmd";
	// public final static String FROM = "from";

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
	public final static String EXT_HIDDEN = "_h";
	public final static String EXT_IGNORE = EXT_HIDDEN+"_ign_";

	// public final static String SESSION = EXT_IGNORE+"SSM";

	public final static String HTTP_PARAM_FIX_HEAD = "fh";
	public final static String HTTP_PARAM_BODY_DATA = "bd";

	public final static byte[] EMPTY_BYTES = new byte[] {};

}
