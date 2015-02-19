package de.fu_berlin.inf.ag_se.browser;

import com.sun.istack.internal.Nullable;
import de.fu_berlin.inf.ag_se.browser.functions.CallbackFunction;
import de.fu_berlin.inf.ag_se.browser.functions.Function;
import de.fu_berlin.inf.ag_se.browser.javafx.JavaFxFrameworkBrowser;
import de.fu_berlin.inf.ag_se.browser.listener.JavaScriptExceptionListener;
import de.fu_berlin.inf.ag_se.browser.utils.IConverter;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;

public class JavaFxBrowser implements IBrowser {

    private Browser browser;

    public JavaFxBrowser(Stage stage) {
        Group root = new Group();
        Scene scene = new Scene(root, javafx.scene.paint.Color.ALICEBLUE);
        this.browser = new Browser(new InternalBrowserWrapper(new JavaFxFrameworkBrowser(root)));
        stage.setScene(scene);
    }

    @Override
    public Future<Boolean> open(String uri, int timeout) {
        return browser.open(uri, timeout);
    }

    @Override
    public Future<Boolean> open(String uri, int timeout, @Nullable String pageLoadCheckScript) {
        return browser.open(uri, timeout, pageLoadCheckScript);
    }

    @Override
    public Future<Boolean> open(URI uri, int timeout) {
        return browser.open(uri, timeout);
    }

    @Override
    public boolean syncOpen(URI uri, int timeout) {
        return browser.syncOpen(uri, timeout);
    }

    @Override
    public Future<Boolean> open(URI uri, int timeout, CallbackFunction<Boolean, Boolean> callback) {
        return browser.open(uri, timeout, callback);
    }

    @Override
    public Future<Boolean> open(URI uri, int timeout, @Nullable String pageLoadCheckScript) {
        return browser.open(uri, timeout, pageLoadCheckScript);
    }

    @Override
    public boolean syncOpen(URI uri, int timeout, @Nullable String pageLoadCheckScript) {
        return browser.syncOpen(uri, timeout, pageLoadCheckScript);
    }

    @Override
    public Future<Boolean> openWithCallback(URI uri, int timeout, @Nullable String pageLoadCheckScript,
                                            CallbackFunction<Boolean, Boolean> callback) {
        return browser.openWithCallback(uri, timeout, pageLoadCheckScript, callback);
    }

    @Override
    public Future<Boolean> openBlank() {
        return browser.openBlank();
    }

    @Override
    public boolean isLoadingCompleted() {
        return browser.isLoadingCompleted();
    }

    @Override
    public String getUrl() {
        return browser.getUrl();
    }

    @Override
    public void waitForCondition(String javaScriptExpression) {
        browser.waitForCondition(javaScriptExpression);
    }

    @Override
    public Future<Void> checkCondition(String javaScriptExpression) {
        return browser.checkCondition(javaScriptExpression);
    }

    @Override
    public <DEST> Future<DEST> executeWhenConditionIsMet(String javaScriptExpression, CallbackFunction<Void, DEST> callback) {
        return browser.executeWhenConditionIsMet(javaScriptExpression, callback);
    }

    @Override
    public void executeAfterCompletion(Runnable runnable) {
        browser.executeAfterCompletion(runnable);
    }

    @Override
    public Future<Boolean> injectJavascript(File javascriptFile) {
        return browser.injectJavascript(javascriptFile);
    }

    @Override
    public Future<Boolean> injectJavascript(URI scriptURI) {
        return browser.injectJavascript(scriptURI);
    }

    @Override
    public boolean syncInjectJavascript(URI scriptURI) {
        return browser.syncInjectJavascript(scriptURI);
    }

    @Override
    public Future<Boolean> injectCssURI(URI cssURI) {
        return browser.injectCssURI(cssURI);
    }

    @Override
    public boolean syncInjectCssURI(URI cssURI) {
        return browser.syncInjectCssURI(cssURI);
    }

    @Override
    public Future<Boolean> injectCss(String css) {
        return browser.injectCss(css);
    }

    @Override
    public boolean syncInjectCss(String css) {
        return browser.syncInjectCss(css);
    }

    @Override
    public <DEST> Future<DEST> injectCss(String css, CallbackFunction<Boolean, DEST> callback) {
        return browser.injectCss(css, callback);
    }

    @Override
    public Future<Boolean> run(File scriptFile) {
        return browser.run(scriptFile);
    }

    @Override
    public Future<Boolean> runContentAsScriptTag(File scriptFile) throws IOException {
        return browser.runContentAsScriptTag(scriptFile);
    }

    @Override
    public Future<Boolean> run(URI scriptURI) {
        return browser.run(scriptURI);
    }

    @Override
    public Future<Object> run(String script) {
        return browser.run(script);
    }

    @Override
    public Object syncRun(String script) {
        return browser.syncRun(script);
    }

    @Override
    public <DEST> Future<DEST> run(String script, CallbackFunction<Object, DEST> callback) {
        return browser.run(script, callback);
    }

    @Override
    public <DEST> Future<DEST> run(String script, IConverter<Object, DEST> converter) {
        return browser.run(script, converter);
    }

    @Override
    public <DEST> DEST syncRun(String script, IConverter<Object, DEST> converter) {
        return browser.syncRun(script, converter);
    }

    @Override
    public <T, DEST> Future<T> run(String script, IConverter<Object, DEST> converter, CallbackFunction<DEST, T> callback) {
        return browser.run(script, converter, callback);
    }

    @Override
    public void executeBeforeScript(Function<String> function) {
        browser.executeBeforeScript(function);
    }

    @Override
    public void executeAfterScript(Function<Object> function) {
        browser.executeAfterScript(function);
    }

    @Override
    public Future<Boolean> containsElementWithID(String id) {
        return browser.containsElementWithID(id);
    }

    @Override
    public Future<Boolean> containsElementsWithName(String name) {
        return browser.containsElementsWithName(name);
    }

    @Override
    public Future<Boolean> setBodyHtml(String html) {
        return browser.setBodyHtml(html);
    }

    @Override
    public Future<String> getBodyHtml() {
        return browser.getBodyHtml();
    }

    @Override
    public Future<String> getHtml() {
        return browser.getHtml();
    }

    @Override
    public <T> Future<T> getHtml(CallbackFunction<String, T> callback) {
        return browser.getHtml(callback);
    }

    @Override
    public Future<Boolean> pasteHtmlAtCaret(String html) {
        return browser.pasteHtmlAtCaret(html);
    }

    @Override
    public void setAllowLocationChange(boolean allowed) {
        browser.setAllowLocationChange(allowed);
    }

    @Override
    public void deactivateTextSelections() {
        browser.deactivateTextSelections();
    }

    @Override
    public IBrowserFunction createBrowserFunction(IBrowserFunction function) {
        return browser.createBrowserFunction(function);
    }

    @Override
    public void addJavaScriptExceptionListener(JavaScriptExceptionListener exceptionListener) {
        browser.addJavaScriptExceptionListener(exceptionListener);
    }

    @Override
    public void removeJavaScriptExceptionListener(JavaScriptExceptionListener exceptionListener) {
        browser.removeJavaScriptExceptionListener(exceptionListener);
    }
}
