package de.fu_berlin.inf.ag_se.browser.swt;

import de.fu_berlin.inf.ag_se.browser.InternalBrowserWrapper;
import org.apache.log4j.Logger;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

public class SWTInternalBrowserWrapper extends InternalBrowserWrapper<SWTFrameworkBrowser> {

    private static Logger LOGGER = Logger.getLogger(InternalBrowserWrapper.class);

    public SWTInternalBrowserWrapper(Composite parent) {
        super(new SWTFrameworkBrowser(parent));

        browser.addLocationListener(new LocationAdapter() {
            @Override
            public void changing(LocationEvent event) {
                if (!settingUri) {
                    event.doit = allowLocationChange || isLoading();
                }
            }
        });

        browser.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                fireIsDisposed();
            }
        });
    }


    void addListener(int eventType, Listener listener) {
        // TODO evtl. erst ausführen, wenn alles wirklich geladen wurde, um
        // evtl. falsche Mauskoordinaten zu verhindern und so ein Fehlverhalten
        // im InformationControl vorzeugen
        browser.addListener(eventType, listener);
    }

    protected void layoutRoot() {
        LOGGER.debug("layout all");
        browser.layoutRoot();
    }
}

