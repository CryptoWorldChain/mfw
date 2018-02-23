package onight.tfw.otransio.api;

/**
 * 最大78个字符的
 * 
 * @author brew
 *
 */
public class LengthUtils {

	public static int parseInt(CharSequence cs, int radix) {
		int v = 0;
		for (int i = 0; i < cs.length(); i++) {
			v = v * radix + (cs.charAt(i) - '0');
		}
		return v;
	}

	public static int byte2Int(byte b1, byte b2, byte b3) {
		int v = (int) (b1 & 0xFF) << 16 | (b2 & 0xFF) << 8 | (b3 & 0xFF);
		return v;
	}

	public static int byte2Int(byte b1, byte b2, byte b3, byte b4) {
		int v = (int) (b1 & 0xFF) << 24 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 8 | (b4 & 0xFF);
		return v;
	}

	public static int int2Byte2(int v, byte[] bb, int offset) {
		bb[offset] = (byte) (v >> 8 & 0xFF);
		bb[offset + 1] = (byte) (v & 0xFF);
		return v;
	}

	public static int int2Byte3(int v, byte[] bb, int offset) {
		bb[offset] = (byte) (v >> 16 & 0xFF);
		bb[offset + 1] = (byte) (v >> 8 & 0xFF);
		bb[offset + 2] = (byte) (v & 0xFF);
		return v;
	}

	public static int int2Byte4(int v, byte[] bb, int offset) {
		bb[offset] = (byte) (v >> 24 & 0xFF);
		bb[offset + 1] = (byte) (v >> 16 & 0xFF);
		bb[offset + 2] = (byte) (v >> 8 & 0xFF);
		bb[offset + 3] = (byte) (v & 0xFF);
		return v;
	}

	public static int byte2Int(byte b1, byte b2) {
		int v = (int) (b1 & 0xFF) << 8 | (b2 & 0xFF);
		return v;
	}

	public static String formatV3(int v, int radix) {
		return "" + (char) (v / radix / radix % radix + '0') + (char) (v / radix % radix + '0')
				+ (char) (v % radix + '0');
	}

	public static String formatV2(int v, int radix) {
		return "" + (char) (v / radix % radix + '0') + (char) (v % radix + '0');
	}

	public static void to16FixCharBytes(byte buff[], int offset, int len) {
		for (int i = 0; i < len; i++) {
			if (buff[i + offset] >= 10) {
				buff[i + offset] += 'A' - 10;
			} else {
				buff[i + offset] += '0';
			}
		}
	}

	public static void to78FixCharBytes(byte buff[], int offset, int len) {
		for (int i = 0; i < len; i++) {
			buff[i + offset] += '0';
		}
	}

	public static void format78V2(int v, int radix, byte buff[], int offset) {
		buff[offset] = (byte) (v / radix % radix);
		buff[offset + 1] = (byte) (v % radix);
		to78FixCharBytes(buff, offset, 2);
	}

	public static void format78V3(int v, int radix, byte buff[], int offset) {
		buff[offset] = (byte) (v / radix / radix % radix);
		buff[offset + 1] = (byte) (v / radix % radix);
		buff[offset + 2] = (byte) (v % radix);
		to78FixCharBytes(buff, offset, 3);
	}

	public static void format16V2(int v, int radix, byte buff[], int offset) {
		buff[offset] = (byte) (v / radix % radix);
		buff[offset + 1] = (byte) (v % radix);
		to16FixCharBytes(buff, offset, 2);
	}

	public static void format16V3(int v, int radix, byte buff[], int offset) {
		buff[offset] = (byte) (v / radix / radix % radix);
		buff[offset + 1] = (byte) (v / radix % radix);
		buff[offset + 2] = (byte) (v % radix);
		to16FixCharBytes(buff, offset, 3);
	}

	public static void main(String[] args) {
		int radix = 78;// 最大78个字符

		for (int i = 0; i <= radix; i++) {
			// System.out.println(i + "," + (char) ('0' + i) + "-->" +
			// parseInt(((char) ('0' + i)) + "", 74));
		}
		// System.out.println(parseInt("z", 74));
		//
		byte bb[] = new byte[10];
		for (int i = 0; i < 16 * 16 * 16; i++) {
			format16V3(i, 16, bb, 0);
			int j = Integer.parseInt(new String(bb, 0, 3), 16);
			if (i != j) {
				System.out.println("error::::i=" + i + ",j=" + j + ",bb=" + new String(bb, 0, 3));
			}
		}
		System.out.println("ookk");
		for (int i = 0; i < 78 * 78; i++) {
			format78V2(i, 78, bb, 0);
			int j = parseInt(new String(bb, 0, 2), 78);
			if (i != j) {
				System.out.println("error::::i=" + i + ",j=" + j + ",bb=" + new String(bb, 0, 2));
			}
		}
		System.out.println("ookk");
		for (int i = 0; i < 78 * 78 * 78; i++) {
			format78V3(i, 78, bb, 0);
			int j = parseInt(new String(bb, 0, 3), 78);
			if (i != j) {
				System.out.println("error::::i=" + i + ",j=" + j + ",bb=" + new String(bb, 0, 3));
			}
		}
		System.out.println("ookk");

		if (true)
			return;
		System.out.println(formatV3(90, 74));
		System.out.println(parseInt("01@", 74));
		for (int i = 0; i < radix * radix * radix; i++) {
			String str = formatV3(i, radix);
			if (i != parseInt(str, radix)) {
				System.out.println("not equal::i=" + i + ",str=" + str);
			}
		}
		bb = new byte[3];
		for (int i = 0; i < 0xffffff; i++) {
			int2Byte3(i, bb, 0);
			int v = byte2Int(bb[0], bb[1], bb[2]);
			if (v != i) {
				System.out.println("not equal::i=" + i + ",str=" + bb[0] + "," + bb[1] + "," + bb[2] + ",");
			}
		}

		System.out.println(parseInt("90", 10));
		System.out.println("oookk");

	}

}
