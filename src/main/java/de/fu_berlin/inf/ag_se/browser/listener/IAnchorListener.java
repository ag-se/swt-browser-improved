package de.fu_berlin.inf.ag_se.browser.listener;

import de.fu_berlin.inf.ag_se.browser.html.IAnchor;
import de.fu_berlin.inf.ag_se.browser.html.IElement;

public interface IAnchorListener {

	/**
	 * This method is called if the user's mouse entered or left a
	 * {@link IElement}.
	 * 
	 * @param anchor instance of IAnchor
	 * @param entered
	 *            is true if the mouse entered the {@link IElement} and false if
	 *            the mouse left it.
	 */
	public void anchorHovered(IAnchor anchor, boolean entered);
}
