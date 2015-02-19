package de.fu_berlin.inf.ag_se.browser.listener;

import de.fu_berlin.inf.ag_se.browser.html.IElement;

public interface IMouseListener {
	public void mouseMove(double x, double y);

	public void mouseDown(double x, double y, IElement element);

	public void mouseUp(double x, double y, IElement element);

	public void clicked(double x, double y, IElement element);
}
