package onight.tfw.otransio.api.beans;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Hex;
import org.apache.felix.ipojo.util.Log;
import org.codehaus.jackson.annotate.JsonIgnore;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.otransio.api.LengthUtils;
import onight.tfw.otransio.api.MessageException;
import onight.tfw.otransio.api.PackHeader;

/**
 * 固定头长度
 * 
 * @author brew
 *
 */
@Data
@Slf4j
public class FixHeader {
	//
	// -- 1字节版本号，3字节cmd，3字节moduel模块id;
	// -- 1字节：flag 用后4位，sync(前2位)|bits进制数（后2位），
	// -- sync=0表示同步，=1为异步
	// -- bits
	// 0表示16进制（扩展头部最大255,body最大4k)，1表示78进制（大小写字母+数字，扩展头部最大6k,body最大474k),2表示采用字节方式,
	// 3表示第一个字节是
	// --
	// 例如0表示同步16进制，1表示同步78进制，0100=4表示异步16进制，0101=5表示异步78进制，0010=2表示同步字节，0110=6表示异步字节
	// -- 一共有2*4=8种情况

	// 2字节扩展头部，==>9个字节
	// 3字节body（最大4k）
	// -- 1字节：enc编码，
	// -- 1字节：优先级
	// -- 1字节：预留,0-请求包,1相应包
	// -- 一共16个字节
	byte[] data;

	@JsonIgnore
	public final static int LENGTH = 16;

	// 版本号:1字节
	@JsonIgnore
	char ver = 'v';// 字母+数字，区分大小写，62个版本，够用了吧。如果是‘b’表示发送的是BC的包

	// 命令码:3字节
	String cmd;
	// 模块名:3字节
	String module;
	// 标志位：1字节：用后4位，sync(前2位)|bits进制数（后2位），
	int flag;
	boolean isSync = true;
	boolean isResp = false;
	// kv扩展头部的大小: 2字节
	int extsize;
	// body的大小: 3字节
	int bodysize;
	// body编码类型:1字节:/J==JSON,P==Protobuf,M==MapBean
	char enctype = 'T';
	// 优先级：1字节
	byte prio;
	// 预留：1字节
	byte reserved;

	// byte buff[] = new byte[16];

	public FixHeader(byte[] data) {
		this.data = data;
	}

	public boolean isResp() {
		return isResp;
	}

	public FixHeader() {
		this.data = new byte[16];
		reset();
	}

	public static FixHeader parseFrom(byte[] data) {
		return new FixHeader(data).parse();
	}

	public void reset() {
		this.data[0] = -1;
		dataAlreadyGen = false;
	}

	public byte[] genBytes() {
		return toBytes(isSync());
	}

	public boolean isSync() {
		return isSync;// ((data[7] & 4) == 4);
	}

	public void setSync(boolean sync) {
		isSync = sync;
		// if (sync) {
		// data[7] |= 4;
		// } else {
		// data[7] &= 0xFB;// 0000,0100==>1111,1011==>
		// }
	}

	public void setResp(boolean resp) {
		isResp = true;
	}

	boolean dataAlreadyGen = false;

	public byte[] toBytes(boolean sync) {
		// if (dataAlreadyGen) {
		// return data;
		// }
		if (ver == 'B' || ver == 'b') {// 表示BC的包
			data[0] = (byte) (ver);//
			// 0-->表示类型，接着6位表示模块和：共1字节
			// 1-6 --> 命令+模块，共6字节
			// 7-9 --> 扩展信息长度，3个字节
			// 10->13->body长度，4个字节//
			// 14,->优先级
			// 15-->保留字段
			{
				// System.arraycopy(cmd.getBytes(), 0, data, 1, 3);
				byte bb[] = cmd.getBytes();
				for (int i = 0; i < 3; i++) {
					if (i < bb.length) {
						data[i + 1] = bb[i];
					} else {
						data[i + 1] = '*';
						log.error("cmd length error:cmd=" + cmd + ",module=" + module);
					}
				}
			}
			{
				// System.arraycopy(module.getBytes(), 0, data, 4, 3);
				byte bb[] = module.getBytes();
				for (int i = 0; i < 3; i++) {
					if (i < bb.length) {
						data[i + 4] = bb[i];
					} else {
						data[i + 4] = '*';
						try {
							throw new MessageException("module length error:cmd=" + cmd + ",module=" + module);
						} catch (Exception e) {
							e.printStackTrace();
							log.error("error:Stack", e);
							throw new MessageException(e);
						}
						
//						log.error("module length error:cmd=" + cmd + ",module=" + module);
					}
				}
			}

			LengthUtils.int2Byte3(extsize, data, 7);
			LengthUtils.int2Byte4(bodysize, data, 10);
			data[14] = (byte) prio;
			if (isSync) {
				if (isResp) {
					data[15] = '3';
				} else {
					data[15] = '2';
				}
			} else {
				if (isResp) {
					data[15] = '1';
				} else {
					data[15] = '0';
				}
			}
			dataAlreadyGen = true;
		} else {
			data[0] = (byte) (ver);
			System.arraycopy(cmd.getBytes(), 0, data, 1, 3);
			System.arraycopy(module.getBytes(), 0, data, 4, 3);
			data[7] = 0;
			if (sync) {
				data[7] |= 4;
			}
			if (extsize < 16 * 16 && bodysize < 16 * 16 * 16) {// 采用16进制
				LengthUtils.format16V2(extsize, 16, data, 8);
				LengthUtils.format16V3(bodysize, 16, data, 10);
			} else if (extsize < 78 * 78 || bodysize < 78 * 78 * 78) {
				data[7] |= 1;// 78进制编码
				LengthUtils.format78V2(extsize, 78, data, 8);
				LengthUtils.format78V3(bodysize, 78, data, 10);
			} else {
				data[7] |= 2;// 字节编码
				LengthUtils.int2Byte2(extsize, data, 8);
				LengthUtils.int2Byte3(bodysize, data, 10);
			}
			if (data[7] < 10) {
				data[7] = (byte) (data[7] + '0');
			} else {
				data[7] = (byte) (data[7] + 'A' - '0');
			}
			data[13] = (byte) enctype;
			data[14] = (byte) (prio + '0');
			data[15] = reserved;
			dataAlreadyGen = true;
		}
		return data;
	}

	private FixHeader parse() {
		ver = (char) data[0];
		if (ver == 'B' || ver == 'b') {// 2进制版本
			cmd = new String(data, 1, 3).trim();
			module = new String(data, 4, 3).trim();
			flag = 2;
			extsize = LengthUtils.byte2Int(data[7], data[8], data[9]);
			bodysize = LengthUtils.byte2Int(data[10], data[11], data[12], data[13]);
			enctype = 'P';// protobuf
			prio = data[14];
			reserved = (byte) (data[15]);
			if (reserved == '0') {
				isSync = false;
				isResp = false;
			} else if (reserved == '1') {
				isResp = true;
				isSync = false;
			} else if (reserved == '2') {
				isResp = false;
				isSync = true;
			} else if (reserved == '3') {
				isResp = true;
				isSync = true;
			}

		} else if (ver == 'v' || ver == 'V') {// 2进制版本{//
			cmd = new String(data, 1, 3).trim();
			module = new String(data, 4, 3).trim();
			if (data[7] <= '9') {
				flag = (byte) (data[7] - '0');
			} else {
				flag = (byte) (data[7] - 'A' + 10);
			}
			if ((flag & 0x3) == 2) {// 去字节方式
				extsize = LengthUtils.byte2Int(data[8], data[9]);
				bodysize = LengthUtils.byte2Int(data[10], data[11], data[12]);
			} else {
				int radix = 16;
				if ((flag & 0x3) == 1) {// 取后2位
					radix = 78;
					extsize = LengthUtils.parseInt(new String(data, 8, 2), radix);
					bodysize = LengthUtils.parseInt(new String(data, 10, 3), radix);
				} else {
					extsize = Integer.parseInt(new String(data, 8, 2), radix);
					bodysize = Integer.parseInt(new String(data, 10, 3), radix);
				}
			}
			enctype = (char) data[13];
			prio = (byte) (data[14] - '0');
			reserved = (byte) (data[15]);
		} else {
			log.error("unknow package:" + Hex.encodeHexString(data));
			throw new MessageException("unknow package:Type:" + data[0] + ",fh=" + Hex.encodeHexString(data));
		}
		return this;
	}

	public String toString() {
		return "FixHeader[" + new String(genBytes()) + ",fs=" + extsize + ",bs=" + bodysize + "]@" + this.hashCode();
	}

	public String toStrHead() {
		return new String(genBytes());
	}

	public static FixHeader buildFrom(HttpServletRequest req) {
		if (req.getParameter(PackHeader.HTTP_PARAM_FIX_HEAD) == null) {
			FixHeader fh = new FixHeader();
			fh.setSync(true);
			try {
				String paths[] = req.getServletPath().split("/");
				if (paths.length > 2) {
					fh.setCmd(paths[paths.length - 1].substring(2).replaceAll(".do", "").toUpperCase());
					fh.setModule(paths[paths.length - 2].toUpperCase());
					fh.setEnctype('J');
				}
			} catch (Throwable t) {
			}
			return fh;
		}
		return parseFrom(req.getParameter(PackHeader.HTTP_PARAM_FIX_HEAD).getBytes());
	}

	public int getTotalSize() {
		return this.bodysize + this.extsize;
	}
}
