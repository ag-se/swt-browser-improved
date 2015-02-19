package de.fu_berlin.inf.ag_se.browser.exception;

import de.fu_berlin.inf.ag_se.browser.JavascriptString;

public class JavaScriptException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String script;
	private final String filename;
	private final Long lineNumber;
	private final Long columnNumber;
	private final String detail;

	public JavaScriptException(String script, String filename, Long lineNumber,
			Long columnNumber, String detail) {
		super((script != null ? "JavaScript" : "*Asynchronous* JavaScript")
				+ " error occurred"
				+ (filename != null ? " in " + filename
						: " at unknown location") + "\n\tLine: "
				+ (lineNumber != null ? lineNumber : "unknown")
				+ "\n\tColumn: "
				+ (columnNumber != null ? columnNumber : "unknown")
				+ "\n\tDetail: " + detail
				+ (script != null ? "\n\tScript: " + script : ""));
		this.script = script;
		this.filename = filename;
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
		this.detail = detail;
	}

    /**
     * This method creates an {@link JavaScriptException} out of the arguments passed by
     * the {@link org.eclipse.swt.browser.Browser} to the callback specified using {@link JavascriptString#getExceptionForwardingScript(String)}.
     *
     * @param arguments
     * @return
     */
    public static JavaScriptException parseJavaScriptException(
            Object[] arguments) {
        String filename = (String) arguments[0];
        Long lineNumber = Math.round((Double) arguments[1]);
        Long columnNumber = Math.round((Double) arguments[2]);
        String detail = (String) arguments[3];

        return new JavaScriptException(null, filename, lineNumber,
                columnNumber, detail);
    }

    public String getScript() {
		return this.script;
	}

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return this.filename;
	}

	/**
	 * @return the line number
	 */
	public Long getLineNumber() {
		return this.lineNumber;
	}

	/**
	 * @return the column number
	 */
	public Long getColumnNumber() {
		return this.columnNumber;
	}

	/**
	 * @return the detail
	 */
	public String getDetail() {
		return this.detail;
	}

}
