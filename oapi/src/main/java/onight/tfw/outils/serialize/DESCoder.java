package onight.tfw.outils.serialize;

import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import org.apache.commons.codec.binary.Base64;

public class DESCoder {

	private final static String CHARSET = "utf-8";

	/**
	 * 对cipherText进行DES解密
	 * 
	 * @param cipherText
	 * @return
	 * @throws DesException
	 */
	public static String desDecrypt(String cipherText, String desKey)
			throws DesException {
		String decryptStr = null;
		try {
			// 解密
			Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Pading");

			byte[] input = Base64.decodeBase64(cipherText.trim().getBytes(
					CHARSET));

			cipher.init(Cipher.DECRYPT_MODE, genSecretKey(desKey));
			byte[] output = cipher.doFinal(input);
			decryptStr = new String(output, CHARSET);

		} catch (Exception e) {
			throw new DesException("DES解密发生异常!", e);
		}

		return decryptStr;
	}

	/**
	 * 对message进行DES加密
	 * 
	 * @param message
	 * @return
	 * @throws DesException
	 */
	public static String desEncrypt(String message, String desKey)
			throws DesException {
		String encryptStr = null;
		byte encryptByte[];

		try {
			// 加密
			Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Pading");
			cipher.init(Cipher.ENCRYPT_MODE, genSecretKey(desKey));
			byte[] cipherText = cipher
					.doFinal(message.trim().getBytes(CHARSET));
			return Base64.encodeBase64URLSafeString(cipherText);
//			encryptStr = new String(encryptByte, CHARSET);
//			encryptStr = encryptStr.replaceAll("\r\n", "").replaceAll("\n", "");

		} catch (Exception e) {
			throw new DesException("des加密发生异常!", e);
		}
	}
	/**
	 * 对message进行DES加密
	 * 
	 * @param message
	 * @return
	 * @throws DesException
	 */
	public static byte[] encEncryptToBytes(String message, String desKey)
			throws DesException {
		try {
			// 加密
			Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, genSecretKey(desKey));
			byte[] cipherText = cipher
					.doFinal(message.trim().getBytes(CHARSET));
			return cipherText;
		} catch (Exception e) {
			throw new DesException("des加密发生异常!", e);
		}
	}

	/**
	 * 对byte进行DES解密
	 * 
	 * @param message
	 * @return
	 * @throws DesException
	 */
	public static String desFromBytes(byte []bytes, String desKey)
			throws DesException {
		String decryptStr = null;
		try {
			// 解密
			Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, genSecretKey(desKey));
			byte[] output = cipher.doFinal(bytes);
			decryptStr = new String(output, CHARSET);
			return decryptStr;
		} catch (Exception e) {
			throw new DesException("DES解密发生异常!", e);
		}

	}
	
	/**
	 * 对流进行3DES加密
	 * 
	 * @param message
	 * @return
	 * @throws DesException
	 */
	public static InputStream enFromInStream(InputStream is, String desKey)
			throws DesException {
		String decryptStr = null;
		try {
			// 解密
			Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, genSecretKey(desKey));
			CipherInputStream cis = new CipherInputStream(is, cipher);
			return cis;
		} catch (Exception e) {
			throw new DesException("DES解密发生异常!", e);
		}
	}
	/**
	 * 对流进行3DES解密
	 * 
	 * @param is
	 * @return
	 * @throws DesException
	 */
	public static InputStream desFromInStream(InputStream is, String desKey)
			throws DesException {
		String decryptStr = null;
		try {
			// 解密
			Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, genSecretKey(desKey));
			CipherInputStream cis = new CipherInputStream(is, cipher);
			return cis;
		} catch (Exception e) {
			throw new DesException("DES解密发生异常!", e);
		}
	}
	
	private static SecretKey genSecretKey(String key)
			throws InvalidKeyException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		DESKeySpec desKeySpec = new DESKeySpec(hexStringToByte(key));
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

		return secretKey;
	}

	private static byte[] hexStringToByte(String hex) {
		int len = (hex.length() / 2);
		byte[] result = new byte[len];
		char[] achar = hex.toCharArray();
		for (int i = 0; i < len; i++) {
			int pos = i * 2;
			result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
		}
		return result;
	}

	private static byte toByte(char c) {
		byte b = (byte) "0123456789ABCDEF".indexOf(c);
		return b;
	}
}
