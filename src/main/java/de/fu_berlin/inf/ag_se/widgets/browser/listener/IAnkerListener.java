package de.fu_berlin.inf.ag_se.widgets.browser.listener;

import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IAnker;

public interface IAnkerListener {
/**
	 * This method is called if an {@link de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement} was clicked.
	 * <p>
	 * <strong>Warning:</strong> Only {@link de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement#getHref()} returns a value.
	 * The other methods return null; FIXME: Fill other values
	 * <p><strong>REPLACED BY {@link IMouseListener#clicked(double, double, de.fu_berlin.inf.ag_se.widgets.browser.extended.html.Element)</strong>
	 * 
	 * @param anker
	 * 
	 */
	@Deprecated
	public void ankerClicked(IAnker anker);

	/**
	 * This method is called if the user's mouse entered or left a
	 * {@link de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement}.
	 * 
	 * @param anker
	 * @param entered
	 *            is true if the mouse entered the {@link de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement} and false if
	 *            the mouse left it.
	 */
	public void ankerHovered(IAnker anker, boolean entered);
}
