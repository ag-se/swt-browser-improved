package de.fu_berlin.inf.ag_se.widgets.browser.listener;

import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement;

public interface IMouseListener {
	public void mouseMove(double x, double y);

	public void mouseDown(double x, double y, IElement element);

	public void mouseUp(double x, double y, IElement element);

	public void clicked(double x, double y, IElement element);
}
