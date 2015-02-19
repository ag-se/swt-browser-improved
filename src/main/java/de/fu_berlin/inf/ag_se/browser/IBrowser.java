package de.fu_berlin.inf.ag_se.browser;

import com.sun.istack.internal.Nullable;
import de.fu_berlin.inf.ag_se.browser.exception.ScriptExecutionException;
import de.fu_berlin.inf.ag_se.browser.functions.CallbackFunction;
import de.fu_berlin.inf.ag_se.browser.functions.Function;
import de.fu_berlin.inf.ag_se.browser.listener.JavaScriptExceptionListener;
import de.fu_berlin.inf.ag_se.browser.utils.IConverter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;

/**
 * This interface encapsulates a broad range of functionality of a web browser.
 * Its main features are the opening of URLs and the execution or injection of Javascript
 * or CSS code.
 * The difference between injection and execution is that generally injected code remains in
 * the website.
 * Most of the execution and injection methods are delayed until the website is fully loaded
 * because otherwise their result would be undetermined.
 */
@SuppressWarnings("UnusedDeclaration")
public interface IBrowser {

    /**
     * Opens the given URI.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param uri     the URI to open given as string
     * @param timeout the time after which the browser stops loading
     *                if zero or a negative value is supplied, no timeout is used
     * @return a boolean future containing true if page could be successfully loaded,
     * false if the timeout was reached
     *
     * @throws NullPointerException if the passed uri is null
     */
    Future<Boolean> open(String uri, int timeout);

    /**
     * Opens the given URI.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param uri                 the URI to open given as string
     * @param timeout             the time after which the browser stops loading
     *                            if zero or a negative value is supplied, no timeout is used
     * @param pageLoadCheckScript this script must return true if the page is correctly loaded.
     *                            This is especially useful if some inner page setup takes place.
     *                            May be null
     * @return a boolean future containing true if page could be successfully loaded,
     * false if the timeout was reached
     *
     * @throws NullPointerException if the passed uri is null
     */
    Future<Boolean> open(String uri, int timeout, @Nullable String pageLoadCheckScript);

    /**
     * Opens the given URI.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param uri     the URI to load
     * @param timeout the time after which the browser stops loading
     *                if zero or a negative value is supplied, no timeout is used
     * @return a boolean future containing true if page could be successfully loaded,
     * false if the timeout was reached
     *
     * @throws NullPointerException if the passed uri is null
     */
    Future<Boolean> open(URI uri, int timeout);

    /**
     * Opens the given URI and waits for the result.
     *
     * It must not be called from the UI thread as it is blocking.
     *
     * @param uri     the URI to load
     * @param timeout the time after which the browser stops loading
     *                if zero or a negative value is supplied, no timeout is used
     * @return true if page could be successfully loaded, false if the timeout was reached
     *
     * @throws NullPointerException  if the passed uri is null
     * @throws IllegalStateException if called from the UI thread
     */
    boolean syncOpen(URI uri, int timeout);

    /**
     * Opens the given URI and executes the given callback function after loading is complete.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param uri      the URI to load
     * @param timeout  the time after which the browser stops loading
     *                 if zero or a negative value is supplied, no timeout is used
     * @param callback the callback function to execute after loading
     * @return the boolean future returned from the user provided callback function
     *
     * @throws NullPointerException if the passed uri or callback is null
     */
    Future<Boolean> open(URI uri, int timeout, CallbackFunction<Boolean, Boolean> callback);

    /**
     * Opens the given URI and checks completion with a specified Javascript expression.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param uri                 the URI to load
     * @param timeout             the time after which the browser stops loading
     *                            if zero or a negative value is supplied, no timeout is used
     * @param pageLoadCheckScript this script must return true if the page is correctly loaded.
     *                            This is especially useful if some inner page setup takes place.
     *                            May be null
     * @return a boolean future containing true if page could be successfully loaded,
     * false if the timeout was reached
     *
     * @throws NullPointerException if the passed uri is null
     */
    Future<Boolean> open(URI uri, int timeout, @Nullable String pageLoadCheckScript);

    /**
     * Opens the given URI and checks completion with a specified Javascript expression.
     *
     * It must not be called from the UI thread as it is blocking.
     *
     * @param uri                 the URI to load
     * @param timeout             the time after which the browser stops loading
     *                            if zero or a negative value is supplied, no timeout is used
     * @param pageLoadCheckScript this script must return true if the page is correctly loaded.
     *                            This is especially useful if some inner page setup takes place.
     *                            May be null
     * @return true if page could be successfully loaded, false if the timeout was reached
     *
     * @throws NullPointerException  if the passed uri is null
     * @throws IllegalStateException if called from the UI thread
     */
    boolean syncOpen(URI uri, int timeout, @Nullable String pageLoadCheckScript);

    /**
     * Opens the given URI, checks completion with a specified Javascript expression,
     * and executes a callback function after that.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param uri      the URI to load
     * @param timeout  the time after which the browser stops loading
     *                 if zero or a negative value is supplied, no timeout is used
     * @param callback the callback function to execute after loading
     * @return the boolean future returned from the user provided callback function
     *
     * @throws NullPointerException if the passed uri is null
     */
    Future<Boolean> openWithCallback(URI uri, int timeout, @Nullable String pageLoadCheckScript,
                                     CallbackFunction<Boolean, Boolean> callback);

    /**
     * Opens a blank page.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @return a boolean future indicating whether the page could be loaded successfully
     */
    Future<Boolean> openBlank();

    /**
     * Checks if the current URI has been successfully loaded.
     * If the URI has been opened with a custom page check expression,
     * this expression is considered here.
     *
     * May be called from whatever thread.
     *
     * @return true is it is fully loaded, false otherwise
     */
    boolean isLoadingCompleted();

    /**
     * Returns the currently set URL.
     *
     * May be called from whatever thread.
     *
     * @return the current URL or an empty string if there is no current URL
     */
    String getUrl();

    /**
     * Blocks until the condition given by a Javascript string
     * evaluates to true.
     *
     * Must not be called from the UI thread as it is blocking.
     *
     * @param javaScriptExpression the Javascript expression to be evaluated
     * @throws NullPointerException if javaScriptExpression is null
     */
    void waitForCondition(String javaScriptExpression);

    /**
     * Continuously checks the given Javascript expression until it evaluates to true.
     * The returned future should be used to see when the condition is fulfilled.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param javaScriptExpression the Javascript expression to be evaluated
     * @return a future that is done when the condition is met
     *
     * @throws NullPointerException  if javaScriptExpression is null
     * @throws IllegalStateException if called from the UI thread
     */
    Future<Void> checkCondition(String javaScriptExpression);

    /**
     * Continuously checks the given Javascript expression until it evaluates to true.
     * The supplied callback function is callback when the condition is fulfilled.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param javaScriptExpression the Javascript expression to be evaluated
     * @param callback             the function to be executed when the condition is fulfilled
     * @return a future of the return value of the callback function
     *
     * @throws NullPointerException if javaScriptExpression or callback is null
     */
    <DEST> Future<DEST> executeWhenConditionIsMet(String javaScriptExpression, CallbackFunction<Void, DEST> callback);

    /**
     * Set a runnable to be executed after the browser completed
     * loading the page. This takes place after the URI has been
     * set and all additional initializing has been done.
     * If the URI has been opened with a custom page check expression,
     * this expression is considered here.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * The added runnable will be called from a non-UI thread.
     *
     * @param runnable the runnable to be executed
     * @throws NullPointerException if the runnable is null1
     */
    void executeAfterCompletion(Runnable runnable);

    /**
     * Adds the content of the Javascript contained in the given file
     * to the current website after loading has been completed.
     * For that, a new script tag is added to HTML head linking to
     * the given file.
     *
     * The execution of this method may be delayed.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if it has been executed.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param javascriptFile the file containing the Javascript to be injected
     * @return a future to check whether the delayed execution has happened
     *
     * @throws NullPointerException if javascriptFile is null
     */
    Future<Boolean> injectJavascript(File javascriptFile);

    /**
     * Injects the Javascript addressed by the given URI and returns
     * to the current website after loading has been completed.
     * For that, a new script tag is added to HTML head linking to
     * the given URI.
     * In contrast to {@link #run(java.net.URI)} the script tag is kept after the execution.
     *
     * The execution of this method may be delayed.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if it has been executed.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param scriptURI the URI to the Javascript code to be injected
     * @return a boolean future that blocks until script is loaded completely
     *
     * @throws NullPointerException if scriptURI is null
     */
    Future<Boolean> injectJavascript(URI scriptURI);

    /**
     * Injects the Javascript addressed by the given URI and returns
     * to the current website after loading has been completed.
     * For that, a new script tag is added to HTML head linking to
     * the given URI.
     * In contrast to {@link #run(java.net.URI)} the script tag is kept after the execution.
     *
     * The execution of this method may be delayed when the loading of the page is not complete.
     * This method blocks until the injection has been done.
     *
     * It may only be called from the UI thread if loading is completed.
     * Use {@link #isLoadingCompleted()} to check.
     *
     * @param scriptURI the URI to the Javascript code to be injected
     * @return a boolean indicating whether the injection was successful
     *
     * @throws NullPointerException  if scriptURI is null
     * @throws IllegalStateException if called from the UI thread and the browser is still loading
     */
    boolean syncInjectJavascript(URI scriptURI);

    /**
     * Includes the given URI as a cascading style sheet.
     * The injection is delayed until after the page is loaded completely.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if this has happened.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param cssURI the URI to the CSS to be injected
     * @return a boolean future to check whether the delayed execution was successful
     *
     * @throws NullPointerException if cssURI is null
     */
    Future<Boolean> injectCssURI(URI cssURI);

    /**
     * Includes the given URI as a cascading style sheet.
     * The injection is delayed until after the page is loaded completely.
     * This method blocks until the injection has been done.
     *
     * It may only be called from the UI thread if loading is completed.
     * Use {@link #isLoadingCompleted()} to check.
     *
     * @param cssURI the URI to the CSS to be injected
     * @return a boolean indicating whether the injection was successful
     *
     * @throws NullPointerException  if cssURI is null
     * @throws IllegalStateException if called from the UI thread and the browser is still loading
     */
    boolean syncInjectCssURI(URI cssURI);

    /**
     * Adds the given CSS code to current website.
     * For that a new style tag is added inside the HTML head.
     * The injection is delayed until after the page is loaded completely.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if this has happened.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param css the CSS to be injected as string
     * @return a boolean future to check whether the delayed execution was successful
     *
     * @throws NullPointerException if css is null
     */
    Future<Boolean> injectCss(String css);

    /**
     * Adds the given CSS code to current website.
     * For that a new style tag is added inside the HTML head.
     * The injection is delayed until after the page is loaded completely.
     * This method blocks until the injection has been done.
     *
     * It may only be called from the UI thread if loading is completed.
     * Use {@link #isLoadingCompleted()} to check.
     *
     * @param css the CSS to be injected as string
     * @return a boolean indicating whether the injection was successful
     *
     * @throws NullPointerException  if css is null
     * @throws IllegalStateException if called from the UI thread and the browser is still loading
     */
    boolean syncInjectCss(String css);

    /**
     * Adds the given CSS code to current website and executes the given callback function after that.
     * For that a new style tag is added inside the HTML head.
     * The injection is delayed until after the page is loaded completely.
     * When this has happened, the given callback function will be executed.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param css      the CSS to be injected as string
     * @param callback the callback function to be executed after the injection
     * @return a future containing the return value of the callback function
     *
     * @throws NullPointerException if css or callback is null
     */
    <DEST> Future<DEST> injectCss(String css, CallbackFunction<Boolean, DEST> callback);

    /**
     * Runs the Javascript contained in the given file in the browser as soon as
     * loading is completed.
     * This means the file is not linked but its content is read and directly executed.
     * The execution is delayed until after the page is loaded completely.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if it has been executed.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param scriptFile file object pointing to the script to be executed
     * @return a boolean future that blocks until script is has been executed
     *
     * @throws NullPointerException if scriptFile is null
     */
    Future<Boolean> run(File scriptFile);

    /**
     * Runs the script contained in the given {@link java.io.File} in the browser.
     * This means the file is not linked but its content is directly put into a script tag.
     * The execution is delayed until after the page is loaded completely.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if it has been executed.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param scriptFile a file object pointing to the Javascript code
     * @return a boolean future that blocks until script is has been executed
     *
     * @throws NullPointerException if scriptFile is null
     * @throws IOException          if a exception is thrown while accessing the file
     */
    Future<Boolean> runContentAsScriptTag(File scriptFile) throws IOException;

    /**
     * Runs the Javascript addressed by the given URI in the browser as soon as
     * loading is completed.
     * The execution is delayed until after the page is loaded completely.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if it has been executed.
     *
     * In contrast to {@link #injectJavascript(java.net.URI)} functionality made available
     * through the script does not persist.
     * Exception: If the resource is an actual file on the local file system, its
     * content will be run and therefore persisted to circumvent security restrictions.
     * To inject script libraries like jQuery {@link #injectJavascript(java.net.URI)} is recommended.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param scriptURI URI to the Javascript code to be executed
     * @return a boolean future that blocks until script is has been executed
     *
     * @throws NullPointerException if scriptURI is null
     */
    Future<Boolean> run(URI scriptURI);

    /**
     * Runs the given Javascript in the browser as soon as loading is completed
     * and returns the evaluation's return value.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if it has been executed.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param script the Javascript code to be executed as string
     * @return an object future that blocks until script is has been executed
     *
     * @throws NullPointerException if script is null
     */
    Future<Object> run(String script);

    /**
     * Runs the given Javascript in the browser as soon as loading is completed.
     * The execution is delayed until after the page is loaded completely.
     * This method blocks until it has been executed.
     *
     * It may only be called from the UI thread if loading is completed.
     * Use {@link #isLoadingCompleted()} to check.
     *
     * @param script Javascript to be evaluated as string
     * @return the result of the evaluation as Java object or null if cancelled
     *
     * @throws NullPointerException     if script is null
     * @throws IllegalStateException    if called from the UI thread and the browse is still loading
     * @throws ScriptExecutionException if an exception occurs while executing the script
     */
    Object syncRun(String script);

    /**
     * Runs the given Javascript in the browser as soon as loading is completed
     * and executes the callback that gets executed after the completion.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param script   the Javascript to be executed
     * @param callback the callback function to execute after completion
     * @return a future containing the return value of the callback function
     *
     * @throws NullPointerException if script or callback is null
     */
    <DEST> Future<DEST> run(String script, CallbackFunction<Object, DEST> callback);

    /**
     * Runs the given script in the browser as soon as loading is completed
     * and returns the evaluation's converted return value.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param script    the Javascript code to be executed as string
     * @param converter a converter for the return value
     * @return a future of the converted return value
     *
     * @throws NullPointerException if script or converter is null
     */
    <DEST> Future<DEST> run(String script, IConverter<Object, DEST> converter);

    /**
     * Runs the given Javascript in the browser as soon as loading is completed
     * and returns the evaluation's converted return value.
     * The execution is delayed until after the page is loaded completely.
     * This method blocks until it has been executed.
     *
     * It may only be called from the UI thread if loading is completed.
     * Use {@link #isLoadingCompleted()} to check.
     *
     * @param script    the Javascript code to be executed as string
     * @param converter a converter for the return value
     * @return a future of the converted return value
     *
     * @throws NullPointerException  if script or converter is null
     * @throws IllegalStateException if called from the UI thread and the browser is still loading
     */
    <DEST> DEST syncRun(String script, IConverter<Object, DEST> converter);

    /**
     * Runs the given script in the browser as soon as loading is completed
     * and then executes the callback function with the converted return value.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param script    the Javascript code to be executed as string
     * @param converter a converter for the return value
     * @param callback  the callback function to execution
     * @return a future containing the return value of the callback function
     *
     * @throws NullPointerException if script, converter, or callback is null
     */
    <T, DEST> Future<T> run(String script, IConverter<Object, DEST> converter, CallbackFunction<DEST, T> callback);

    /**
     * Sets a {@link Function} that is executed when a script is about to be executed by the browser.
     *
     * May be called from whatever thread.
     *
     * @param function the function to be executed with the script as parameter
     * @throws NullPointerException if function is null
     */
    void executeBeforeScript(Function<String> function);

    /**
     * Sets a {@link Function} that is executed when a script finishes execution.
     *
     * May be called from whatever thread.
     *
     * @param function the function to be executed with the return value of the last script execution
     * @throws NullPointerException if function is null
     */
    void executeAfterScript(Function<Object> function);

    /**
     * Returns a {@link java.util.concurrent.Future} indicating
     * if an element with the given id exists in the current website.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param id the element ID to look for
     * @return a boolean future containing the result of the search
     *
     * @throws NullPointerException if id is null
     */
    Future<Boolean> containsElementWithID(String id);

    /**
     * Returns a {@link java.util.concurrent.Future} indicating
     * if at least one element with the given name exists in
     * the current website.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param name the element name to look for
     * @return a boolean future containing the result of the search
     *
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
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param html a string representing the HTML body's new content
     * @return a future to check whether the delayed execution has successful
     *
     * @throws NullPointerException if html is null
     */
    Future<Boolean> setBodyHtml(String html);

    /**
     * Returns the body's inner HTML after
     * the page has been loaded.
     * The execution of this method may be delayed.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @return a string future containing the body's inner HTML
     */
    Future<String> getBodyHtml();

    /**
     * Returns the document's inner HTML after
     * the page has been loaded.
     * The execution of this method may be delayed.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @return a string future containing the document's inner HTML
     */
    Future<String> getHtml();

    /**
     * Executes the given callback function with the obtained HTML
     * The execution of this method may be delayed.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param callback the callback function to be executed
     * @return a future containing the return value of the callback function
     *
     * @throws NullPointerException if callback is null
     */
    <T> Future<T> getHtml(CallbackFunction<String, T> callback);

    /**
     * Inserts the given html at the current caret / cursor position
     * after the page has been loaded.
     * The execution of this method may be delayed.
     * The returned {@link java.util.concurrent.Future} can be used to check
     * if it has happened.
     *
     * May be called from whatever thread. Note, however, that {@link Future#get()} may not
     * be called from the UI thread unless {@link Future#isDone()} returns true.
     *
     * Important note: Anyone interested in whether this method has executed successfully or
     * in thrown exceptions has to query the returned future because this method does not throw any
     * exception (except by illegal use).
     *
     * @param html the HTML to paste as string
     * @return a future to check whether the delayed execution has happened
     *
     * @throws NullPointerException if html is null
     */
    Future<Boolean> pasteHtmlAtCaret(String html);

    /**
     * Sets whether the browser may change its location.
     * If set to false the initially loaded URI may not be
     * changed.
     *
     * May be called from whatever thread.
     *
     * @param allowed true or false
     */
    void setAllowLocationChange(boolean allowed);

    /**
     * Deactivates the selection of text inside the browser.
     *
     * May be called from whatever thread.
     */
    void deactivateTextSelections();

    /**
     * Creates a Javascript function that can call Java code.
     * At each call the given body gets executed.
     *
     * May be called from whatever thread.
     *
     * @param function     the Java code to be executed
     * @return the created function
     *
     * @throws NullPointerException if functionName or function is null
     */
    IBrowserFunction createBrowserFunction(IBrowserFunction function);

    /**
     * Adds a {@link JavaScriptExceptionListener} that is notified if a
     * exception is thrown in the browser by code that was not invoked from the Java but the JavaScript
     * world (e.g. a click on a button invoking erroneous code).
     *
     * May be called from whatever thread.
     *
     * @param exceptionListener the listener to be added
     * @throws NullPointerException if exceptionListener is null
     */
    void addJavaScriptExceptionListener(JavaScriptExceptionListener exceptionListener);

    /**
     * Removes the given {@link JavaScriptExceptionListener}
     * from the browser.
     *
     * May be called from whatever thread.
     *
     * @param exceptionListener the listener to be removed
     * @throws NullPointerException if exceptionListener is null
     */
    void removeJavaScriptExceptionListener(JavaScriptExceptionListener exceptionListener);
}