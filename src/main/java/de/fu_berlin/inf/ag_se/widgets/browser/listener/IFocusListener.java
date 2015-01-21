package de.fu_berlin.inf.ag_se.widgets.browser.listener;

import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement;

public interface IFocusListener {
	public void focusGained(IElement element);

	public void focusLost(IElement element);
}