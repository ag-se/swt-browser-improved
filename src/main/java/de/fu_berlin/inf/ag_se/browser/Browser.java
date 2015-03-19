package de.fu_berlin.inf.ag_se.browser;

import com.sun.istack.internal.Nullable;
import de.fu_berlin.inf.ag_se.browser.functions.CallbackFunction;
import de.fu_berlin.inf.ag_se.browser.functions.Function;
import de.fu_berlin.inf.ag_se.browser.functions.IBrowserFunction;
import de.fu_berlin.inf.ag_se.browser.functions.JavascriptFunction;
import de.fu_berlin.inf.ag_se.browser.listener.JavaScriptExceptionListener;
import de.fu_berlin.inf.ag_se.browser.threading.CompletedFuture;
import de.fu_berlin.inf.ag_se.browser.utils.IConverter;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class is a {@link org.eclipse.swt.widgets.Composite} that provides extended functionality to
 * the {@link org.eclipse.swt.browser.Browser}.
 *
 * Its features include reporting Javascript errors back to Java,
 * checking the loading state dependent on custom Javascript conditions,
 * delaying the execution of Javascript code based on the loading state,
 * and providing listener for different kinds of Javascript events.
 */
public class Browser implements IBrowser {

    private static Logger LOGGER = Logger.getLogger(Browser.class);
    protected final InternalBrowserWrapper internalBrowser;

    private boolean textSelectionsDisabled = false;

    public Browser(InternalBrowserWrapper internalBrowser) {
        this.internalBrowser = internalBrowser;
        executeAfterCompletion(new Runnable() {
            @Override
            public void run() {
                if (textSelectionsDisabled) {
                    try {
                        injectCss(JavascriptString.createCssToDisableTextSelection());
                    } catch (RuntimeException e) {
                        LOGGER.error(e);
                    }
                }
            }
        });
    }

    @Override
    public Future<Boolean> open(String uri, int timeout) {
        checkNotNull(uri);
        return open(uri, timeout, null);
    }

    @Override
    public Future<Boolean> open(String uri, int timeout, @Nullable String pageLoadCheckScript) {
        checkNotNull(uri);
        return internalBrowser.open(uri, timeout, pageLoadCheckScript);
    }

    @Override
    public Future<Boolean> open(URI uri, int timeout) {
        checkNotNull(uri);
        return open(uri.toString(), timeout, null);
    }

    @Override
    public boolean syncOpen(URI uri, int timeout) {
        checkNotNull(uri);
        checkNotUIThread();
        return syncOpen(uri, timeout, null);
    }

    @Override
    public Future<Boolean> open(URI uri, int timeout, CallbackFunction<Boolean, Boolean> callback) {
        checkNotNull(uri);
        checkNotNull(callback);
        return openWithCallback(uri, timeout, null, callback);
    }

    @Override
    public Future<Boolean> open(URI uri, int timeout, @Nullable String pageLoadCheckExpression) {
        checkNotNull(uri);
        return open(uri.toString(), timeout, pageLoadCheckExpression);
    }

    @Override
    public boolean syncOpen(URI uri, int timeout, @Nullable String pageLoadCheckScript) {
        checkNotNull(uri);
        checkNotUIThread();
        return internalBrowser.syncOpen(uri, timeout, pageLoadCheckScript);
    }

    @Override
    public Future<Boolean> openWithCallback(URI uri, int timeout, @Nullable String pageLoadCheckScript,
                                            CallbackFunction<Boolean, Boolean> callback) {
        checkNotNull(uri);
        checkNotNull(callback);
        return internalBrowser.openWithCallback(uri, timeout, pageLoadCheckScript, callback);
    }

    @Override
    public Future<Boolean> openBlank() {
        return open(BrowserUtils.createBlankHTMLFile(), 6000);
    }

    @Override
    public boolean isLoadingCompleted() {
        return internalBrowser.isLoadingCompleted();
    }

    @Override
    public String getUrl() {
        return internalBrowser.getUrl();
    }

    @Override
    public void waitForCondition(String javaScriptExpression) {
        checkNotNull(javaScriptExpression);
        checkNotUIThread();
        internalBrowser.waitForCondition(javaScriptExpression);
    }

    @Override
    public Future<Void> checkCondition(String javaScriptExpression) {
        checkNotNull(javaScriptExpression);
        return internalBrowser.checkCondition(javaScriptExpression);
    }

    @Override
    public <DEST> Future<DEST> executeWhenConditionIsMet(String javaScriptExpression, CallbackFunction<Void, DEST> callback) {
        checkNotNull(javaScriptExpression);
        checkNotNull(callback);
        return internalBrowser.executeWhenConditionIsMet(javaScriptExpression, callback);
    }

    @Override
    public void executeAfterCompletion(Runnable runnable) {
        checkNotNull(runnable);
        internalBrowser.executeAfterCompletion(runnable);
    }

    @Override
    public Future<Boolean> injectJavascript(File javascriptFile) {
        checkNotNull(javascriptFile);
        return internalBrowser.injectJavascript(javascriptFile);
    }

    @Override
    public Future<Boolean> injectJavascript(URI scriptURI) {
        checkNotNull(scriptURI);
        return internalBrowser.injectJavascript(scriptURI);
    }

    @Override
    public boolean syncInjectJavascript(URI scriptURI) {
        checkNotNull(scriptURI);
        if (!isLoadingCompleted()) {
            checkNotUIThread();
        }
        return internalBrowser.syncInjectJavascript(scriptURI);
    }

    @Override
    public Future<Boolean> injectCssURI(URI cssURI) {
        checkNotNull(cssURI);
        return internalBrowser.injectCss(cssURI);
    }

    @Override
    public boolean syncInjectCssURI(URI cssURI) {
        checkNotNull(cssURI);
        if (!isLoadingCompleted()) {
            checkNotUIThread();
        }
        return internalBrowser.syncInjectCssURI(cssURI);
    }

    @Override
    public Future<Boolean> injectCss(String css) {
        checkNotNull(css);
        return internalBrowser.injectCss(css);
    }

    @Override
    public boolean syncInjectCss(String css) {
        checkNotNull(css);
        if (!isLoadingCompleted()) {
            checkNotUIThread();
        }
        return internalBrowser.syncInjectCss(css);
    }

    @Override
    public <DEST> Future<DEST> injectCss(String css, CallbackFunction<Boolean, DEST> callback) {
        checkNotNull(css);
        checkNotNull(callback);
        return internalBrowser.injectCss(css, callback);
    }

    @Override
    public Future<Boolean> run(File scriptFile) {
        checkNotNull(scriptFile);
        return internalBrowser.run(scriptFile);
    }

    @Override
    public Future<Boolean> runContentAsScriptTag(File scriptFile) throws IOException {
        checkNotNull(scriptFile);
        return internalBrowser.runContentAsScriptTag(scriptFile);
    }

    @Override
    public Future<Boolean> run(URI scriptURI) {
        checkNotNull(scriptURI);
        return internalBrowser.run(scriptURI);
    }

    @Override
    public Future<Object> run(String script) {
        checkNotNull(script);
        return internalBrowser.run(script);
    }

    @Override
    public Object syncRun(String script) {
        checkNotNull(script);
        if (!isLoadingCompleted()) {
            checkNotUIThread();
        }
        return internalBrowser.syncRun(script);
    }

    @Override
    public <DEST> Future<DEST> run(String script, CallbackFunction<Object, DEST> callback) {
        checkNotNull(script);
        checkNotNull(callback);
        return internalBrowser.run(script, callback);
    }

    @Override
    public <DEST> Future<DEST> run(String script, IConverter<Object, DEST> converter) {
        checkNotNull(script);
        checkNotNull(converter);
        return internalBrowser.run(script, converter);
    }

    @Override
    public <DEST> DEST syncRun(String script, IConverter<Object, DEST> converter) {
        checkNotNull(script);
        checkNotNull(converter);
        if (!isLoadingCompleted()) {
            checkNotUIThread();
        }
        return (DEST) internalBrowser.syncRun(script, converter);
    }

    /**
     * Use with care!
     * This method is just for internal usage. It may be useful for browser extensions' afterComplete scripts as they get executed before
     * the status
     * is set to loaded, but the browser status is fairly defined.
     *
     * @param script    the script to be executed
     * @param converter the converter for the return value
     * @return the converted return value
     */
    protected <DEST> DEST runImmediately(String script, IConverter<Object, DEST> converter) {
        return (DEST) internalBrowser.runImmediately(script, converter);
    }

    @Override
    public <T, DEST> Future<T> run(String script, IConverter<Object, DEST> converter, CallbackFunction<DEST, T> callback) {
        checkNotNull(script);
        checkNotNull(converter);
        checkNotNull(callback);
        return internalBrowser.runWithCallback(run(script, converter), callback);
    }

    @Override
    public void executeBeforeScript(Function<String> function) {
        checkNotNull(function);
        internalBrowser.executeBeforeScript(function);
    }

    @Override
    public void executeAfterScript(Function<Object> function) {
        internalBrowser.executeAfterScript(function);
    }

    @Override
    public Future<Boolean> containsElementWithID(String id) {
        checkNotNull(id);
        return run("return document.getElementById('" + id + "') != null", IConverter.CONVERTER_BOOLEAN);
    }

    @Override
    public Future<Boolean> containsElementsWithName(String name) {
        checkNotNull(name);
        return run("return document.getElementsByName('" + name + "').length > 0", IConverter.CONVERTER_BOOLEAN);
    }

    @Override
    public Future<Boolean> setBodyHtml(String html) {
        checkNotNull(html);
        return run("document.body.innerHTML = ('" + JavascriptString.escape(html) + "');", IConverter.CONVERTER_BOOLEAN);
    }

    @Override
    public Future<String> getBodyHtml() {
        return run("return document.body.innerHTML", IConverter.CONVERTER_STRING);
    }

    @Override
    public Future<String> getHtml() {
        return run("return document.documentElement.outerHTML", IConverter.CONVERTER_STRING);
    }

    @Override
    public <T> Future<T> getHtml(CallbackFunction<String, T> callback) {
        checkNotNull(callback);
        return internalBrowser.runWithCallback(getHtml(), callback);
    }

    @Override
    public Future<Boolean> pasteHtmlAtCaret(String html) {
        checkNotNull(html);
        try {
            File js = File.createTempFile("paste", ".js");
            FileUtils.write(js, JavascriptString.createJavascriptForInsertingHTML(html));
            return injectJavascript(js);
        } catch (IOException e) {
            return new CompletedFuture<Boolean>(null, e);
        }
    }

    @Override
    public void setAllowLocationChange(boolean allowed) {
        internalBrowser.setAllowLocationChange(allowed);
    }

    @Override
    public void deactivateTextSelections() {
        textSelectionsDisabled = true;
    }

    @Override
    public IBrowserFunction createBrowserFunction(final JavascriptFunction function) {
        checkNotNull(function);
        return internalBrowser.createBrowserFunction(function);
    }

    @Override
    public void addJavaScriptExceptionListener(
            JavaScriptExceptionListener exceptionListener) {
        checkNotNull(exceptionListener);
        internalBrowser.addJavaScriptExceptionListener(exceptionListener);
    }

    @Override
    public void removeJavaScriptExceptionListener(
            JavaScriptExceptionListener exceptionListener) {
        checkNotNull(exceptionListener);
        internalBrowser.removeJavaScriptExceptionListener(exceptionListener);
    }

    public <V, T> Future<T> runWithCallback(final Future<V> future, final CallbackFunction<V, T> callback) {
        checkNotNull(future);
        checkNotNull(callback);
        return internalBrowser.runWithCallback(future, callback);
    }

    public void runOnDisposal(Runnable runnable) {
        checkNotNull(runnable);
        internalBrowser.runOnDisposal(runnable);
    }

    @Override
    public void setSize(int width, int height) {
        internalBrowser.setSize(width, height);
    }

    @Override
    public void setText(String html) {
        checkNotNull(html);
        internalBrowser.setText(html);
    }

    @Override
    public boolean setFocus() {
        return internalBrowser.setFocus();
    }

    public void setCachedContentBounds(Rectangle rectangle) {
        internalBrowser.setCachedContentBounds(rectangle);
    }

    public Rectangle getCachedContentBounds() {
        return internalBrowser.getCachedContentBounds();
    }

    private void checkNotUIThread() {
        internalBrowser.checkNotUIThread();
	}
}
