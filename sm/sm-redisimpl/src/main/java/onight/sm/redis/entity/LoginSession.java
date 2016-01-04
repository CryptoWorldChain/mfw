package onight.sm.redis.entity;


public class LoginSession {
	String smid;
	String logid;
	String pwd;
	String ext;

	public LoginSession(String smid, String logid, String pwd, String ext) {
		super();
		this.smid = smid;
		this.logid = logid;
		this.pwd = pwd;
		this.ext = ext;
	}

	public String getSmid() {
		return smid;
	}

	public void setSmid(String smid) {
		this.smid = smid;
	}

	public String getLogid() {
		return logid;
	}

	public void setLogid(String logid) {
		this.logid = logid;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public String getExt() {
		return ext;
	}

	public void setExt(String ext) {
		this.ext = ext;
	}

}
