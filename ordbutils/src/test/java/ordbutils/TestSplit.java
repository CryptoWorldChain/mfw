package ordbutils;

import org.apache.commons.lang3.StringUtils;

public class TestSplit {
	
	public static void main(String[] args) {
		String str = "123|aab|||";
		String ss[] = str.split("\\|");

		System.out.println(ss.length);
		ss = StringUtils.splitPreserveAllTokens(str, '|');
		for(String s:ss){
			System.out.println(":"+s+":");
		}
		System.out.println(ss.length);
	}
}
