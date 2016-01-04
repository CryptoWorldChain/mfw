package onight.tfw.otransio.api.beans;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.HttpHelper;

import org.apache.commons.lang3.StringUtils;

import sun.misc.BASE64Decoder;

@Slf4j
@Data
public class ExtHeader {
	byte[] data;

	public final static String SPLIT_CHAR = "&";
	public final static String EQUAL_CHAR = "=";
	public final static String HTTP_COOKIE_NAME = "__exth";

	Map<String, String> kvs = new HashMap<String, String>();

	private ExtHeader(byte[] data, int offset, int len) {
		appendFrom(data, offset, len);
		genBytes();
	}

	public ExtHeader() {
	}

	public void appendFrom(byte[] data) {
		appendFrom(data, 0, data.length);
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
						kvs.put(kv[0], kv[1]);
					} else if (kv.length == 1) {// 仅仅就是设置
						kvs.put(kv[0], "1");
					} else {
						log.trace("Unknow ext header:size=" + kv.length + ",str=" + strkv + ",data=" + data);
					}
				}
			}
		} catch (UnsupportedEncodingException e) {
			log.warn("UnsupportedEncodingException：" + data, e);
		}
	}

	public String append(String key, String value) {
		return kvs.put(key, value);
	}

	public void reset() {
		this.data = null;
	}

	public byte[] genBytes() {
		if (kvs.size() == 0)
			return PackHeader.EMPTY_BYTES;
		if (data != null) {
			return data;
		}
		StringBuffer sb = new StringBuffer();
		for (Entry<String, String> pair : kvs.entrySet()) {
			sb.append(pair.getKey()).append(EQUAL_CHAR).append(pair.getValue()).append(SPLIT_CHAR);
		}
		try {
			data = sb.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.warn("UnsupportedEncodingException：" + data, e);
		}
		return data;
	}

	public boolean isExist(String key) {
		return kvs.containsKey(key);
	}

	public String get(String key) {
		return kvs.get(key);
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
			if(!kv.getKey().equals(PackHeader.HTTP_PARAM_FIX_HEAD)&&!kv.getKey().equals(PackHeader.HTTP_PARAM_BODY_DATA))
			{
				ret.append(kv.getKey(), kv.getValue()[0]);
			}
		}
		if (req.getCookies() != null) {
			for (Cookie ck : req.getCookies()) {
				if (HTTP_COOKIE_NAME.equals(ck.getName())) {
					BASE64Decoder decoder = new BASE64Decoder();
					try {
						ret.appendFrom(decoder.decodeBuffer(ck.getValue()));
					} catch (IOException e) {
					}
				}
			}
		}
		ret.append(PackHeader.PEER_IP, HttpHelper.getIpAddr(req));
		return ret;
	}

}
