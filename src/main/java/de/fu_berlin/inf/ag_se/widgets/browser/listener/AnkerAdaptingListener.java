package de.fu_berlin.inf.ag_se.widgets.browser.listener;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Assert;

import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IAnker;

/**
 * Instances of this class adapt {@link de.fu_berlin.inf.ag_se.widgets.browser.listener.IURIListener}s so they can be used as
 * {@link de.fu_berlin.inf.ag_se.widgets.browser.listener.IAnkerListener}s.
 * 
 * @author bkahlert
 * 
 */
public class AnkerAdaptingListener implements IAnkerListener {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger
			.getLogger(AnkerAdaptingListener.class);

	private final IURIListener uriListener;

	public AnkerAdaptingListener(IURIListener uriListener) {
		Assert.isNotNull(uriListener);
		this.uriListener = uriListener;
	}

	@Override
	public void ankerClicked(IAnker anker) {
		try {
			this.uriListener.uriClicked(new URI(anker.getHref()));
		} catch (URISyntaxException e) {
			// LOGGER.info("Error converting " + anker.getHref() + " to a "
			// + URI.class.getSimpleName(), e);
		}
	}

	@Override
	public void ankerHovered(IAnker anker, boolean entered) {
		try {
			this.uriListener.uriHovered(new URI(anker.getHref()), entered);
		} catch (URISyntaxException e) {
			// LOGGER.info("Error converting " + anker.getHref() + " to a "
			// + URI.class.getSimpleName(), e);
		}
	}

}
