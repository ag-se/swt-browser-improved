package de.fu_berlin.inf.ag_se.browser.extensions;

import com.google.common.collect.Iterables;
import de.fu_berlin.inf.ag_se.browser.BrowserUtils;
import de.fu_berlin.inf.ag_se.browser.InternalBrowserWrapper;
import de.fu_berlin.inf.ag_se.browser.functions.JavascriptFunction;
import de.fu_berlin.inf.ag_se.browser.html.Anchor;
import de.fu_berlin.inf.ag_se.browser.html.IAnchor;
import de.fu_berlin.inf.ag_se.browser.html.IElement;
import de.fu_berlin.inf.ag_se.browser.listener.IAnchorListener;
import de.fu_berlin.inf.ag_se.browser.listener.IDNDListener;
import de.fu_berlin.inf.ag_se.browser.listener.IFocusListener;
import de.fu_berlin.inf.ag_se.browser.listener.IMouseListener;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class EventCatchBrowser extends ExtendedBrowser implements IEventCatchBrowser {

    private static Logger LOGGER = Logger.getLogger(EventCatchBrowser.class);

    private final List<IAnchorListener> anchorListeners = new ArrayList<IAnchorListener>();

    private final List<IMouseListener> mouseListeners = new ArrayList<IMouseListener>();

    private final List<IFocusListener> focusListeners = new ArrayList<IFocusListener>();

    private final List<IDNDListener> dndListeners = new ArrayList<IDNDListener>();

    public EventCatchBrowser(InternalBrowserWrapper internalBrowserWrapper) {
            this(internalBrowserWrapper, Collections.<BrowserExtension>emptyList());
        }

    public EventCatchBrowser(InternalBrowserWrapper internalBrowserWrapper,
                             Iterable<BrowserExtension> extensions) {
        super(internalBrowserWrapper, Iterables.concat(extensions, Arrays.asList(BrowserExtension.EVENT_CATCH_EXTENSION)));

        executeAfterCompletion(new Runnable() {
            @Override
            public void run() {
                for (JavascriptFunction browserFunction : getEventCatchBrowserFunctions()) {
                    createBrowserFunction(browserFunction);
                }
            }
        });
    }

    @Override
    public void addAnchorListener(IAnchorListener anchorListener) {
        checkNotNull(anchorListener);
        anchorListeners.add(anchorListener);
    }

    @Override
    public void removeAnchorListener(IAnchorListener anchorListener) {
        checkNotNull(anchorListener);
        anchorListeners.add(anchorListener);
    }

    @Override
    public void addMouseListener(IMouseListener mouseListener) {
        checkNotNull(mouseListener);
        mouseListeners.add(mouseListener);
    }

    @Override
    public void removeMouseListener(IMouseListener mouseListener) {
        checkNotNull(mouseListener);
        mouseListeners.remove(mouseListener);
    }

    @Override
    public void addFocusListener(IFocusListener focusListener) {
        checkNotNull(focusListener);
        focusListeners.add(focusListener);
    }

    @Override
    public void removeFocusListener(IFocusListener focusListener) {
        checkNotNull(focusListener);
        focusListeners.remove(focusListener);
    }

    @Override
    public void addDNDListener(IDNDListener dndListener) {
        checkNotNull(dndListener);
        dndListeners.add(dndListener);
    }

    @Override
    public void removeDNDListener(IDNDListener dndListener) {
        checkNotNull(dndListener);
        dndListeners.remove(dndListener);
    }

    protected void fireAnchorHover(String html, boolean mouseEnter) {
        IElement element = BrowserUtils.extractElement(html);
        IAnchor anchor = new Anchor(element.getAttributes(), element.getContent());
        for (IAnchorListener anchorListener : anchorListeners) {
            anchorListener.anchorHovered(anchor, mouseEnter);
        }
    }

    protected void fireMouseMove(double x, double y) {
        for (IMouseListener mouseListener : mouseListeners) {
            mouseListener.mouseMove(x, y);
        }
    }

    private void fireMouseDown(double x, double y, String html) {
        IElement element = BrowserUtils.extractElement(html);
        for (IMouseListener mouseListener : mouseListeners) {
            mouseListener.mouseDown(x, y, element);
        }
    }

    private void fireMouseUp(double x, double y, String html) {
        IElement element = BrowserUtils.extractElement(html);
        for (IMouseListener mouseListener : mouseListeners) {
            mouseListener.mouseUp(x, y, element);
        }
    }

    private void fireClicked(Double x, Double y, String html) {
        IElement element = BrowserUtils.extractElement(html);
        for (IMouseListener mouseListener : mouseListeners) {
            mouseListener.clicked(x, y, element);
        }
    }

    private void fireDragStart(long offsetX, long offsetY,
                               IElement element, String mimeType, String data) {
        for (IDNDListener dndListener : dndListeners) {
            dndListener.dragStart(offsetX, offsetY, element, mimeType, data);
        }
    }

    private void fireDrop(long offsetX, long offsetY,
                          IElement element, String mimeType, String data) {
        for (IDNDListener dndListener : dndListeners) {
            dndListener.drop(offsetX, offsetY, element, mimeType, data);
        }
    }

    private void fireFocusGained(IElement element) {
        for (IFocusListener focusListener : focusListeners) {
            focusListener.focusGained(element);
        }
    }

    private void fireFocusLost(IElement element) {
        for (IFocusListener focusListener : focusListeners) {
            focusListener.focusLost(element);
        }
    }

    protected List<JavascriptFunction> getEventCatchBrowserFunctions() {
        return Arrays.asList(new JavascriptFunction("__mouseenter") {
                                 public Object function(Object[] arguments) {
                                     if (arguments.length == 1 && arguments[0] instanceof String) {
                                         fireAnchorHover((String) arguments[0], true);
                                     }
                                     return null;
                                 }
                             },
                new JavascriptFunction("__mouseleave") {
                    @Override
                    public Object function(Object[] arguments) {
                        if (arguments.length == 1 && arguments[0] instanceof String) {
                            fireAnchorHover((String) arguments[0], false);
                        }
                        return null;
                    }
                },
                new JavascriptFunction("__mousemove") {
                    @Override
                    public Object function(Object[] arguments) {
                        if (arguments.length == 2 && (arguments[0] == null
                                || arguments[0] instanceof Double) && (arguments[1] == null
                                || arguments[1] instanceof Double)) {

                            fireMouseMove((Double) arguments[0],
                                    (Double) arguments[1]);
                        }
                        return null;
                    }
                },
                new JavascriptFunction("__mousedown") {
                    @Override
                    public Object function(Object[] arguments) {
                        if (arguments.length == 3 && (arguments[0] == null
                                || arguments[0] instanceof Double) && (arguments[1] == null
                                || arguments[1] instanceof Double) && (arguments[2] == null
                                || arguments[2] instanceof String)) {

                            fireMouseDown((Double) arguments[0],
                                    (Double) arguments[1], (String) arguments[2]);
                        }
                        return null;
                    }
                },
                new JavascriptFunction("__mouseup") {
                    @Override
                    public Object function(Object[] arguments) {
                        if (arguments.length == 3 && (arguments[0] == null
                                || arguments[0] instanceof Double) && (arguments[1] == null
                                || arguments[1] instanceof Double) && (arguments[2] == null
                                || arguments[2] instanceof String)) {

                            fireMouseUp((Double) arguments[0],
                                    (Double) arguments[1], (String) arguments[2]);
                        }
                        return null;
                    }
                },
                new JavascriptFunction("__click") {
                    @Override
                    public Object function(Object[] arguments) {
                        if (arguments.length == 3 && (arguments[0] == null
                                || arguments[0] instanceof Double) && (arguments[1] == null
                                || arguments[1] instanceof Double) && (arguments[2] == null
                                || arguments[2] instanceof String)) {

                            fireClicked((Double) arguments[0],
                                    (Double) arguments[1], (String) arguments[2]);
                        }
                        return null;
                    }
                },
//                new JavascriptFunction("__focusgained") {
//                    @Override
//                    public Object function(Object[] arguments) {
//                        if (arguments.length == 1 && arguments[0] instanceof String) {
//                            final IElement element = new Element((String) arguments[0]);
//
//                            fireFocusGained(element);
//                        }
//                        return null;
//                    }
//                },
//                new JavascriptFunction("__focuslost") {
//                    @Override
//                    public Object function(Object[] arguments) {
//                        if (arguments.length == 1 && arguments[0] instanceof String) {
//                            final IElement element = new Element((String) arguments[0]);
//
//                            fireFocusLost(element);
//                        }
//                        return null;
//                    }
//                },
                new JavascriptFunction("__dragStart") {
                    @Override
                    public Object function(Object[] arguments) {
                        if (arguments.length == 5 && arguments[0] instanceof Double
                                && arguments[1] instanceof Double
                                && arguments[2] instanceof String
                                && arguments[3] instanceof String
                                && arguments[4] instanceof String) {
                            long offsetX = Math.round((Double) arguments[0]);
                            long offsetY = Math.round((Double) arguments[1]);
                            IElement element = BrowserUtils
                                    .extractElement((String) arguments[2]);
                            String mimeType = (String) arguments[3];
                            String data = (String) arguments[4];

                            fireDragStart(offsetX, offsetY, element, mimeType,
                                    data);
                        }
                        return null;
                    }
                },
                new JavascriptFunction("__drop") {
                    @Override
                    public Object function(Object[] arguments) {
                        if (arguments.length == 5 && arguments[0] instanceof Double
                                && arguments[1] instanceof Double
                                && arguments[2] instanceof String
                                && arguments[3] instanceof String
                                && arguments[4] instanceof String) {
                            long offsetX = Math.round((Double) arguments[0]);
                            long offsetY = Math.round((Double) arguments[1]);
                            IElement element = BrowserUtils
                                    .extractElement((String) arguments[2]);
                            String mimeType = (String) arguments[3];
                            String data = (String) arguments[4];

                            fireDrop(offsetX, offsetY, element, mimeType, data);
                        }
                        return null;
                    }
                },
                new JavascriptFunction("__consoleLog") {
                    @Override
                    public Object function(Object[] arguments) {
                        LOGGER.debug(StringUtils.join(arguments, ", "));
                        return null;
                    }
                },
                new JavascriptFunction("__consoleError") {
                    @Override
                    public Object function(Object[] arguments) {
                        LOGGER.error(StringUtils.join(arguments, ", "));
                        return null;
                    }
                });
    }
}
