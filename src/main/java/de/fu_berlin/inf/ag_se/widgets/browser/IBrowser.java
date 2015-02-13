package de.fu_berlin.inf.ag_se.widgets.browser;

import com.sun.istack.internal.Nullable;
import de.fu_berlin.inf.ag_se.utils.IConverter;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.ScriptExecutionException;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;

/**
 * This interface encapsulates a broad range of functionality of a web browser.
 */
@SuppressWarnings("UnusedDeclaration")
public interface IBrowser {

    /**
     * Opens the given URI.
     *
     * @param uri     the URI to open given as string
     * @param timeout the time after which the browser stops loading
     *                if zero or a negative value is supplied, no timeout is used
     * @return true if page could be successfully loaded, false if the timeout was reached
     *
     * @throws NullPointerException if the passed uri is null
     */
    Future<Boolean> open(String uri, int timeout);

    /**
     * Opens the given URI.
     *
     * @param uri                 the URI to open given as string
     * @param timeout             the time after which the browser stops loading
     *                            if zero or a negative value is supplied, no timeout is used
     * @param pageLoadCheckScript this script must return true if the page is correctly loaded.
     *                            This is especially useful if some inner page setup takes place.
     *                            May be null
     * @return true if page could be successfully loaded, false if the timeout was reached
     *
     * @throws NullPointerException if the passed uri is null
     */
    Future<Boolean> open(String uri, int timeout, @Nullable String pageLoadCheckScript);

    /**
     * Opens the given URI.
     *
     * @param uri     the URI to load
     * @param timeout the time after which the browser stops loading
     *                if zero or a negative value is supplied, no timeout is used
     * @return true if page could be successfully loaded, false if the timeout was reached
     *
     * @throws NullPointerException if the passed uri is null
     */
    Future<Boolean> open(URI uri, int timeout);

    /**
     * Opens the given URI.
     *
     * @param uri                 the URI to load
     * @param timeout             the time after which the browser stops loading
     *                            if zero or a negative value is supplied, no timeout is used
     * @param pageLoadCheckScript this script must return true if the page is correctly loaded.
     *                            This is especially useful if some inner page setup takes place.
     *                            May be null
     * @return true if page could be successfully loaded, false if the timeout was reached
     *
     * @throws NullPointerException if the passed uri is null
     */
    Future<Boolean> open(URI uri, int timeout, @Nullable String pageLoadCheckScript);

    /**
     * Opens a blank page.
     *
     * @return true if page could be successfully loaded, false if an error occurred
     */
    Future<Boolean> openBlank();

    /**
     * Set a runnable to the executed just before the
     * URI is set internally.
     *
     * @param runnable the runnable to be executed
     * @throws NullPointerException if the runnable is null
     */
    void executeBeforeSettingURI(Runnable runnable);

    /**
     * Set a runnable to the executed just after the
     * URI is set internally.
     *
     * @param runnable the runnable to be executed
     * @throws NullPointerException if the runnable is null
     */
    void executeAfterSettingURI(Runnable runnable);

    /**
     * Checks if the current URI has been successfully loaded.
     *
     * @return true is it is fully loaded, false otherwise
     */
    boolean isLoadingCompleted();

    /**
     * Returns the currently set URL.
     *
     * May be called from whatever thread.
     *
     * @return the current URL or an empty <code>String</code> if there is no current URL
     */
    String getUrl();

    /**
     * Blocks until the condition given by a Javascript string
     * evaluates to true.
     *
     * @param javaScriptExpression the Javascript to be evaluation
     * @throws NullPointerException if javaScriptExpression is null
     */
    void waitForCondition(String javaScriptExpression);

    /**
     * Set a runnable to be executed after the browser completed
     * loading the page. This takes place after the URI has been
     * set and all additional initializing has been done. Using
     * {@link #open(String, int, String)} allows to supply a
     * custom Javascript to check the completion.
     *
     * @param runnable the runnable to be executed
     * @throws NullPointerException if the runnable is null1
     */
    void executeBeforeCompletion(Runnable runnable);

    /**
     * Adds the content of the Javascript contained in the given file
     * to the current website after loading has been completed.
     * For that a new script tag is added to HTML head linking to
     * the given file.
     *
     * The difference to {@link #injectJavascriptFileImmediately(java.io.File)} is
     * that this method delays injection until after the page is loaded.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if this has happened.
     *
     * @param javascriptFile the file containing the Javascript to be injected
     * @return a future to check whether the delayed execution has happened
     *
     * @throws NullPointerException if javascriptFile is null
     */
    Future<Void> injectJavascriptFile(File javascriptFile);

    /**
     * Adds the content of the Javascript contained in the given file
     * to the current website immediately.
     * For that a new script tag is added to HTML head linking to
     * the given file.
     *
     * The difference to {@link #injectJavascriptFile(java.io.File)} is
     * that this method injects the script immediately.
     *
     * @param javascriptFile the file containing the Javascript to be injected
     * @throws NullPointerException if javascriptFile is null
     */
    void injectJavascriptFileImmediately(File javascriptFile);

    /**
     * Includes the given URI as a cascading style sheet.
     *
     * The injection is delayed until after the page is loaded completely.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if this has happened.
     *
     * @param uri the URI to the CSS file to be injected
     * @return a future to check whether the delayed execution has happened
     *
     * @throws NullPointerException if uri is null
     */
    Future<Void> injectCssFile(URI uri);

    /**
     * Includes the given URI as a cascading style sheet immediately.
     *
     * @param uri the URI to the CSS file to be injected
     * @throws NullPointerException if uri is null
     */
    void injectCssFileImmediately(URI uri);

    /**
     * Adds the given CSS code to current website.
     * For that a new style tag is added inside the HTML head.
     *
     * The injection is delayed until after the page is loaded completely.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if this has happened.
     *
     * @param css the CSS to be injected as string
     * @return a future to check whether the delayed execution has happened
     *
     * @throws NullPointerException if css is null
     */
    Future<Void> injectCss(String css);

    /**
     * Adds the given CSS code to the current website immediately.
     * For that a new style tag is added inside the HTML head.
     *
     * In contrast to the other injection methods this does not wait
     * for the browser to have finished loading.
     *
     * @param css the CSS to be injected as string
     * @throws NullPointerException if css is null
     */
    void injectCssImmediately(String css);

    /**
     * Injects the Javascript addressed by the given URI and returns
     * a {@link java.util.concurrent.Future} that blocks until
     * the script is completely loaded.
     * In contrast to {@link #run(java.net.URI)} the script tag is kept after the execution.
     *
     * @param scriptURI an URI to the Javascript code to be injected
     * @return a boolean future that blocks until script is loaded completely
     *
     * @throws NullPointerException if scriptURI is null
     * @ArbitraryThread may be called from whatever thread.
     */
    Future<Boolean> inject(URI scriptURI);

    /**
     * Runs the Javascript contained in the given file in the browser as soon as
     * loading is completed.
     *
     * @param scriptFile file object pointing to the script to be executed
     * @return a boolean future that blocks until script is loaded completely
     *
     * @throws NullPointerException if scriptFile is null
     * @ArbitraryThread may be called from whatever thread.
     */
    Future<Boolean> run(File scriptFile);

    /**
     * Runs the Javascript addressed by the given URI in the browser as soon as
     * loading is completed.
     *
     * In contrast to {@link #inject(java.net.URI)} functionality made available
     * through the script does not persist.
     * Exception: If the resource is an actual file on the local file system, its
     * content will be run and therefore persisted to circumvent security restrictions.
     * To inject script libraries like jQuery {@link #inject(java.net.URI)} is recommended.
     *
     * @param scriptURI URI to the Javascript code to be executed
     * @return a boolean future that blocks until script is loaded completely
     *
     * @throws NullPointerException if scriptURI is null
     * @ArbitraryThread may be called from whatever thread.
     */
    Future<Boolean> run(URI scriptURI);

    /**
     * Runs the given Javascript in the browser as soon as loading is completed
     * and returns the evaluation's return value.
     *
     * @param script the Javascript code to be executed as string
     * @return an object future that blocks until script is loaded completely
     *
     * @throws NullPointerException if script is null
     * @ArbitraryThread may be called from whatever thread.
     */
    Future<Object> run(String script);

    /**
     * This method is blocking as it waits for the result of the evaluation.
     * It must not be called from the SWT UI thread.
     * May return null.
     *
     * @param script Javascript to be evaluated as string
     * @return the result of the evaluation as Java object or null if an error occurred
     *
     * @throws NullPointerException     if script is null
     * @throws IllegalStateException    if this method is called from the UI thread
     * @throws ScriptExecutionException if an exception occurs while executing the script
     */
    Object syncRun(String script);

    /**
     * This methods runs the given Javascript asynchronously and registers a callback
     * that gets executed after the completion.
     *
     * @param script the Javascript to be executed
     * @param callback the callback function to execute after script completion
     * @throws NullPointerException if script or callback is null
     */
    void asyncRun(String script, CallbackFunction<Object> callback);

    /**
     * Runs the given script in the browser as soon as loading is completed
     * and returns the evaluation's converted return value.
     *
     * @param script    the Javascript code to be executed as string
     * @param converter a converter for the return value
     * @return a future of the converted return value
     * @throws NullPointerException if script or converter is null
     *
     * @ArbitraryThread may be called from whatever thread.
     */
    <DEST> Future<DEST> run(String script, IConverter<Object, DEST> converter);

    /**
     * Runs the script contained in the given {@link java.io.File} in the browser immediately.
     * This means the file is not linked but its content is read and directly executed.
     *
     * In contrast to non-immediately run methods, it does not wait for the current URL to be loaded.
     *
     * @param scriptFile a file object pointing to the Javascript code
     * @throws IOException if an error occurred while accessing the given file
     * @throws NullPointerException if scriptFile is null
     * @ArbitraryThread may be called from whatever thread.
     */
    void runContentImmediately(File scriptFile) throws IOException;

    /**
     * Runs the script contained in the given {@link java.io.File} in the browser immediately.
     * This means the file is not linked but its content is directly put into a script tag.
     *
     * In contrast to non-immediately run methods, it does not wait for the current URL to be loaded.
     *
     * @param scriptFile a file object pointing to the Javascript code
     * @throws IOException if an error occurred while accessing the given file
     * @throws NullPointerException if scriptFile is null
     * @ArbitraryThread may be called from whatever thread.
     */
    void runContentAsScriptTagImmediately(File scriptFile) throws IOException;

    /**
     * Runs the given script in the browser immediately and
     * returns the evaluation's converted return value.
     *
     * In contrast to non-immediately run methods, it does not wait for the current URL to be loaded.
     *
     * @param script    the Javascript code as string
     * @param converter a converter for the return value
     * @return a future of the converted return value
     * @throws NullPointerException if script or converter is null
     *
     * @ArbitraryThread may be called from whatever thread.
     */
    <DEST> DEST runImmediately(String script, IConverter<Object, DEST> converter);

    /**
     * Sets a {@link de.fu_berlin.inf.ag_se.widgets.browser.ParametrizedRunnable}
     * that is executed if when a script is about to be executed by the browser.
     *
     * @param runnable the runnable to be executed with the script as parameter
     * @throws NullPointerException if runnable is null
     */
    void executeBeforeScript(Function<String> runnable);

    /**
     * Sets a {@link de.fu_berlin.inf.ag_se.widgets.browser.ParametrizedRunnable}
     * to get executed when a script finishes execution.
     *
     * @param runnable the runnable to be executed with the return value of the last script execution
     * @throws NullPointerException if runnable is null
     */
    void executeAfterScript(Function<Object> runnable);

    /**
     * Returns a {@link java.util.concurrent.Future} indicating
     * if an element with the given id exists in the current website.
     *
     * @param id the element ID to look for
     * @return a boolean future containing the result of the search
     * @throws NullPointerException if id is null
     */
    Future<Boolean> containsElementWithID(String id);

    /**
     * Returns a {@link java.util.concurrent.Future} indicating
     * if at least one element with the given name exists in
     * the current website.
     *
     * @param name the element name to look for
     * @return a boolean future containing the result of the search
     * @throws NullPointerException if name is null
     */
    Future<Boolean> containsElementsWithName(String name);

    /**
     * Sets the body's inner HTML to the given string after
     * the page has been loaded.
     * The execution of this method may be delayed.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if it has happened.
     *
     * @param html a string representing the HTML body's new content
     * @return a future to check whether the delayed execution has happened
     * @throws NullPointerException if html is null
     */
    Future<Void> setBodyHtml(String html);

    /**
     * Returns the body's inner HTML after
     * the page has been loaded.
     * The execution of this method may be delayed.
     *
     * @return a string future containing the body's inner HTML
     */
    Future<String> getBodyHtml();

    /**
     * Returns the document's inner HTML after
     * the page has been loaded.
     * The execution of this method may be delayed.
     *
     * @return a string future containing the document's inner HTML
     */
    Future<String> getHtml();

    /**
     * Inserts the given html at the current caret / cursor position
     * after the page has been loaded.
     * The execution of this method may be delayed.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if it has happened.
     * @param html the HTML to paste as string
     * @return a future to check whether the delayed execution has happened
     * @throws NullPointerException if html is null
     */
    Future<Void> pasteHtmlAtCaret(String html);

    /**
     * Adds a border that signifies the {@link org.eclipse.swt.widgets.Control}'s focus.
     * The execution of this method may be delayed.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if it has happened.
     *
     * @return a future to check whether the delayed execution has happened
     */
    Future<Void> addFocusBorder();

    /**
     * Removes the border that signifies the {@link org.eclipse.swt.widgets.Control}'s focus.
     * The execution of this method may be delayed.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if it has happened.
     *
     * @return a future to check whether the delayed execution has happened
     */
    Future<Void> removeFocusBorder();

    /**
     * Sets whether the browser may change its location.
     * If set to false the initially loaded URI may not be
     * changed.
     *
     * @param allowed true or false
     */
    void setAllowLocationChange(boolean allowed);

    /**
     * Deactivates the selection of text inside the browser.
     */
    void deactivateTextSelections();

    /**
     * Creates a Javascript function that can call Java code.
     * At each call the given body gets executed.
     *
     * @param functionName the of the function in Javascript
     * @param function     the Java code to be executed
     * @return the created function
     * @throws NullPointerException if functionName or function is null
     */
    IBrowserFunction createBrowserFunction(String functionName,
                                           IBrowserFunction function);

    /**
     * Adds an {@link de.fu_berlin.inf.ag_se.widgets.browser.listener.IAnchorListener}
     * to the browser.
     * This can be used to react to the hovering of anchor tags
     *
     * @param anchorListener the listener to be added
     * @throws NullPointerException if anchorListener is null
     */
    void addAnchorListener(IAnchorListener anchorListener);

    /**
     * Removes the given {@link de.fu_berlin.inf.ag_se.widgets.browser.listener.IAnchorListener}.
     *
     * @param anchorListener the listener to be removed
     * @throws NullPointerException if anchorListener is null
     */
    void removeAnchorListener(IAnchorListener anchorListener);

    /**
     * Adds an {@link de.fu_berlin.inf.ag_se.widgets.browser.listener.IMouseListener}
     * to the browser.
     * This can be used to react to mouse events inside the browser
     *
     * @param mouseListener the listener to be added
     * @throws NullPointerException if mouseListener is null
     */
    void addMouseListener(IMouseListener mouseListener);

    /**
     * Removes the given {@link de.fu_berlin.inf.ag_se.widgets.browser.listener.IMouseListener}.
     *
     * @param mouseListener the listener to be removed
     * @throws NullPointerException if mouseListener is null
     */
    void removeMouseListener(IMouseListener mouseListener);

    /**
     * Adds an {@link de.fu_berlin.inf.ag_se.widgets.browser.listener.IFocusListener}
     * to the browser.
     * This can be used to react to focus gaining and focus losing of HTML elements.
     *
     * @param focusListener the listener to be added
     * @throws NullPointerException if focusListener is null
     */
    void addFocusListener(IFocusListener focusListener);

    /**
     * Removes the given {@link de.fu_berlin.inf.ag_se.widgets.browser.listener.IFocusListener}.
     *
     * @param focusListener the listener to be removed
     * @throws NullPointerException if focusListener is null
     */
    void removeFocusListener(IFocusListener focusListener);

    /**
     * Adds an {@link de.fu_berlin.inf.ag_se.widgets.browser.listener.IDNDListener}
     * to the browser.
     * This can be used to react to drag and drop events.
     *
     * @param dNDListener the listener to be added
     * @throws NullPointerException if dNDListener is null
     */
    void addDNDListener(IDNDListener dNDListener);

    /**
     * Removes the given {@link de.fu_berlin.inf.ag_se.widgets.browser.listener.IDNDListener}.
     *
     * @param dNDListener the listener to be removed
     * @throws NullPointerException if dNDListener is null
     */
    void removeDNDListener(IDNDListener dNDListener);

    /**
     * Adds a {@link de.fu_berlin.inf.ag_se.widgets.browser.listener.JavaScriptExceptionListener} that is notified if a
     * exception is thrown in the browser by code that was not invoked from the Java but the JavaScript
     * world (e.g. a click on a button invoking erroneous code).
     *
     * @param exceptionListener the listener to be added
     * @throws NullPointerException if exceptionListener is null
     */
    void addJavaScriptExceptionListener(JavaScriptExceptionListener exceptionListener);

    /**
     * Removes the given {@link de.fu_berlin.inf.ag_se.widgets.browser.listener.JavaScriptExceptionListener}
     * from the browser.
     *
     * @param exceptionListener the listener to be removed
     * @throws NullPointerException if exceptionListener is null
     */
    void removeJavaScriptExceptionListener(JavaScriptExceptionListener exceptionListener);
}