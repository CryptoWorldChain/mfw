package org.fc.zippo.ordbutils.rest.filter;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Provides;
import org.fc.zippo.filter.FilterConfig;
import org.fc.zippo.filter.exception.FilterException;

import onight.osgi.annotation.iPojoBean;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.ActWrapper;
import onight.tfw.otransio.api.PacketFilter;
import onight.tfw.otransio.api.SimplePacketFilter;
import onight.tfw.otransio.api.beans.FramePacket;

@iPojoBean
@Provides(specifications = { PacketFilter.class }, strategy = "SINGLETON")
public class RequestSizeFilter extends SimplePacketFilter {

	@Override
	public String getSimpleName() {
		return "sizefilter";
	}

	int maxsize = -1;

	@Override
	public void init(FilterConfig filterConfig) throws FilterException {
		maxsize = -1;// 1M
		String strsize = filterConfig.getStrProp("org.zippo.rest.req.maxsize", "1M");
		if (StringUtils.endsWithIgnoreCase(strsize, "M")) {
			maxsize = Integer.parseInt(strsize.trim().substring(0, strsize.length() - 1)) * 1024 * 1024;
		} else if (StringUtils.endsWithIgnoreCase(strsize, "K")) {
			maxsize = Integer.parseInt(strsize.trim().substring(0, strsize.length() - 1)) * 1024;
		} else if (StringUtils.endsWithIgnoreCase(strsize, "G")) {
			maxsize = Integer.parseInt(strsize.trim().substring(0, strsize.length() - 1)) * 1024 * 1024 * 1024;
		} else if (StringUtils.endsWithIgnoreCase(strsize, "B")) {
			maxsize = Integer.parseInt(strsize.trim().substring(0, strsize.length() - 1));
		} else {
			maxsize = Integer.parseInt(strsize.trim());
		}
	}

	@Override
	public boolean preRoute(ActWrapper arg0, FramePacket pack, CompleteHandler arg2) throws FilterException {
		if (pack.getHttpServerletRequest() != null)
			if (maxsize > 0 && pack.getHttpServerletRequest().getContentLength() > maxsize) {
				throw new FilterException("Request Size Limit to :" + maxsize + ", contentLength="
						+ pack.getHttpServerletRequest().getContentLength() + ":");
			}
		return true;
	}

}
