package de.fu_berlin.inf.ag_se.widgets.browser.extended;

import de.fu_berlin.inf.ag_se.utils.colors.RGB;
import de.fu_berlin.inf.ag_se.widgets.browser.SwtInternalBrowserWrapper;
import org.apache.log4j.Logger;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

import java.util.Collections;

public class SwtBootstrapBrowser<T extends BootstrapBrowser> extends SwtJQueryBrowser<T> implements IBootstrapBrowser {
    private static final Logger LOGGER = Logger.getLogger(SwtBootstrapBrowser.class);

    protected SwtBootstrapBrowser(Composite parent, int style) {
        super(parent, style);
    }

    public static SwtBootstrapBrowser createSWTBrowser(Composite parent, int style) {
        return createSWTBrowser(parent, style, Collections.<BrowserExtension>emptyList());
    }

    public static SwtBootstrapBrowser createSWTBrowser(Composite parent, int style, Iterable<BrowserExtension> extensions) {
        SwtBootstrapBrowser<BootstrapBrowser> swtBootstrapBrowser = new SwtBootstrapBrowser<BootstrapBrowser>(parent,
                style);
        SwtInternalBrowserWrapper internalSWTBrowserWrapper = new SwtInternalBrowserWrapper(swtBootstrapBrowser);
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
