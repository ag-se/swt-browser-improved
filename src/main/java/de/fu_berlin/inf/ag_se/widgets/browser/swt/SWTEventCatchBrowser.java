package de.fu_berlin.inf.ag_se.widgets.browser.swt;

import de.fu_berlin.inf.ag_se.widgets.browser.IBrowserFunction;
import de.fu_berlin.inf.ag_se.widgets.browser.extensions.BrowserExtension;
import de.fu_berlin.inf.ag_se.widgets.browser.extensions.EventCatchBrowser;
import de.fu_berlin.inf.ag_se.widgets.browser.extensions.IEventCatchBrowser;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IAnchorListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IDNDListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IFocusListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IMouseListener;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import java.awt.*;
import java.util.Collections;

public class SWTEventCatchBrowser<T extends EventCatchBrowser> extends SWTExtendedBrowser<T> implements IEventCatchBrowser {

    private static Logger LOGGER = Logger.getLogger(SWTEventCatchBrowser.class);

    /**
     * Constructs a new browser composite with the given styles.
     *
     * @param parent a widget which will be the parent of the new instance (cannot be null)
     * @param style  if {@link SWT#INHERIT_FORCE} is set the loaded page's
     */
    protected SWTEventCatchBrowser(Composite parent, int style) {
        super(parent, style);
    }

    public static SWTEventCatchBrowser createSWTBrowser(Composite parent, int style) {
        return createSWTBrowser(parent, style, Collections.<BrowserExtension>emptyList());
    }

    public static SWTEventCatchBrowser createSWTBrowser(Composite parent, int style, Iterable<BrowserExtension> extensions) {
        final SWTEventCatchBrowser swtEventCatchBrowser = new SWTEventCatchBrowser(parent, style);
        SwtInternalBrowserWrapper internalSWTBrowserWrapper = new SwtInternalBrowserWrapper(swtEventCatchBrowser);
        swtEventCatchBrowser.setInternalBrowser(internalSWTBrowserWrapper);
        swtEventCatchBrowser.setBrowser(new EventCatchBrowser(internalSWTBrowserWrapper, extensions));
        swtEventCatchBrowser.createBrowserFunction(new IBrowserFunction("__resize") {
            @Override
            public Object function(Object[] arguments) {
                if (arguments.length == 4 && (arguments[0] == null
                        || arguments[0] instanceof Double) && (arguments[1] == null
                        || arguments[1] instanceof Double) && (arguments[2] == null
                        || arguments[2] instanceof Double) && (arguments[3] == null
                        || arguments[3] instanceof Double)) {

                    swtEventCatchBrowser.setCachedContentBounds(new Rectangle(
                            arguments[0] != null ?
                            (int) Math.round((Double) arguments[0]) :
                            0, arguments[1] != null ?
                               (int) Math.round((Double) arguments[1]) :
                               0, arguments[2] != null ?
                                  (int) Math.round((Double) arguments[2]) :
                                  Integer.MAX_VALUE, arguments[3] != null ?
                                                     (int) Math.round((Double) arguments[3]) :
                                                     Integer.MAX_VALUE));
                    LOGGER.debug("browser content resized to " + swtEventCatchBrowser.getCachedContentBounds());
                    swtEventCatchBrowser.layoutRoot();
                }
                return null;
            }
        });
        return swtEventCatchBrowser;
    }

    @Override
    public void addAnchorListener(IAnchorListener anchorListener) {
        browser.addAnchorListener(anchorListener);
    }

    @Override
    public void removeAnchorListener(IAnchorListener anchorListener) {
        browser.removeAnchorListener(anchorListener);
    }

    @Override
    public void addMouseListener(IMouseListener mouseListener) {
        browser.addMouseListener(mouseListener);
    }

    @Override
    public void removeMouseListener(IMouseListener mouseListener) {
        browser.removeMouseListener(mouseListener);
    }

    @Override
    public void addFocusListener(IFocusListener focusListener) {
        browser.addFocusListener(focusListener);
    }

    @Override
    public void removeFocusListener(IFocusListener focusListener) {
        browser.removeFocusListener(focusListener);
    }

    @Override
    public void addDNDListener(IDNDListener dNDListener) {
        browser.addDNDListener(dNDListener);
    }

    @Override
    public void removeDNDListener(IDNDListener dNDListener) {
        browser.removeDNDListener(dNDListener);
    }
}
