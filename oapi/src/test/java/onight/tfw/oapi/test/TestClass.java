package onight.tfw.oapi.test;

public class TestClass {

	public static void main(String[] args) {
		
		Bean1OnA b1oa=new Bean1OnA("aa");
		Class clazz=b1oa.getSubType();
		System.out.println(clazz);
		try {
			System.out.println(clazz.newInstance());
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(int i=0;i<100;i++){
			System.out.println("insert into tfg.TFG_LOGIN_USER values('11"+i+"', 'abc"+i+"', 'test"+i+"', 'aab@cc.com', '136772730"+i+"', 'a', 'b', '1', '1', '000000', '000000', NULL, NULL);");
		}
		
	}
}
