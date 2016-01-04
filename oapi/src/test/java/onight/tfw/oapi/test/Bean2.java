package onight.tfw.oapi.test;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class Bean2 {

	public final static String fst1="abc";
	
	@Getter @Setter
	public  static String st1="abc";
	
	
	boolean bol;
	int in1;
	String str1;
	
	ListBean2 bb1;

	List<ListBean2> list1;
//	
	Map<String,ListBean2> map1;

	
}
