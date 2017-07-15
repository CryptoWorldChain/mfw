package onight.tfw.outils.serialize;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

public class SessionIDGenerator {

	public static char[] StrMapping = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM0123456789".toCharArray();
	static int radix = StrMapping.length;
	String prefix;
	static char SEP_CH = ':';

	public SessionIDGenerator(String nodeStr) {
		if (nodeStr == null) {
			int ipadd;
			try {
				ipadd = toInt(InetAddress.getLocalHost().getAddress());
			} catch (Exception e) {
				ipadd = 0;
			}
			nodeStr = int2Str(ipadd);
		}
		this.prefix = nodeStr + JVMStr;
	}

	public static String int2Str(int v, int minv) {
		StringBuffer sb = new StringBuffer();
		int radix = StrMapping.length;
		if (v < 0) {
			v = v * -1;
		}
		while (v > minv) {
			sb.append(StrMapping[v % radix]);
			v /= radix;
		}
		return sb.toString();
	}

	public static String int2Str(int v) {
		return int2Str(v, 0);
	}

	public static String randStr() {
		return int2Str((int) (Math.random() * 100000) % 100000);
	}

	public String generate(String userid) {
		StringBuilder sb = new StringBuilder(32).append(genSum(userid)).append(randStr()).append(prefix).append(getCount()).append(SEP_CH).append(userid);
		String v = sb.toString();
		v = Base64.encodeBase64URLSafeString(v.getBytes());
		char cs = genSum(v);
		return v + cs;
	}

	public static  String fetchid(String token) {
		if (!checkSum(token))
			return null;
		String decode = new String(Base64.decodeBase64(token.substring(0,token.length()-1)));
		int idx = decode.indexOf(':');
		if (idx <= 0||decode.length()<8)
			return null;
		return decode.substring(idx+1).trim();
	}
	

	public String genToken(String userid, String desKey, String keyIdx) {
		// StringBuilder sb = new StringBuilder(generate(userid));
		StringBuilder sb = new StringBuilder(32).append(randStr()).append(prefix).append(getCount()).append(SEP_CH).append(userid);
		String v = sb.toString();
		char cs = genSum(v);
		v += cs;
		try {
			if (desKey.length() < 16)
				desKey = StringUtils.rightPad(desKey, 16, SEP_CH);
			v = DESCoder.desEncrypt(v, desKey);
		} catch (DesException e) {
			e.printStackTrace();
		}
		v = genSum(userid) + v + keyIdx;
		cs = genSum(v);
		return v + cs;
	}

	public String checkToken(String vstring, String desKey) {
		if (!checkSum(vstring)) {
			return null;
		}
		try {
			if (desKey.length() < 16)
				desKey = StringUtils.rightPad(desKey, 16, SEP_CH);
			String v = DESCoder.desDecrypt(vstring.substring(1, vstring.length() - 3), desKey);
			if (!checkSum(v)) {
				return null;
			}
			int idx = v.indexOf(SEP_CH);
			if (idx < 0 || idx >= v.length() - 3) {
				return null;
			}
			return v.substring(idx + 1, v.length() - 1);
		} catch (DesException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static AtomicInteger counter = new AtomicInteger(8888);

	private static final String JVMStr = int2Str(UUIDGenerator.JVM);

	public static String timeTick = int2Str((int) (System.currentTimeMillis()), 100);
	
	public static String CounterLock="CounterLock";
	
	private final static String getCount() {
		int curr = counter.get();
		if (curr > 10000) {
			synchronized (CounterLock) {
				if (curr > 10000) {
					counter.set(0);
					timeTick = int2Str((int) (System.currentTimeMillis()), 100);
				}
			}
		} else if (curr % 100 == 0) {
			timeTick = int2Str((int) (System.currentTimeMillis()), 100);
			// System.out.println("tick:" + timeTick);
		}
		int next = counter.incrementAndGet();

		return timeTick + int2Str(next);
	}

	private final static int toInt(byte[] bytes) {
		int result = 0;
		for (int i = 0; i < 4; i++) {
			result = (result << 8) - Byte.MIN_VALUE + (int) bytes[i];
		}
		return result;
	}

	public static char genSum(String vstring) {
		int sum = 0, checkDigit = 0;

		boolean isDouble = true;
		for (int i = vstring.length() - 1; i >= 0; i--) {
			int k = (int) vstring.charAt(i);
			sum += sumToSingleDigit((k * (isDouble ? 2 : 1)));
			isDouble = !isDouble;
		}

		int radix = StrMapping.length;
		if ((sum % radix) > 0)
			checkDigit = (radix - (sum % radix));
		return StrMapping[checkDigit];
	}

	public static boolean checkSum(String vstring) {
		int sum = 0, checkDigit = 0;

		boolean isDouble = true;
		for (int i = vstring.length() - 2; i >= 0; i--) {
			int k = (int) vstring.charAt(i);
			sum += sumToSingleDigit((k * (isDouble ? 2 : 1)));
			isDouble = !isDouble;
		}
		int radix = StrMapping.length;
		if ((sum % radix) > 0)
			checkDigit = (radix - (sum % radix));

		return vstring.charAt(vstring.length() - 1) == (StrMapping[checkDigit]);
	}

	private static int sumToSingleDigit(int k) {
		if (k < radix)
			return k;
		return sumToSingleDigit(k / radix) + (k % radix);
	}

	public static void main(String[] args) {
		System.out.println("PID:" + JVMStr);
		SessionIDGenerator sid = new SessionIDGenerator("abc");
		System.out.println("IP:" + sid.prefix);
		System.out.println("randStr:" + randStr());
		System.out.println((System.currentTimeMillis()) + "::" + System.currentTimeMillis());
		System.out.println(int2Str((int) (System.currentTimeMillis() >> 8)));
		System.out.println(int2Str((int) (System.currentTimeMillis() >> 8)));
		for (int i = 0; i < 10; i++) {
			String smid = sid.generate("hello");
			System.out.println(i + ":" + smid + ":check==" + checkSum(smid)+",userid="+fetchid(smid).equals("hello"));
		}
		System.out.println(checkSum("abccIelpyjSxe3"));
		for (int i = 0; i < 10; i++) {
			String smid = sid.genToken("a-"+i, "aabbcc", "AF");
			System.out.println("a-" + i + ":token:" + smid + ":check==" + sid.checkToken(smid, "aabbcc") + ":equal="
					+ StringUtils.equals("a-" + i, sid.checkToken(smid, "aabbcc")));
		}

	}
}
