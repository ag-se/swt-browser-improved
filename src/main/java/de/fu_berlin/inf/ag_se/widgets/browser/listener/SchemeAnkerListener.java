package de.fu_berlin.inf.ag_se.widgets.browser.listener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.log4j.Logger;

import de.fu_berlin.inf.ag_se.utils.ExecUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IAnker;

/**
 * Instances of this class handle {@link de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement} based on their schema (e.g.
 * http, file).
 * <p>
 * Furthermore independently of which thread calls the listener the provided
 * listeners are called from a non-UI thread. If you need to make changes to the
 * GUI you will have to use {@link ExecUtils}.
 * 
 * @author bkahlert
 * 
 */
public class SchemeAnkerListener implements IAnkerListener {

	private static final Logger LOGGER = Logger
			.getLogger(SchemeAnkerListener.class);

	private Map<String, IAnkerListener> listeners;
	private IAnkerListener defaultListener;

	public SchemeAnkerListener(Map<String, IAnkerListener> listeners) {
		this.listeners = listeners;
		this.defaultListener = new IAnkerListener() {
			@Override
			public void ankerHovered(IAnker anker, boolean entered) {
				return;
			}
		};
	}

	public SchemeAnkerListener(Map<String, IAnkerListener> listeners,
			IAnkerListener defaultListener) {
		this.listeners = listeners;
		this.defaultListener = defaultListener;
	}

	@Override
	public void ankerHovered(final IAnker anker, final boolean entered) {
		ExecUtils.nonUIAsyncExec(SchemeAnkerListener.class,
				"Anker Hovered Notification", new Runnable() {
					@Override
					public void run() {
						try {
							final URI uri = new URI(anker.getHref());
							if (uri.getScheme() == null) {
								if (SchemeAnkerListener.this.listeners
										.containsKey(null)) {
									SchemeAnkerListener.this.listeners
											.get(null).ankerHovered(anker,
													entered);
								} else {
									return;
								}
							} else {
								boolean handled = false;
								for (String schema : SchemeAnkerListener.this.listeners
										.keySet()) {
									if (uri.getScheme()
											.equalsIgnoreCase(schema)) {
										SchemeAnkerListener.this.listeners.get(
												schema).ankerHovered(anker,
												entered);
										handled = true;
										break;
									}
								}
								if (!handled) {
									SchemeAnkerListener.this.defaultListener
											.ankerHovered(anker, entered);
								}
							}
						} catch (URISyntaxException e) {
							LOGGER.info("Invalid URI in "
									+ SchemeAnkerListener.class.getSimpleName()
									+ ": " + anker);
						}
					}
				});
	}

}
