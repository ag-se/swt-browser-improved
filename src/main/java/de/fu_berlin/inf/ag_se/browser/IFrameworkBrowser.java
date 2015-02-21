package de.fu_berlin.inf.ag_se.browser;

import de.fu_berlin.inf.ag_se.browser.threading.UIThreadExecutor;

public interface IFrameworkBrowser {
    void addProgressListener(Runnable runnable);

    String getUrl();

    Object evaluate(String script);

    void setVisible(boolean visible);

    boolean isDisposed();

    IBrowserFunction createBrowserFunction(IBrowserFunction function);

    void setUrl(String uri);

    UIThreadExecutor getUIThreadExecutor();

    void setSize(int width, int height);
}
