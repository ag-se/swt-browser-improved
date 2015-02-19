package de.fu_berlin.inf.ag_se.widgets.browser;

import org.eclipse.swt.widgets.Composite;

public abstract class AbstractSwtBrowser<T extends IBrowser> extends Composite {

    protected T browser;

    protected SwtInternalBrowserWrapper internalBrowser;

    protected AbstractSwtBrowser(Composite parent, int style) {
        super(parent, style);
    }

    protected void setBrowser(T browser) {
        this.browser = browser;
    }

    protected void setInternalBrowser(SwtInternalBrowserWrapper internalBrowser) {
        this.internalBrowser = internalBrowser;
    }
}
