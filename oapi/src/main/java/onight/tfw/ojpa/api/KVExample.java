package onight.tfw.ojpa.api;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class KVExample {
	protected String orderByClause;

	protected boolean distinct;

	protected List<Object> criterias;

	protected int offset;

	protected int limit;

	protected String sumCol;
	
	protected String selectCol;

	protected String groupSelClause;

	protected String groupByClause;

	public KVExample() {
		criterias = new ArrayList<Object>();
		offset = 0;
		limit = Integer.MAX_VALUE;
	}

}