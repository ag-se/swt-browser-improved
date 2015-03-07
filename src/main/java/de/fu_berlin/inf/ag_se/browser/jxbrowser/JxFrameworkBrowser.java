package de.fu_berlin.inf.ag_se.browser.jxbrowser;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.BrowserFunction;
import com.teamdev.jxbrowser.chromium.JSValue;
import com.teamdev.jxbrowser.chromium.events.FinishLoadingEvent;
import com.teamdev.jxbrowser.chromium.events.LoadAdapter;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import de.fu_berlin.inf.ag_se.browser.IBrowserFunction;
import de.fu_berlin.inf.ag_se.browser.IFrameworkBrowser;
import de.fu_berlin.inf.ag_se.browser.threading.UIThreadExecutor;

import java.awt.*;

public class JxFrameworkBrowser implements IFrameworkBrowser {

    private final Browser browser;
    private final BrowserView browserView;

    private final UIThreadExecutor uiThreadExecutor = new JxUIThreadExecutor();

    public JxFrameworkBrowser(Container container) {
        this.browser = new Browser();
        browserView = new BrowserView(browser);
        container.add(browserView);
    }

    @Override
    public void addProgressListener(final Runnable runnable) {
        browser.addLoadListener(new LoadAdapter() {
            @Override
            public void onFinishLoadingFrame(FinishLoadingEvent finishLoadingEvent) {
                runnable.run();
            }
        });
    }

    @Override
    public String getUrl() {
        return browser.getURL();
    }

    @Override
    public Object evaluate(String script) {
        return convert(browser.executeJavaScriptAndReturnValue(script));
    }

    @Override
    public void setVisible(boolean visible) {
        browserView.setVisible(true);
    }

    @Override
    public boolean isDisposed() {
        return browser.isDisposed();
    }

    @Override
    public IBrowserFunction createBrowserFunction(final IBrowserFunction function) {
        browser.registerFunction(function.getName(), new BrowserFunction() {
            @Override
            public JSValue invoke(JSValue... jsValues) {
                Object[] arguments = new Object[jsValues.length];
                for (int i = 0; i < arguments.length; i++) {
                    JSValue jsValue = jsValues[i];
                    arguments[i] = convert(jsValue);
                }

                function.function(arguments);
                return JSValue.createNull();
            }
        });
        return function;
    }

    private static Object convert(JSValue jsValue) {
        if (jsValue.isString())
            return jsValue.getString();
        else if (jsValue.isBoolean())
            return jsValue.getBoolean();
        else if (jsValue.isNumber())
            return jsValue.getNumber();
        else
            return null;
    }

    @Override
    public void setUrl(String uri) {
        browser.loadURL(uri);
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
        //TODO
    }

    @Override
    public boolean setFocus() {
        //TODO
        return false;
    }
}
