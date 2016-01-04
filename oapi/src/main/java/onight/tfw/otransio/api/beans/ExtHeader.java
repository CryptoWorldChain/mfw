package onight.tfw.otransio.api.beans;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.otransio.api.ActorSession;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.outils.serialize.HttpHelper;
import onight.tfw.outils.serialize.SerializerUtil;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Data
public class ExtHeader {
	byte[] data;

	public final static String SPLIT_CHAR = "&";
	public final static String EQUAL_CHAR = "=";
	// public final static String HTTP_COOKIE_NAME = "__exth";
	public final static String SESSIONID = PackHeader.EXT_HIDDEN + "_smid";
	public final static String PACK_SESSION = PackHeader.EXT_IGNORE + "__session";

	Map<String, Object> hiddenkvs = new HashMap<String, Object>();
	Map<String, Object> ignorekvs = new HashMap<String, Object>();
	Map<String, Object> vkvs = new HashMap<String, Object>();

	private ExtHeader(byte[] data, int offset, int len) {
		appendFrom(data, offset, len);
		genBytes();
	}

	public ExtHeader() {
	}

	public void appendFrom(byte[] data) {
		appendFrom(data, 0, data.length);
	}

	public String getSMID() {
		Object obj = get(PACK_SESSION);
		if (obj != null) {
			return ((ActorSession) obj).getSmid();
		}
		return null;
	}

	public void appendFrom(byte[] data, int offset, int len) {
		if (data == null) {
			return;
		}
		try {
			String strdata = new String(data, offset, len, "UTF-8");
			if (StringUtils.isNoneBlank(strdata)) {
				for (String strkv : strdata.split(SPLIT_CHAR)) {
					String kv[] = strkv.split(EQUAL_CHAR);
					if (kv.length == 2) {// 正常kv
						append(kv[0], kv[1]);
					} else if (kv.length == 1) {// 仅仅就是设置
						append(kv[0], "1");
					} else {
						log.trace("Unknow ext header:size=" + kv.length + ",str=" + strkv + ",data=" + data);
					}
				}
			}
		} catch (UnsupportedEncodingException e) {
			log.warn("UnsupportedEncodingException：" + data, e);
		}
	}

	public Object append(String key, Object value) {
		return getMap(key).put(key, value);
	}

	public Object remove(String key) {
		return getMap(key).remove(key);
	}

	public void reset() {
		this.data = null;
	}

	public HashMap<String, Object> visibleMap() {
		HashMap<String, Object> ret = new HashMap<>();
		return ret;
	}

	public Map<String, Object> getMap(String key) {
		if (key.startsWith(PackHeader.EXT_HIDDEN)) {
			return hiddenkvs;
		}
		if (key.startsWith(PackHeader.EXT_IGNORE)) {
			return ignorekvs;
		}
		return vkvs;
	}

	public byte[] genBytes() {
		if (vkvs.size() == 0)
			return PackHeader.EMPTY_BYTES;
		if (data != null) {
			return data;
		}
		StringBuffer sb = new StringBuffer();
		for (Entry<String, Object> pair : vkvs.entrySet()) {
			if (pair.getValue() != null && pair.getValue() instanceof String) {
				sb.append(pair.getKey()).append(EQUAL_CHAR).append(pair.getValue()).append(SPLIT_CHAR);
			}
		}
		for (Entry<String, Object> pair : hiddenkvs.entrySet()) {
			if (pair.getValue() != null && pair.getValue() instanceof String) {
				sb.append(pair.getKey()).append(EQUAL_CHAR).append(pair.getValue()).append(SPLIT_CHAR);
			}
		}
		try {
			data = sb.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.warn("UnsupportedEncodingException：" + data, e);
		}
		return data;
	}

	public byte[] genHiddenBytes() {
		if (hiddenkvs.size() == 0)
			return PackHeader.EMPTY_BYTES;
		StringBuffer sb = new StringBuffer();
		for (Entry<String, Object> pair : hiddenkvs.entrySet()) {
			if (pair.getValue() != null && !pair.getKey().startsWith(PackHeader.EXT_HIDDEN) && pair.getValue() instanceof String) {
				sb.append(pair.getKey()).append(EQUAL_CHAR).append(pair.getValue()).append(SPLIT_CHAR);
			}
		}
		try {
			return sb.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.warn("UnsupportedEncodingException：" + sb.toString(), e);
		}
		return null;
	}

	public boolean isExist(String key) {
		return hiddenkvs.containsKey(key) || vkvs.containsKey(key) || ignorekvs.containsKey(key);
	}

	public Object get(String key) {
		return getMap(key).get(key);
	}

	public static ExtHeader buildFrom(byte[] data) {
		return new ExtHeader(data, 0, data.length);
	}

	public static ExtHeader buildFrom(byte[] data, int offset, int len) {
		return new ExtHeader(data, offset, len);
	}

	public static ExtHeader buildFrom(HttpServletRequest req) {
		ExtHeader ret = new ExtHeader();
		for (Map.Entry<String, String[]> kv : req.getParameterMap().entrySet()) {
			if (!kv.getKey().equals(PackHeader.HTTP_PARAM_FIX_HEAD) && !kv.getKey().equals(PackHeader.HTTP_PARAM_BODY_DATA)) {
				// if (PackHeader.GCMD.equals(kv.getKey())) {
				// ret.append(PackHeader.H_IGN_GCMD, kv.getValue()[0]);
				// } else {
				ret.append(kv.getKey(), kv.getValue()[0]);
				// }
			}
		}
		if (req.getCookies() != null) {
			for (Cookie ck : req.getCookies()) {
				// if (HTTP_COOKIE_NAME.equals(ck.getName())) {
				// BASE64Decoder decoder = new BASE64Decoder();
				// try {
				// ret.appendFrom(decoder.decodeBuffer(ck.getValue()));
				// } catch (IOException e) {
				// }
				// } else
				if (SESSIONID.equals(ck.getName())) {
					ret.append(SESSIONID, ck.getValue());
				} else {
					ret.append(ck.getName(), ck.getValue());
				}
			}
		}
		ret.append(PackHeader.PEER_IP, HttpHelper.getIpAddr(req));
		return ret;
	}

	public static void addCookie(HttpServletResponse res, String key, Object value) {
		if (value != null) {
			Cookie cookie = null;
			if (value instanceof CookieBean) {
				CookieBean cb = (CookieBean) value;
				if (cb.getValue() instanceof String && StringUtils.isNotBlank((String) cb.getValue())) {
					cookie = new Cookie(key, (String) cb.getValue());
				} else {
					cookie = (new Cookie(key, Base64.encodeBase64URLSafeString(SerializerUtil.toBytes(cb.getValue()))));
				}
				// cookie.setDomain(PackHeader.CookieDomain);
				// cookie.setHttpOnly(true);
				cookie.setMaxAge(cb.getExpiry());
			} else if (value instanceof String && StringUtils.isNotBlank((String) value)) {
				cookie = new Cookie(key, (String) value);
			} else {
				cookie = (new Cookie(key, Base64.encodeBase64URLSafeString(SerializerUtil.toBytes(value))));
			}

			res.addCookie(cookie);

		}
	}

	public void buildFor(HttpServletResponse res) {
		addCookie(res, "_" + PackHeader.HTTP_PARAM_FIX_HEAD, get(PackHeader.HTTP_PARAM_FIX_HEAD));
		for (Entry<String, Object> pair : hiddenkvs.entrySet()) {
			if (pair.getKey().startsWith(PackHeader.EXT_HIDDEN) && !pair.getKey().startsWith(PackHeader.EXT_IGNORE_RESPONSE)) {
				addCookie(res, pair.getKey(), pair.getValue());
			}
		}
	}

}
