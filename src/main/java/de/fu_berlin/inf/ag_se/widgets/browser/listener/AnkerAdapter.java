package de.fu_berlin.inf.ag_se.widgets.browser.listener;

import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IAnker;

public class AnkerAdapter implements IAnkerListener {

	@Override
	@Deprecated
	public void ankerClicked(IAnker anker) {
		return;
	}

	@Override
	public void ankerHovered(IAnker anker, boolean entered) {
		return;
	}

}