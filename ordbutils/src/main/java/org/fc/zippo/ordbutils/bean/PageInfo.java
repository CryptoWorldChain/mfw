package org.fc.zippo.ordbutils.bean;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.fc.zippo.ordbutils.rest.StringHelper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.NonFinal;

@AllArgsConstructor
@NoArgsConstructor
public class PageInfo {

	private @Setter boolean page;
	private @Setter Integer skip;
	private @Setter Integer limit;
	private @Setter @Getter boolean sortModed;
	private @Setter String sort;

	public static PageInfo fromReq(HttpServletRequest req) {
		return new PageInfo(StringHelper.toBool(req.getParameter("page")),
				StringHelper.toInteger(req.getParameter("skip")), StringHelper.toInteger(req.getParameter("limit")),
				StringHelper.toBool(req.getParameter("sortModed")), req.getParameter("sort"));
	}

	public Integer getSkip() {
		return skip == null ? 0 : skip;
	}

	public Integer getLimit() {
		return limit == null ? Integer.MAX_VALUE : limit;
	}

	public String getSort() {
		return sort;
	}

	public boolean isPage() {
		return page;
	}

}
