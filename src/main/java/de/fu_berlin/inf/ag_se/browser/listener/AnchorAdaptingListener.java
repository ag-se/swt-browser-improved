package de.fu_berlin.inf.ag_se.browser.listener;

import de.fu_berlin.inf.ag_se.browser.html.IAnchor;
import de.fu_berlin.inf.ag_se.browser.utils.Assert;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Instances of this class adapt {@link IURIListener}s so they can be used as
 * {@link IAnchorListener}s.
 * 
 * @author bkahlert
 * 
 */
public class AnchorAdaptingListener implements IAnchorListener {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger
			.getLogger(AnchorAdaptingListener.class);

	private final IURIListener uriListener;

	public AnchorAdaptingListener(IURIListener uriListener) {
		Assert.isNotNull(uriListener);
		this.uriListener = uriListener;
	}

	@Override
	public void anchorHovered(IAnchor anchor, boolean entered) {
		try {
			this.uriListener.uriHovered(new URI(anchor.getHref()), entered);
		} catch (URISyntaxException e) {
			// LOGGER.info("Error converting " + anchor.getHref() + " to a "
			// + URI.class.getSimpleName(), e);
		}
	}

}
