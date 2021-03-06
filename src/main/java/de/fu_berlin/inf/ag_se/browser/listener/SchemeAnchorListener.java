package de.fu_berlin.inf.ag_se.browser.listener;

import de.fu_berlin.inf.ag_se.browser.html.IAnchor;
import de.fu_berlin.inf.ag_se.browser.html.IElement;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Instances of this class handle {@link IElement} based on their schema (e.g.
 * http, file).
 * <p>
 * Furthermore independently of which thread calls the listener the provided
 * listeners are called from a non-UI thread.
 * 
 * @author bkahlert
 * 
 */
public class SchemeAnchorListener implements IAnchorListener {

	private static final Logger LOGGER = Logger
			.getLogger(SchemeAnchorListener.class);

	private Map<String, IAnchorListener> listeners;
	private IAnchorListener defaultListener;

	public SchemeAnchorListener(Map<String, IAnchorListener> listeners) {
		this.listeners = listeners;
		this.defaultListener = new IAnchorListener() {
			@Override
			public void anchorHovered(IAnchor anchor, boolean entered) {
				return;
			}
		};
	}

	public SchemeAnchorListener(Map<String, IAnchorListener> listeners,
                                IAnchorListener defaultListener) {
		this.listeners = listeners;
		this.defaultListener = defaultListener;
	}

	@Override
	public void anchorHovered(final IAnchor anchor, final boolean entered) {
		new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final URI uri = new URI(anchor.getHref());
                            if (uri.getScheme() == null) {
                                if (SchemeAnchorListener.this.listeners
                                        .containsKey(null)) {
                                    SchemeAnchorListener.this.listeners
                                            .get(null).anchorHovered(anchor,
                                            entered);
                                } else {
                                    return;
                                }
                            } else {
                                boolean handled = false;
                                for (String schema : SchemeAnchorListener.this.listeners
                                        .keySet()) {
                                    if (uri.getScheme()
                                           .equalsIgnoreCase(schema)) {
                                        SchemeAnchorListener.this.listeners.get(
                                                schema).anchorHovered(anchor,
                                                entered);
                                        handled = true;
                                        break;
                                    }
                                }
                                if (!handled) {
                                    SchemeAnchorListener.this.defaultListener
                                            .anchorHovered(anchor, entered);
                                }
                            }
                        } catch (URISyntaxException e) {
                            LOGGER.info("Invalid URI in "
                                    + SchemeAnchorListener.class.getSimpleName()
                                    + ": " + anchor);
                        }
                    }
                }).start();
	}

}
