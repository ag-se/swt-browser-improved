package de.fu_berlin.inf.ag_se.browser.swt;

import de.fu_berlin.inf.ag_se.browser.extensions.BootstrapBrowser;
import de.fu_berlin.inf.ag_se.browser.extensions.BrowserExtension;
import de.fu_berlin.inf.ag_se.browser.extensions.IBootstrapBrowser;
import de.fu_berlin.inf.ag_se.browser.utils.colors.RGB;
import org.apache.log4j.Logger;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

import java.util.Collections;

public class SWTBootstrapBrowser<T extends BootstrapBrowser> extends SWTJQueryBrowser<T> implements IBootstrapBrowser {
    private static final Logger LOGGER = Logger.getLogger(SWTBootstrapBrowser.class);

    protected SWTBootstrapBrowser(Composite parent, int style) {
        super(parent, style);
    }

    public static SWTBootstrapBrowser createSWTBrowser(Composite parent, int style) {
        return createSWTBrowser(parent, style, Collections.<BrowserExtension>emptyList());
    }

    public static SWTBootstrapBrowser createSWTBrowser(Composite parent, int style, Iterable<BrowserExtension> extensions) {
        SWTBootstrapBrowser<BootstrapBrowser> swtBootstrapBrowser = new SWTBootstrapBrowser<BootstrapBrowser>(parent,
                style);
        SWTInternalBrowserWrapper internalSWTBrowserWrapper = new SWTInternalBrowserWrapper(swtBootstrapBrowser);
        swtBootstrapBrowser.setInternalBrowser(internalSWTBrowserWrapper);
        swtBootstrapBrowser.setBrowser(new BootstrapBrowser(internalSWTBrowserWrapper, extensions));
        return swtBootstrapBrowser;
    }

    @Override
    public void setBackground(Color color) {
        //TODO check the result or omit the call
        super.setBackground(color);
        String hex = color != null ? new RGB(color.getRGB()).toDecString()
                                   : "transparent";
        try {
            injectCss("body { background-color: " + hex + " !important; }");
        } catch (RuntimeException e) {
            LOGGER.error("Error setting background color to " + color, e);
        }
    }
}
