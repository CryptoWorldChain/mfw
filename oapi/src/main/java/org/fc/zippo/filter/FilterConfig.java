package org.fc.zippo.filter;

import org.osgi.framework.BundleContext;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import onight.tfw.outils.conf.PropHelper;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FilterConfig {
	BundleContext ctx;
	PropHelper props;

	public String getStrProp(String key, String defaultv) {
		return props.get(key, defaultv);
	}
	public int getIntProp(String key, int defaultv) {
		return props.get(key, defaultv);
	}
}
