package de.fu_berlin.inf.ag_se.browser.swt;

import de.fu_berlin.inf.ag_se.browser.IBrowserFunction;
import de.fu_berlin.inf.ag_se.browser.IFrameworkBrowser;
import de.fu_berlin.inf.ag_se.browser.threading.UIThreadExecutor;
import de.fu_berlin.inf.ag_se.browser.utils.SWTUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

public class SwtFrameworkBrowser implements IFrameworkBrowser {

    private final Browser browser;
    private final SwtUiThreadExecutor uiThreadExecutor = new SwtUiThreadExecutor();

    public SwtFrameworkBrowser(Composite parent) {
        browser = new Browser(parent, SWT.NONE);
    }

    @Override
    public void setVisible(boolean visible) {
        browser.setVisible(visible);
    }

    /**
     * May be called from whatever thread.
     */
    @Override
    public IBrowserFunction createBrowserFunction(final IBrowserFunction function) {
        new BrowserFunction(browser, function.getName()) {
            @Override
            public Object function(Object[] arguments) {
                return function.function(arguments);
            }
        };
        return function;
    }

    @Override
    public String getUrl() {
        return browser.getUrl();
    }

    @Override
    public void setUrl(String url) {
        browser.setUrl(url);
    }

    @Override
    public UIThreadExecutor getUIThreadExecutor() {
        return uiThreadExecutor;
    }

    @Override
    public void setSize(int width, int height) {
        browser.setSize(width, height);
    }

    @Override
    public void setText(String html) {
        browser.setText(html);
    }

    @Override
    public boolean setFocus() {
        return browser.setFocus();
    }

    @Override
    public Object evaluate(String javaScript) {
        return browser.evaluate(javaScript);
    }

    @Override
    public void addProgressListener(final Runnable runnable) {
        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
                runnable.run();
            }
        });
    }

    @Override
    public boolean isDisposed() {
        return browser.isDisposed();
    }

    public void addLocationListener(LocationListener listener) {
        browser.addLocationListener(listener);
    }

    public void addDisposeListener(DisposeListener disposeListener) {
        browser.addDisposeListener(disposeListener);
    }

    public void addListener(int eventType, Listener listener) {
        browser.addListener(eventType, listener);
    }

    protected void layoutRoot() {
        Composite root = SWTUtils.getRoot(browser);
        root.layout(true, true);
    }
}
