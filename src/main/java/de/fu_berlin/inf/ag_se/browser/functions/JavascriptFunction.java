package de.fu_berlin.inf.ag_se.browser.functions;

import de.fu_berlin.inf.ag_se.browser.IBrowser;

/**
 * This class abstracts from the specific browser function
 * implementation like SWT's BrowserFunction class.
 * Those browser functions represent Java code that is callable
 * from Javascript.
 */
public abstract class JavascriptFunction extends InternalJavascriptFunction {

    protected IBrowser browser;

	/**
	 * @param name the name of the Javascript function that calls into Java
	 */
    public JavascriptFunction(String name) {
        super(name);
    }

    public void setBrowser(IBrowser browser) {
        this.browser = browser;
    }
}
