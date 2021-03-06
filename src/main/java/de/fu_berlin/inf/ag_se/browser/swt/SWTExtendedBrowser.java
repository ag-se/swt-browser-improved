package de.fu_berlin.inf.ag_se.browser.swt;

import de.fu_berlin.inf.ag_se.browser.extensions.BrowserExtension;
import de.fu_berlin.inf.ag_se.browser.extensions.ExtendedBrowser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import java.util.Collections;

public class SWTExtendedBrowser<T extends ExtendedBrowser> extends SWTBrowser<T> {

    /**
     * Constructs a new browser composite with the given styles.
     *
     * @param parent a widget which will be the parent of the new instance (cannot be null)
     * @param style  if {@link SWT#INHERIT_FORCE} is set the loaded page's
     */
    protected SWTExtendedBrowser(Composite parent, int style) {
        super(parent, style);
    }

    public static SWTExtendedBrowser createSWTBrowser(Composite parent, int style) {
           return createSWTBrowser(parent, style, Collections.<BrowserExtension>emptyList());
    }

    public static SWTExtendedBrowser createSWTBrowser(Composite parent, int style, Iterable<BrowserExtension> extensions) {
        SWTExtendedBrowser swtExtendedBrowser = new SWTExtendedBrowser(parent, style);
        SWTInternalBrowserWrapper internalSWTBrowserWrapper = new SWTInternalBrowserWrapper(swtExtendedBrowser);
        swtExtendedBrowser.setInternalBrowser(internalSWTBrowserWrapper);
        swtExtendedBrowser.setBrowser(new ExtendedBrowser(internalSWTBrowserWrapper, extensions));
        return swtExtendedBrowser;
    }
}
