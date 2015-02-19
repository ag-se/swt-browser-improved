package de.fu_berlin.inf.ag_se.browser.listener;

import java.net.URI;

public interface IURIListener {

	/**
	 * This method is called if the user's mouse entered of left a {@link java.net.URI}.
	 * 
	 * @param uri
	 * @param entered
	 *            is true if the mouse entered the {@link java.net.URI} and false if the
	 *            mouse left it.
	 */
	public void uriHovered(URI uri, boolean entered);
}
