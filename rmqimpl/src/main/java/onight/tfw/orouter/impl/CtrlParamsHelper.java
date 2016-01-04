package onight.tfw.orouter.impl;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

import org.apache.felix.framework.BundleWiringImpl.BundleClassLoader;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;

/**
 * 参数表帮助类
 * 
 * @author brew
 * 
 */
@Deprecated
@Slf4j
public class CtrlParamsHelper {
 
	private File configFile;

	BundleContext context;

	public CtrlParamsHelper(BundleContext context) {
		this.context = context;
		String config = context.getProperty("mqconf");
		if (config == null) {
			config = "conf/rabbitmq.props";
		}
		configFile = new File(config);
		log.debug("configFile::" + config + "::FF==" + configFile.exists());
	}

	Properties default_props = new Properties();
	Properties user_props = new Properties();

	Properties all_props = new Properties();

	@Validate
	public void init() {
		try {

			InputStream in = BundleClassLoader.getSystemResourceAsStream("/rabbitmq_default.props");
			log.debug("inProp:"+in);
			default_props.load(in);
			in.close();
		} catch (Exception e1) {
		}

		if (configFile.exists()) {
			Properties propsConf = new Properties();
			try {
				InputStream in = new BufferedInputStream(new FileInputStream(configFile));
				propsConf.load(in);
				in.close();
			} catch (Exception e) {

			}
			user_props.putAll(propsConf);
		}
		all_props.putAll(default_props);
		all_props.putAll(user_props);
	}

	public void insert(String key, String v) {
		insert(key, v, null);
	}

	public void insert(String key, String v, String comments) {
		all_props.setProperty(key, v);
		if (v != null) {
			if (configFile.exists() && (user_props.contains(key) || !default_props.contains(key))) {
				try {
					user_props.setProperty(key, v);
					OutputStream out = new FileOutputStream(configFile);
					user_props.store(out, comments);
					out.close();
				} catch (Exception e) {
				}
			}

		}
	}

	public String get(String key, String defaultv) {
		Object v = all_props.get(key);
		if (v == null) {
			insert(key, defaultv);
			return defaultv;
		}
		return v.toString();
	}

	public int get(String key, int defaultv) {
		Object v = all_props.get(key);
		if (v == null) {
			insert(key, String.valueOf(defaultv));
			return defaultv;
		}
		try {
			return Integer.parseInt(v.toString());
		} catch (NumberFormatException e) {
		}
		return defaultv;
	}

	public static void main(String[] args) {
		CtrlParamsHelper params = new CtrlParamsHelper(null);

		params.init();

		params.insert("kkk555", "2");
		params.insert("kkk5", "3");

		System.out.println(params.all_props);
	}

}
