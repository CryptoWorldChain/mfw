package org.fc.zippo.ordbutils.bean;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ListInfo<T> {

	int total_rows;
	List<T> rows;
	int totalPages;
	int skip;
	int limit;
//	String identifier;
//	String secondid;
//	String label;

	public ListInfo(int total_rows, List<T> rows, PageInfo para) {
		super();
//		identifier = "";
		init(total_rows, rows, para.getSkip(), para.getLimit());
	}
	
	public ListInfo(int total_rows, List<T> rows, int skip, int limit) {
		super();
//		identifier = "";
		init(total_rows, rows, skip, limit);
	}

	public ListInfo(String identifier, int total_rows, List<T> rows, int skip,
			int limit) {
		super();
//		this.identifier = identifier;
//		this.identifier = "";
		init(total_rows, rows, skip, limit);
	}

	public void init(int total_rows, List<T> rows, int skip, int limit) {
		this.total_rows = total_rows;
		this.rows = rows;
		if (rows!=null&&rows.size() > 0) {
			this.totalPages = Math.abs(total_rows / limit
					+ (total_rows % limit == 0 ? 0 : 1));
		} else {
			this.totalPages = 0;
		}
		this.skip = skip;
		this.limit = limit;
	}

	public ListInfo() {
		super();
		rows=new ArrayList<T>();
	}
	
	public T getItem(int index)
	{
		if(index>=0&&index<rows.size())
		{
			return rows.get(index);
		}
		else
		{
			return null;
		}
	}
	
	public T firstItem()
	{
		if(rows.size()>0)
		{
			return rows.get(0);
		}
		return null;
	}
	
}
