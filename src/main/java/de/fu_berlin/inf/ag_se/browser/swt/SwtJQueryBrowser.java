package de.fu_berlin.inf.ag_se.browser.swt;

import de.fu_berlin.inf.ag_se.browser.extensions.BrowserExtension;
import de.fu_berlin.inf.ag_se.browser.extensions.IJQueryBrowser;
import de.fu_berlin.inf.ag_se.browser.extensions.JQueryBrowser;
import de.fu_berlin.inf.ag_se.browser.html.IElement;
import de.fu_berlin.inf.ag_se.browser.html.ISelector;
import de.fu_berlin.inf.ag_se.browser.utils.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import java.util.Collections;
import java.util.concurrent.Future;

public class SwtJQueryBrowser<T extends JQueryBrowser> extends SWTEventCatchBrowser<T> implements IJQueryBrowser {

    /**
     * Constructs a new browser composite with the given styles.
     *
     * @param parent a widget which will be the parent of the new instance (cannot be null)
     * @param style  if {@link SWT#INHERIT_FORCE} is set the loaded page's
     */
    protected SwtJQueryBrowser(Composite parent, int style) {
        super(parent, style);
    }

    public static SwtJQueryBrowser createSWTBrowser(Composite parent, int style) {
        return createSWTBrowser(parent, style, Collections.<BrowserExtension>emptyList());
    }

    public static SwtJQueryBrowser createSWTBrowser(Composite parent, int style, Iterable<BrowserExtension> extensions) {
        SwtJQueryBrowser<JQueryBrowser> swtJQueryBrowser = new SwtJQueryBrowser<JQueryBrowser>(parent, style);
        SwtInternalBrowserWrapper internalSWTBrowserWrapper = new SwtInternalBrowserWrapper(swtJQueryBrowser);
        swtJQueryBrowser.setInternalBrowser(internalSWTBrowserWrapper);
        swtJQueryBrowser.setBrowser(new JQueryBrowser(internalSWTBrowserWrapper, extensions));
        return swtJQueryBrowser;
    }

    @Override
    public Future<Boolean> containsElement(ISelector selector) {
        return browser.containsElement(selector);
    }

    @Override
    public Future<Point> getRelativePosition(ISelector selector) {
        return browser.getRelativePosition(selector);
    }

    @Override
    public Future<Point> getScrollPosition() {
        return browser.getScrollPosition();
    }

    @Override
    public Future<Point> getScrollPosition(ISelector selector) {
        return browser.getScrollPosition(selector);
    }

    @Override
    public Future<Boolean> scrollTo(int x, int y) {
        return browser.scrollTo(x, y);
    }

    @Override
    public Future<Boolean> scrollTo(Point pos) {
        return browser.scrollTo(pos);
    }

    @Override
    public Future<Boolean> scrollTo(ISelector selector) {
        return browser.scrollTo(selector);
    }

    @Override
    public Future<Object> focus(ISelector selector) {
        return browser.focus(selector);
    }

    @Override
    public Future<IElement> getFocusedElement() {
        return browser.getFocusedElement();
    }

    @Override
    public Future<Object> blur(ISelector selector) {
        return browser.blur(selector);
    }

    @Override
    public Future<Object> keyUp(ISelector selector) {
        return browser.keyUp(selector);
    }

    @Override
    public Future<Object> keyDown(ISelector selector) {
        return browser.keyDown(selector);
    }

    @Override
    public Future<Object> keyPress(ISelector selector) {
        return browser.keyPress(selector);
    }

    @Override
    public Future<Object> forceKeyPress(ISelector selector) {
        return browser.forceKeyPress(selector);
    }

    @Override
    public Future<Object> simulateTyping(ISelector selector, String text) {
        return browser.simulateTyping(selector, text);
    }

    @Override
    public Future<Object> val(ISelector selector, String value) {
        return browser.val(selector, value);
    }

    @Override
    public Future<Object> submit(ISelector selector) {
        return browser.submit(selector);
    }
}
