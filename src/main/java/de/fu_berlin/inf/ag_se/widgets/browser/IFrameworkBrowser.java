package de.fu_berlin.inf.ag_se.widgets.browser;

public interface IFrameworkBrowser {
    void addProgressListener(Runnable runnable);

    String getUrl();

    Object evaluate(String script);

    void setVisible(boolean visible);

    boolean isDisposed();

    IBrowserFunction createBrowserFunction(IBrowserFunction function);

    void setUrl(String uri);
}
