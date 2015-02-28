package de.fu_berlin.inf.ag_se.browser.swt;

import de.fu_berlin.inf.ag_se.browser.IBrowser;
import org.eclipse.swt.widgets.Composite;

public abstract class AbstractSWTBrowser<T extends IBrowser> extends Composite {

    protected T browser;

    protected SWTInternalBrowserWrapper internalBrowser;

    protected AbstractSWTBrowser(Composite parent, int style) {
        super(parent, style);
    }

    protected void setBrowser(T browser) {
        this.browser = browser;
    }

    protected void setInternalBrowser(SWTInternalBrowserWrapper internalBrowser) {
        this.internalBrowser = internalBrowser;
    }
}
