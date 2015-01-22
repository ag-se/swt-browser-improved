package de.fu_berlin.inf.ag_se.widgets.browser.extended.extensions.jquery;

import java.util.Arrays;

import de.fu_berlin.inf.ag_se.utils.ClasspathFileUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.extensions.BrowserExtension;

public class JQueryBrowserExtension extends BrowserExtension {

	public JQueryBrowserExtension() {
		super("jQuery 1.9.0", "return typeof jQuery !== 'undefined';", Arrays
				.asList(ClasspathFileUtils.getFile("/jquery/jquery-1.9.0.js")), null, null);
	}

}
