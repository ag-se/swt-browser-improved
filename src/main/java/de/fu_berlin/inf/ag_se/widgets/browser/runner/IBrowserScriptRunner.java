package de.fu_berlin.inf.ag_se.widgets.browser.runner;

import de.fu_berlin.inf.ag_se.utils.IConverter;

import java.io.File;
import java.net.URI;
import java.util.concurrent.Future;

/**
 * Implementations of this script can execute JavaScript using a browser.
 */
public interface IBrowserScriptRunner {

    /**
     * Injects the script addressed by the given {@link java.net.URI} and returns a {@link java.util.concurrent.Future} that blocks until
     * the script is completely loaded. <p> In contrast to {@link #run(java.net.URI)} the reference <code>&lt;script
     * src="..."&gt&lt;/script&gt</code> is kept.
     *
     * @param scriptURI an URI to the Javascript code to be injected
     * @return a boolean future that blocks until script is loaded completely
     * @ArbitraryThread may be called from whatever thread.
     */
    public Future<Boolean> inject(URI scriptURI);

    /**
     * Runs the script contained in the given {@link java.io.File} in the browser as soon as its content is loaded.
     *
     * @param script file object pointing to the script to be executed
     * @return a boolean future that blocks until script is loaded completely
     * @ArbitraryThread may be called from whatever thread.
     */
    public Future<Boolean> run(File script);

    /**
     * Runs the script contained in the given {@link java.net.URI} in the browser as soon as its content is loaded. <p> In contrast to
     * {@link #inject(java.net.URI)} functionality made available through the script does not persist. To inject script libraries like
     * jQuery {@link #inject(java.net.URI)} is recommended. <b>Exception: If the resource is actually a file on the local file system, its
     * content will be run and therefore persist to circumvent security restrictions.
     *
     * @param scriptURI an URI to the Javascript code to be executed
     * @return a boolean future that blocks until script is loaded completely
     * @ArbitraryThread may be called from whatever thread.
     */
    public Future<Boolean> run(URI scriptURI);

    /**
     * Runs the given script in the browser as soon as its content is loaded and returns the evaluation's return value.
     *
     * @param script the Javascript code as string
     * @return an object future that blocks until script is loaded completely
     * @ArbitraryThread may be called from whatever thread.
     */
    public Future<Object> run(String script);

    /**
     * Runs the given script in the browser as soon as its content is loaded and returns the evaluation's converted return value.
     *
     * @param script    the Javascript code as string
     * @param converter a converter for the return value
     * @return a future of the converted return value
     * @ArbitraryThread may be called from whatever thread.
     */
    public <DEST> Future<DEST> run(String script, IConverter<Object, DEST> converter);

    /**
     * Runs the script <b>contained</b> in the given {@link java.io.File} in the browser immediately. This means the file is not linked but
     * its content is read and directly executed. It does not wait for the current URL to be loaded.
     *
     * @param scriptFile a file object pointing to the Javascript code
     * @ArbitraryThread may be called from whatever thread.
     */

    public void runContentsImmediately(File scriptFile) throws Exception;

    /**
     * Runs the script <b>contained</b> in the given {@link java.io.File} in the browser immediately. This means the file is not linked but
     * its content is directly put into a <code>script</code> tag. It does not wait for the current URL to be loaded.
     *
     * @param scriptFile a file object pointing to the Javascript code
     * @ArbitraryThread may be called from whatever thread.
     */
    void runContentsAsScriptTagImmediately(File scriptFile) throws Exception;

    /**
     * Runs the given script in the browser immediately and returns the evaluation's converted return value. It does not wait for the
     * current URL to be loaded.
     *
     * @param script    the Javascript code as string
     * @param converter a converter for the return value
     * @return a future of the converted return value
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

}
