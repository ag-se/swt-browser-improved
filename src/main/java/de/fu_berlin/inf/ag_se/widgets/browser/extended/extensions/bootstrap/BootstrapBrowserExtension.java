package de.fu_berlin.inf.ag_se.widgets.browser.extended.extensions.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;

import de.fu_berlin.inf.ag_se.widgets.browser.BrowserUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.extensions.BrowserExtension;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.extensions.IBrowserExtension;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.extensions.jquery.JQueryBrowserExtension;

public class BootstrapBrowserExtension extends BrowserExtension {

	public BootstrapBrowserExtension() {
		super(
				"Bootstrap 3.0.0",
				"return (typeof window.jQuery !== 'undefined') && (typeof $().modal == 'function');",
				Arrays.asList(BrowserUtils.getFile(
						BootstrapBrowserExtension.class,
						"bootstrap/js/bootstrap.min.js")), Arrays
						.asList(BrowserUtils.getFileUrl(
								BootstrapBrowserExtension.class,
								"bootstrap/css/bootstrap.min.css")),
				new ArrayList<Class<? extends IBrowserExtension>>(
						Arrays.asList(JQueryBrowserExtension.class)));
	}

}
