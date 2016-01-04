package onight.tfw.oapi.test;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListBean2 {

	
	public   String lb1="abc";
	public   String lb2="abc";
	List<ListBean2> list1;
	ListBean2 bbbean1;
//	List<ListBean1> bblist1;

}
