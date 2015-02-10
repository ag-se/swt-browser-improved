package de.fu_berlin.inf.ag_se.widgets.browser.listener;

import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IAnchor;

public interface IAnchorListener {

	/**
	 * This method is called if the user's mouse entered or left a
	 * {@link de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement}.
	 * 
	 * @param anchor instance of IAnchor
	 * @param entered
	 *            is true if the mouse entered the {@link de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement} and false if
	 *            the mouse left it.
	 */
	public void anchorHovered(IAnchor anchor, boolean entered);
}
