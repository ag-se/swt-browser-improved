package de.fu_berlin.inf.ag_se.browser.listener;

import de.fu_berlin.inf.ag_se.browser.html.IElement;

public interface IFocusListener {
	public void focusGained(IElement element);

	public void focusLost(IElement element);
}