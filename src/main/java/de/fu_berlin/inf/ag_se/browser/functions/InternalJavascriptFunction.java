package de.fu_berlin.inf.ag_se.browser.functions;

/**
 * This class abstracts from the specific browser function
 * implementation like SWT's BrowserFunction class.
 * Those browser functions represent Java code that is callable
 * from Javascript.
 */
public abstract class InternalJavascriptFunction {

    private final String name;

	/**
	 * @param name the name of the Javascript function that calls into Java
	 */
    public InternalJavascriptFunction(String name) {
        this.name = name;
    }

	/**
	 * This method is to be overridden to supply the Java code
	 * @param arguments an array of Object, these are the parameters
	 *                  provided in Javascript
	 * @return the value to return back to Javascript
	 */
    public abstract Object function(Object[] arguments);

    public String getName() {
        return name;
    }
}
