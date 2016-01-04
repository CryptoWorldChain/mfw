package onight.tfw.oapi.test;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListBean1 {

	
	public   String lb1="abc";
	public   String lb2="abc";
	
	ListBean1 bbbean1;
	List<ListBean1> bblist1;
	
	
//	List<ListBean1> list1;
//	public boolean equals(ListBean2 other) {
//		if (lb1 == null) {
//			if (other.lb1 != null)
//				return false;
//		} else if (!lb1.equals(other.lb1))
//			return false;
//		if (lb2 == null) {
//			if (other.lb2 != null)
//				return false;
//		} else if (!lb2.equals(other.lb2))
//			return false;
//		return true;
//	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lb1 == null) ? 0 : lb1.hashCode());
		result = prime * result + ((lb2 == null) ? 0 : lb2.hashCode());
		return result;
	}
	
	
	
}
