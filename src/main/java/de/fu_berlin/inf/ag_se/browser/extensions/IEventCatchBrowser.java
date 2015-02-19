package de.fu_berlin.inf.ag_se.browser.extensions;

import de.fu_berlin.inf.ag_se.browser.IBrowser;
import de.fu_berlin.inf.ag_se.browser.listener.IAnchorListener;
import de.fu_berlin.inf.ag_se.browser.listener.IDNDListener;
import de.fu_berlin.inf.ag_se.browser.listener.IFocusListener;
import de.fu_berlin.inf.ag_se.browser.listener.IMouseListener;

@SuppressWarnings("UnusedDeclaration")
public interface IEventCatchBrowser extends IBrowser {

    /**
     * Adds an {@link IAnchorListener}
     * to the browser.
     * This can be used to react to the hovering of anchor tags
     *
     * May be called from whatever thread.
     *
     * @param anchorListener the listener to be added
     * @throws NullPointerException if anchorListener is null
     */
    void addAnchorListener(IAnchorListener anchorListener);

    /**
     * Removes the given {@link IAnchorListener}.
     *
     * May be called from whatever thread.
     *
     * @param anchorListener the listener to be removed
     * @throws NullPointerException if anchorListener is null
     */
    void removeAnchorListener(IAnchorListener anchorListener);

    /**
     * Adds an {@link IMouseListener}
     * to the browser.
     * This can be used to react to mouse events inside the browser
     *
     * May be called from whatever thread.
     *
     * @param mouseListener the listener to be added
     * @throws NullPointerException if mouseListener is null
     */
    void addMouseListener(IMouseListener mouseListener);

    /**
     * Removes the given {@link IMouseListener}.
     *
     * May be called from whatever thread.
     *
     * @param mouseListener the listener to be removed
     * @throws NullPointerException if mouseListener is null
     */
    void removeMouseListener(IMouseListener mouseListener);

    /**
     * Adds an {@link IFocusListener}
     * to the browser.
     * This can be used to react to focus gaining and focus losing of HTML elements.
     *
     * May be called from whatever thread.
     *
     * @param focusListener the listener to be added
     * @throws NullPointerException if focusListener is null
     */
    void addFocusListener(IFocusListener focusListener);

    /**
     * Removes the given {@link IFocusListener}.
     *
     * May be called from whatever thread.
     *
     * @param focusListener the listener to be removed
     * @throws NullPointerException if focusListener is null
     */
    void removeFocusListener(IFocusListener focusListener);

    /**
     * Adds an {@link IDNDListener}
     * to the browser.
     * This can be used to react to drag and drop events.
     *
     * May be called from whatever thread.
     *
     * @param dNDListener the listener to be added
     * @throws NullPointerException if dNDListener is null
     */
    void addDNDListener(IDNDListener dNDListener);

    /**
     * Removes the given {@link IDNDListener}.
     *
     * May be called from whatever thread.
     *
     * @param dNDListener the listener to be removed
     * @throws NullPointerException if dNDListener is null
     */
    void removeDNDListener(IDNDListener dNDListener);
}
