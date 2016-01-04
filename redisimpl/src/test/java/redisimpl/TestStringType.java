package redisimpl;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;
import onight.ojpa.redis.loader.StringType;
import onight.tfw.outils.serialize.TransBeanSerializer;
import onight.tfw.outils.serialize.TransBeanSerializer.BeanMap;

public class TestStringType {

	@Data
	@NoArgsConstructor
	public static class M1 {
		String name;
		HashMap<String, M2> mmap;
	}
	@Data
	@NoArgsConstructor
	public static class M2 {
		String n2ame;
		HashMap<String, String> m2map;
	}

	public static void main(String[] args) {
		M2 m2 = new M2();
		m2.n2ame = "abc";
		m2.m2map = new HashMap<String, String>();
		m2.m2map.put("m2name1", "abcdef");
		m2.m2map.put("m2name2", "abcde3");

		M1 m1 = new M1();
		m1.name = "mmm1";
		m1.mmap = new HashMap<String, TestStringType.M2>();
		m1.mmap.put("abc", m2);
		m1.mmap.put("def", m2);
		
		HashMap<byte[], byte[]> dist = new HashMap<byte[], byte[]>();
		System.out.println("mm:"+TransBeanSerializer.getInstance().serialize(m1));
		StringType.flatMap((Map<String, Object>)TransBeanSerializer.getInstance().serialize(m1), dist, "");
		for(byte[] key:dist.keySet())
		{
			System.out.println("dist::key=="+new String(key));
		}
		System.out.println("dist=="+dist);
		BeanMap<String, Object> bmap = new BeanMap<String, Object>();
		StringType.foldMap(bmap, dist);
		System.out.println("bmap="+bmap);
		M1 mm1=TransBeanSerializer.getInstance().deserialize(bmap, M1.class);
		System.out.println(m1);
		System.out.println(mm1);
//		System.out.println(mm1.getMmap().get("abc").getN2ame());
		
	}
}
