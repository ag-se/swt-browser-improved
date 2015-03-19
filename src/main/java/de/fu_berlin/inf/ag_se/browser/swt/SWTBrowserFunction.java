package de.fu_berlin.inf.ag_se.browser.swt;

import de.fu_berlin.inf.ag_se.browser.functions.IBrowserFunction;
import org.eclipse.swt.browser.BrowserFunction;

public class SWTBrowserFunction implements IBrowserFunction {
	private final BrowserFunction browserFunction;

	public SWTBrowserFunction(BrowserFunction swtFunction) {
		browserFunction = swtFunction;
	}

	@Override
	public void dispose() {
		browserFunction.dispose();
	}
}
