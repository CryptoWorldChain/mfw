package org.fc.zippo.ordbutils.rest;

import org.apache.commons.lang3.StringUtils;

public class StringHelper {

	public static boolean toBool(String v) {
		return StringUtils.isNotBlank(v) && (//
		StringUtils.equalsIgnoreCase("1", v.trim()) //
				|| StringUtils.equalsIgnoreCase("true", v.trim()) //
				|| StringUtils.equalsIgnoreCase("on", v.trim())//
		);
	}

	public static Integer toInteger(String v) {
		if (StringUtils.isNotBlank(v)) {
			try {
				return Integer.parseInt(v.trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	public static Long toLong(String v) {
		if (StringUtils.isNotBlank(v)) {
			try {
				return Long.parseLong(v.trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}
}
