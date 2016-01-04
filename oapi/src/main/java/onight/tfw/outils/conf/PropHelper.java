package onight.tfw.outils.conf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;

/**
 * 配置信息帮助类
 * 
 * @author brew
 * 
 */
@Slf4j
public class PropHelper {

	private File configFile;

	BundleContext context;

	public PropHelper(BundleContext context, String propfile) {

		if (context == null) {
			context = BundleReference.class.cast(PropHelper.class.getClassLoader()).getBundle().getBundleContext();
		}
		this.context = context;
		String config = context.getProperty(propfile);
		if (config == null) {
			config = "./conf/ofw.properties";
			if (!new File(config).exists()) {
				config = "../conf/ofw.properties";
			}
		}
		configFile = new File(config);
		log.debug("configFile::" + config + "::FF==" + configFile.exists());
		init();
	}

	public PropHelper(BundleContext context) {
		this(context, "ofwConf");
	}

	Properties local_props = new Properties();

	public void init() {
		if (configFile.exists()) {
			Properties propsConf = new Properties();
			try {
				InputStream in = new BufferedInputStream(new FileInputStream(configFile));
				propsConf.load(in);
				in.close();
			} catch (Exception e) {

			}
			local_props.putAll(propsConf);

			{// try ext
				Properties extpropsConf = new Properties();
				String extconfig = "./conf/ofwext.properties";
				if (!new File(extconfig).exists()) {
					extconfig = "../conf/ofwext.properties";
				}

				if (!new File(extconfig).exists()) {
					extconfig = configFile.getParent() + File.separator + "ofwext.properties";
				}
				File extconf = new File(extconfig);
				if (extconf.exists()) {
					try {
						InputStream in = new BufferedInputStream(new FileInputStream(extconf));
						extpropsConf.load(in);
						in.close();
					} catch (Exception e) {

					}
					local_props.putAll(extpropsConf);
				}

			}
		}
	}

	public String get(String key, String defaultv) {
		Object v = local_props.get(key);
		if (v == null) {
			return defaultv;
		}
		return v.toString();
	}

	public static interface IFinder {
		public void onMatch(String key, String v);
	}

	public void findMatch(String kv, IFinder match) {
		for (Entry<Object, Object> entry : local_props.entrySet()) {
			String key = (String) entry.getKey();
			String v = (String) entry.getValue();
			if (key.matches(kv)) {
				match.onMatch(key, v);
			}
		}
	}

	public int get(String key, int defaultv) {
		Object v = local_props.get(key);
		if (v == null) {
			return defaultv;
		}
		try {
			return Integer.parseInt(v.toString());
		} catch (NumberFormatException e) {
		}
		return defaultv;
	}

}
