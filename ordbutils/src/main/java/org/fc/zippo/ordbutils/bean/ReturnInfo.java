package org.fc.zippo.ordbutils.bean;

import lombok.Data;
import onight.tfw.outils.serialize.JsonSerializer;

@Data
public class ReturnInfo {
	private String description;
	private int retcode;
	private Object retObj;
	private boolean success;
	public final static ReturnInfo Success = new ReturnInfo("success", 0, null,true);
	public final static ReturnInfo Faild = new ReturnInfo("failed", 0, null,false);
	public final static String Success_JSON = JsonSerializer.formatToString(Success);
	public final static String Failed_JSON = JsonSerializer.formatToString(Faild);


	public ReturnInfo(String description, int retcode, Object retObj,boolean success) {
		super();
		this.description = description;
		this.retcode = retcode;
		this.retObj = retObj;
		this.success = success;
	}
	
	public ReturnInfo(String description,boolean success) {
		super();
		this.description = description;
		this.retcode = 0;
		this.retObj = null;
		this.success = success;
	}
	
	public ReturnInfo(boolean success) {
		super();
		this.description = "";
		this.retcode = 0;
		this.retObj = null;
		this.success = success;
	}

	public ReturnInfo() {
		super();
	}

}
