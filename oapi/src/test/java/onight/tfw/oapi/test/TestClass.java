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
		
	}
}
