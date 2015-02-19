package de.fu_berlin.inf.ag_se.browser.listener;

import de.fu_berlin.inf.ag_se.browser.html.IElement;

public interface IDNDListener {
	public void dragStart(long offsetX, long offsetY, IElement element,
                          String mimeType, String data);

	public void drop(long offsetX, long offsetY, IElement element,
                     String mimeType, String data);
}
