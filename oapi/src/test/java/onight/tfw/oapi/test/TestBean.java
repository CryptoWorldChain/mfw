package onight.tfw.oapi.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import onight.tfw.outils.serialize.TransBeanSerializer;

public class TestBean {

	public static void main(String[] args) {
		try {
//			BeanInfo info = Introspector.getBeanInfo(Bean1.class,Introspector.USE_ALL_BEANINFO);
//			PropertyDescriptor[] pds = info.getPropertyDescriptors();
//			for(PropertyDescriptor pd:pds)
//			{
//				System.out.println(pd.getName()+",dis="+pd.getDisplayName()+","+pd.getPropertyType());
//				
//			}
//			
//			System.out.println("==================================");
//			for(BeanProp entry:TransBeanSerializer.extractMethods(Bean1.class)){
//				System.out.println(entry.getFieldName()+",get="+entry.getGetM()+",set="+entry.getSetM());
//			}
//			System.out.println("==================================");

			
			Bean1 bb=new Bean1();
			HashMap<String,ListBean1> map=new HashMap<String, ListBean1>();
			map.put("m1", new ListBean1("hashmap","hashmap",null,null));
			map.put("m2", new ListBean1("hashmap2","hashmap2",null,null));
			List<ListBean1> list=Arrays.asList(new ListBean1("1","1",null,null),new ListBean1("2","2",null,null));
			bb.setBol(true);
			bb.setIn1(10);
			bb.setBb1(new ListBean1("bb1",null,new ListBean1("bb2","bb2",null,null),list));
			bb.setStr1("ok");
			bb.setMap1(map);
			bb.setList1(list);
			Object oo=TransBeanSerializer.getInstance().serialize(bb);
			System.out.println("oo=="+oo);
			Bean2 b2=TransBeanSerializer.getInstance().deserialize(oo, Bean2.class);
			System.out.println("bb="+bb);
			System.out.println("b2="+b2);

			System.out.println("equal="+bb.equals(b2));

//			System.out.println("bean="+bb.getBb1().equals(b2.getBb1()));
//			System.out.println("listbeanequal="+bb.getList1().equals(b2.getList1()));

			
			System.out.println("==================================");
			
			LoopBean1 lp=new LoopBean1();
			lp.setBol(true);
			LoopDepBean1 ldp=new LoopDepBean1();
			ldp.setIn1(1);
			ldp.setLoopbean(lp);
			lp.setDepbean(ldp);
			System.out.println(lp);
			Object oolp=TransBeanSerializer.getInstance().serialize(lp);
//			System.out.println(oolp);
			Object oolp2=TransBeanSerializer.getInstance().serialize(oolp);

			Object ooldp=TransBeanSerializer.getInstance().serialize(ldp);
//			System.out.println(ooldp);

			LoopBean2 lp2=TransBeanSerializer.getInstance().deserialize(oolp,LoopBean2.class);
			System.out.println(lp2);

			LoopDepBean2 ldp2=TransBeanSerializer.getInstance().deserialize(ooldp,LoopDepBean2.class);
			System.out.println(ldp2);

			LoopBean2 lp3=TransBeanSerializer.getInstance().deserialize(oolp2,LoopBean2.class);
			System.out.println(lp3);

			System.out.println("==================================");

			LoopBean1 lpn=new LoopBean1();

			LoopBean1 lpn1=new LoopBean1();

			System.out.println(System.identityHashCode("ab"));
			System.out.println(System.identityHashCode("ab"));
			
			System.out.println(System.identityHashCode(lpn));

			System.out.println(System.identityHashCode(lpn1));

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
}
