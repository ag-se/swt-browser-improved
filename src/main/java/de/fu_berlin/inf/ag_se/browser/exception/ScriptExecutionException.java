package de.fu_berlin.inf.ag_se.browser.exception;

import de.fu_berlin.inf.ag_se.browser.utils.StringUtils;

/**
 * In contract to {@link JavaScriptException} this exception is used for
 * exception occurring while executing Javascript from Java.
 */
public class ScriptExecutionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ScriptExecutionException(String script, Throwable e) {
		super("Could not run script: " + StringUtils.shorten(script), e);
	}

    public ScriptExecutionException(String script, String message) {
        super("Could not run script: " + StringUtils.shorten(script) + " because " + message);
    }

}
