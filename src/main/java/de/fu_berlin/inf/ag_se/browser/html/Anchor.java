package de.fu_berlin.inf.ag_se.browser.html;

import java.util.HashMap;
import java.util.Map;

public class Anchor extends Element implements IAnchor {

	public Anchor(Map<String, String> attributes, String content) {
		super("a", attributes, content);
	}

	public Anchor(org.jsoup.nodes.Element anchor) {
		super(anchor);
		if (!anchor.tagName().equals("a")) {
			throw new IllegalArgumentException(
					"The given element is no anchor tag");
		}
	}

	@SuppressWarnings("serial")
	public Anchor(final String href, final String[] classes, String content) {
		super("a", new HashMap<String, String>() {
			{
				if (href != null) {
					this.put("href", href);
				}
				if (classes != null) {
					this.put("class", org.apache.commons.lang.StringUtils.join(
							classes, " "));
				}
			}
		}, content);
	}

	@Override
	public String getHref() {
		return this.getAttribute("href");
	}

}
