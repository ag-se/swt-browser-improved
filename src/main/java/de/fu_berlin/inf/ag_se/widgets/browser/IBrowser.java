package de.fu_berlin.inf.ag_se.widgets.browser;

import java.io.File;
import java.net.URI;
import java.util.concurrent.Future;

import de.fu_berlin.inf.ag_se.utils.IConverter;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IAnchorListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IDNDListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IFocusListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IMouseListener;
import de.fu_berlin.inf.ag_se.widgets.browser.runner.JavaScriptExceptionListener;

/**
 * Instances of this interface denote a {@link org.eclipse.swt.widgets.Composite} that is based on a native {@link
 * org.eclipse.swt.browser.Browser}.
 *
 * @author bkahlert
 */
public interface IBrowser {

    /**
     * Opens the given URI.
     *
     * @param uri     the URI to open given as string
     * @param timeout the time after which the browser stops loading
     * @return true if page could be successfully loaded, false if the timeout was reached
     */
    public Future<Boolean> open(String uri, Integer timeout);

    /**
     * Opens the given URI.
     *
     * @param uri                 the URI to open given as string
     * @param timeout             the time after which the browser stops loading
     * @param pageLoadCheckScript this script must return true if the page is correctly loaded.
     *                            This is especially useful if some inner page setup takes place.
     * @return true if page could be successfully loaded, false if the timeout was reached
     */
    public Future<Boolean> open(String uri, Integer timeout,
                                String pageLoadCheckScript);

    /**
     * Opens the given URI.
     *
     * @param uri     the URI to load
     * @param timeout the time after which the browser stops loading
     * @return true if page could be successfully loaded, false if the timeout was reached
     */
    public Future<Boolean> open(URI uri, Integer timeout);

    /**
     * Opens the given URI.
     *
     * @param uri                 the URI to load
     * @param timeout             the time after which the browser stops loading
     * @param pageLoadCheckScript this script must return true if the page is correctly loaded.
     *                            This is especially useful if some inner page setup takes place.
     * @return true if page could be successfully loaded, false if the timeout was reached
     */
    public Future<Boolean> open(URI uri, Integer timeout,
                                String pageLoadCheckScript);

    /**
     * Opens a blank page.
     *
     * @return true if page could be successfully loaded, false if an error occurred
     */
    public Future<Boolean> openBlank();

    /**
     * Sets whether the browser may change its location.
     * If set to false the initially loaded URI may not be
     * changed.
     *
     * @param allowed true or false
     */
    public void setAllowLocationChange(boolean allowed);

    /**
     * Set a runnable to the executed just before the
     * URI is set internally.
     *
     * @param runnable the runnable to be executed
     */
    public void executeBeforeSettingURI(Runnable runnable);

    /**
     * Set a runnable to the executed just after the
     * URI is set internally.
     *
     * @param runnable the runnable to be executed
     */
    public void executeAfterSettingURI(Runnable runnable);

    /**
     * Set a runnable to be executed after the browser completed
     * loading the page. This takes place after the URI has been
     * set and all additional initializing has been done. Using
     * {@link #open(String, Integer, String)} allows to supply a
     * custom Javascript to check the completion.
     *
     * @param runnable the runnable to be executed
     */
    public void executeBeforeCompletion(Runnable runnable);

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
     * @throws Exception
     */
    void injectJavascriptFileImmediately(File javascriptFile) throws Exception;

    /**
     * Includes the given URI as a cascading style sheet.
     *
     * The injection is delayed until after the page is loaded completely.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if this has happened.
     *
     * @param uri the URI to the CSS file to be injected
     * @return a future to check whether the delayed execution has happened
     */
    public Future<Void> injectCssFile(URI uri);

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
     */
    public Future<Void> injectCss(String css);

    /**
     * Adds the given CSS code to the current website immediately.
     * For that a new style tag is added inside the HTML head.
     *
     * In contrast to the other injection methods this does not wait
     * for the browser to have finished loading.
     *
     * @param css the CSS to be injected as string     *
     * @throws Exception
     */
    public void injectCssImmediately(String css) throws Exception;

    /**
     * Injects the Javascript addressed by the given URI and returns
     * a {@link java.util.concurrent.Future} that blocks until
     * the script is completely loaded.
     * In contrast to {@link #run(java.net.URI)} the script tag is kept after the execution.
     *
     * @param scriptURI an URI to the Javascript code to be injected
     * @return a boolean future that blocks until script is loaded completely
     *
     * @ArbitraryThread may be called from whatever thread.
     */
    public Future<Boolean> inject(URI scriptURI);

    /**
     * Runs the Javascript contained in the given file in the browser as soon as
     * loading is completed.
     *
     * @param scriptFile file object pointing to the script to be executed
     * @return a boolean future that blocks until script is loaded completely
     *
     * @ArbitraryThread may be called from whatever thread.
     */
    public Future<Boolean> run(File scriptFile);

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
     * @ArbitraryThread may be called from whatever thread.
     */
    public Future<Boolean> run(URI scriptURI);

    /**
     * Runs the given Javascript in the browser as soon as loading is completed
     * and returns the evaluation's return value.
     *
     * @param script the Javascript code to be executed as string
     * @return an object future that blocks until script is loaded completely
     *
     * @ArbitraryThread may be called from whatever thread.
     */
    public Future<Object> run(String script);

    /**
     * Runs the given script in the browser as soon as loading is completed
     * and returns the evaluation's converted return value.
     *
     * @param script    the Javascript code to be executed as string
     * @param converter a converter for the return value
     * @return a future of the converted return value
     *
     * @ArbitraryThread may be called from whatever thread.
     */
    public <DEST> Future<DEST> run(String script, IConverter<Object, DEST> converter);

    /**
     * Runs the script contained in the given {@link java.io.File} in the browser immediately.
     * This means the file is not linked but its content is read and directly executed.
     *
     * In contrast to non-immediately run methods, it does not wait for the current URL to be loaded.
     *
     * @param scriptFile a file object pointing to the Javascript code
     * @ArbitraryThread may be called from whatever thread.
     */
    public void runContentImmediately(File scriptFile) throws Exception;

    /**
     * Runs the script contained in the given {@link java.io.File} in the browser immediately.
     * This means the file is not linked but its content is directly put into a script tag.
     *
     * In contrast to non-immediately run methods, it does not wait for the current URL to be loaded.
     *
     * @param scriptFile a file object pointing to the Javascript code
     * @ArbitraryThread may be called from whatever thread.
     */
    void runContentAsScriptTagImmediately(File scriptFile) throws Exception;

    /**
     * Runs the given script in the browser immediately and
     * returns the evaluation's converted return value.
     *
     *  In contrast to non-immediately run methods, it does not wait for the current URL to be loaded.
     * @param script    the Javascript code as string
     * @param converter a converter for the return value
     * @return a future of the converted return value
     *
     * @ArbitraryThread may be called from whatever thread.
     */
    public <DEST> DEST runImmediately(String script, IConverter<Object, DEST> converter) throws Exception;

    /**
     * Gets called if when the given script is about to be executed by the browser.
     *
     * @param script the Javascript code as string
     */
    public void scriptAboutToBeSentToBrowser(String script);

    /**
     * Gets called when the previously executed script finished execution.
     *
     * @param returnValue the object returned from the script
     */
    public void scriptReturnValueReceived(Object returnValue);

    /**
     * Returns a {@link java.util.concurrent.Future} indicating
     * if an element with the given id exists in the current website.
     *
     * @param id the element ID to look for
     * @return a boolean future containing the result of the search
     */
    public Future<Boolean> containsElementWithID(String id);

    /**
     * Returns a {@link java.util.concurrent.Future} indicating
     * if at least one element with the given name exists in
     * the current website.
     *
     * @param name the element name to look for
     * @return a boolean future containing the result of the search
     */
    public Future<Boolean> containsElementsWithName(String name);

    /**
     * Adds an {@link de.fu_berlin.inf.ag_se.widgets.browser.listener.IAnchorListener}
     * to the browser. This can be used to react to the hovering of anchor tags
     * @param anchorListener
     */
    public void addAnchorListener(IAnchorListener anchorListener);

    /**
     *
     * @param anchorListener
     */
    public void removeAnchorListener(IAnchorListener anchorListener);

    /**
     *
     * @param mouseListener
     */
    public void addMouseListener(IMouseListener mouseListener);

    /**
     *
     * @param mouseListener
     */
    public void removeMouseListener(IMouseListener mouseListener);

    /**
     *
     * @param focusListener
     */
    public void addFocusListener(IFocusListener focusListener);

    /**
     *
     * @param focusListener
     */
    public void removeFocusListener(IFocusListener focusListener);

    /**
     *
     * @param dNDListener
     */
    public void addDNDListener(IDNDListener dNDListener);

    /**
     *
     * @param dNDListener
     */
    public void removeDNDListener(IDNDListener dNDListener);

    /**
     * Sets the body's inner HTML.
     *
     * @param html
     * @return
     */
    public Future<Void> setBodyHtml(String html);

    /**
     * Returns the body's inner HTML.
     *
     * @return
     */
    public Future<String> getBodyHtml();

    /**
     * Returns the document's HTML
     */
    public Future<String> getHtml();

    /**
     * Inserts the given html at the current caret / cursor position.
     *
     * @return
     */
    public Future<Void> pasteHtmlAtCaret(String html);

    /**
     * Adds a border that signifies the {@link org.eclipse.swt.widgets.Control}'s focus.
     *
     * @return
     */
    public Future<Void> addFocusBorder();

    /**
     * Removes the border that signifies the {@link org.eclipse.swt.widgets.Control}'s focus.
     *
     * @return
     */
    public Future<Void> removeFocusBorder();

    /**
     * Adds a {@link de.fu_berlin.inf.ag_se.widgets.browser.runner.JavaScriptExceptionListener} that is notified if a
     * exception is thrown in the {@link org.eclipse.swt.browser.Browser} by code that was not invoked from the Java but the JavaScript
     * world (e.g. a click on a button invoking erroneous code).
     *
     * @param javaScriptExceptionListener
     */
    public void addJavaScriptExceptionListener(
            JavaScriptExceptionListener javaScriptExceptionListener);

    /**
     * Removed the given {@link de.fu_berlin.inf.ag_se.widgets.browser.runner.JavaScriptExceptionListener} from the list
     * of notified {@link de.fu_berlin.inf.ag_se.widgets.browser.runner.JavaScriptExceptionListener}s.
     *
     * @param javaScriptExceptionListener
     */
    public void removeJavaScriptExceptionListener(
            JavaScriptExceptionListener javaScriptExceptionListener);

    /**
     * @return the current url
     */
    String getUrl();
}