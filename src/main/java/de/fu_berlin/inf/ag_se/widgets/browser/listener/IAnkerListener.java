package de.fu_berlin.inf.ag_se.widgets.browser.listener;

import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IAnker;

public interface IAnkerListener {

	/**
	 * This method is called if the user's mouse entered or left a
	 * {@link de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement}.
	 * 
	 * @param anker instance of IAnker
	 * @param entered
	 *            is true if the mouse entered the {@link de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement} and false if
	 *            the mouse left it.
	 */
	public void ankerHovered(IAnker anker, boolean entered);
}
