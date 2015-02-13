package de.fu_berlin.inf.ag_se.widgets.browser.exception;

import de.fu_berlin.inf.ag_se.utils.StringUtils;

public class ScriptExecutionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ScriptExecutionException(String script, Throwable e) {
		super("Could not run script: " + StringUtils.shorten(script), e);
	}

    public ScriptExecutionException(String script, String message) {
        super("Could not run script: " + StringUtils.shorten(script) + " because " + message);
    }

}
