package de.fu_berlin.inf.ag_se.widgets.browser;

import de.fu_berlin.inf.ag_se.utils.ClasspathFileUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.Anchor;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.Element;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IAnchor;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IAnchorListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IDNDListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IFocusListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IMouseListener;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Rectangle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EventCatchFunctionality {

    private static Logger LOGGER = Logger.getLogger(EventCatchFunctionality.class);

    private InternalBrowserWrapper browser;

    private final List<IAnchorListener> anchorListeners = new ArrayList<IAnchorListener>();
    private final List<IMouseListener> mouseListeners = new ArrayList<IMouseListener>();
    private final List<IFocusListener> focusListeners = new ArrayList<IFocusListener>();
    private final List<IDNDListener> dndListeners = new ArrayList<IDNDListener>();

    public EventCatchFunctionality(InternalBrowserWrapper browser) {
        this.browser = browser;
    }

    private boolean eventCatchScriptInjected = false;

    private void createBrowserFunctions() {
        browser.createBrowserFunction("__mouseenter", new IBrowserFunction() {
            public Object function(Object[] arguments) {
                if (arguments.length == 1 && arguments[0] instanceof String) {
                    fireAnchorHover((String) arguments[0], true);
                }
                return null;
            }
        });
        browser.createBrowserFunction("__mouseleave", new IBrowserFunction() {
            @Override
            public Object function(Object[] arguments) {
                if (arguments.length == 1 && arguments[0] instanceof String) {
                    fireAnchorHover((String) arguments[0], false);
                }
                return null;
            }
        });
        browser.createBrowserFunction("__mousemove", new IBrowserFunction() {
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
        });
        browser.createBrowserFunction("__mousedown", new IBrowserFunction() {
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
        });
        browser.createBrowserFunction("__mouseup", new IBrowserFunction() {
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
        });
        browser.createBrowserFunction("__click", new IBrowserFunction() {
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
        });
        browser.createBrowserFunction("__focusgained", new IBrowserFunction() {
            @Override
            public Object function(Object[] arguments) {
                if (arguments.length == 1 && arguments[0] instanceof String) {
                    final IElement element = new Element((String) arguments[0]);

                    fireFocusGained(element);
                }
                return null;
            }
        });
        browser.createBrowserFunction("__focuslost", new IBrowserFunction() {
            @Override
            public Object function(Object[] arguments) {
                if (arguments.length == 1 && arguments[0] instanceof String) {
                    final IElement element = new Element((String) arguments[0]);

                    fireFocusLost(element);
                }
                return null;
            }
        });
        browser.createBrowserFunction("__resize", new IBrowserFunction() {
            @Override
            public Object function(Object[] arguments) {
                if (arguments.length == 4 && (arguments[0] == null
                        || arguments[0] instanceof Double) && (arguments[1] == null
                        || arguments[1] instanceof Double) && (arguments[2] == null
                        || arguments[2] instanceof Double) && (arguments[3] == null
                        || arguments[3] instanceof Double)) {

                    browser.setCachedContentBounds(new Rectangle(
                            arguments[0] != null ?
                            (int) Math.round((Double) arguments[0]) :
                            0, arguments[1] != null ?
                               (int) Math.round((Double) arguments[1]) :
                               0, arguments[2] != null ?
                                  (int) Math.round((Double) arguments[2]) :
                                  Integer.MAX_VALUE, arguments[3] != null ?
                                                     (int) Math.round((Double) arguments[3]) :
                                                     Integer.MAX_VALUE));
                    LOGGER.debug("browser content resized to " + browser.getCachedContentBounds());
                    browser.layoutRoot();
                }
                return null;
            }
        });
        browser.createBrowserFunction("__dragStart", new IBrowserFunction() {
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
        });
        browser.createBrowserFunction("__drop", new IBrowserFunction() {
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
        });

        browser.createBrowserFunction("__consoleLog", new IBrowserFunction() {
            @Override
            public Object function(Object[] arguments) {
                LOGGER.debug(StringUtils.join(arguments, ", "));
                return null;
            }
        });

        browser.createBrowserFunction("__consoleError", new IBrowserFunction() {
            @Override
            public Object function(Object[] arguments) {
                LOGGER.error(StringUtils.join(arguments, ", "));
                return null;
            }
        });
    }


    /**
     * Injects the code needed for addAnchorListener, addFocusListener and addDNDListener to work. <p> The JavaScript remembers a
     * successful
     * injection in case to consecutive calls are made. <p> As soon as a successful injection has been registered, {@link
     * #eventCatchScriptInjected} is set so no unnecessary further injection is made.
     */
    public void injectEventCatchScript() {
        if (eventCatchScriptInjected) {
            return;
        }

        createBrowserFunctions();

        File events = ClasspathFileUtils.getFile("/events.js");
        try {
            browser.runContent(events);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof SWTException) {
                // disposed
            } else {
                LOGGER.error(
                        "Could not inject events catch script in " + browser
                                .getClass().getSimpleName(), e);
            }
        } catch (IOException e) {
            LOGGER.error("Could not inject drop catch script in " + browser.getClass().getSimpleName(), e);
        }

        File dnd = ClasspathFileUtils.getFile("/dnd.js");
        File dndCss = ClasspathFileUtils.getFile("/dnd.css");
        try {
            browser.runContent(dnd);
            browser.injectCssFile(dndCss.toURI());
        } catch (RuntimeException e) {
            if (e.getCause() instanceof SWTException) {
                // disposed
            } else {
                LOGGER.error("Could not inject drop catch script in " + browser.getClass().getSimpleName(), e);
            }
        } catch (IOException e) {
            LOGGER.error("Could not inject drop catch script in " + browser.getClass().getSimpleName(), e);
        }

        eventCatchScriptInjected = true;
    }

    /**
     * @param html
     * @param mouseEnter true if mouseenter; false otherwise
     */
    private void fireAnchorHover(String html, boolean mouseEnter) {
        IElement element = BrowserUtils.extractElement(html);
        IAnchor anchor = new Anchor(element.getAttributes(), element.getContent());
        for (IAnchorListener anchorListener : anchorListeners) {
            anchorListener.anchorHovered(anchor, mouseEnter);
        }
    }

    /**
     * @param x
     * @param y
     */
    private void fireMouseMove(double x, double y) {
        for (IMouseListener mouseListener : mouseListeners) {
            mouseListener.mouseMove(x, y);
        }
    }

    /**
     * @param x
     * @param y
     * @param html on which the mouse went down
     */
    private void fireMouseDown(double x, double y, String html) {
        IElement element = BrowserUtils.extractElement(html);
        for (IMouseListener mouseListener : mouseListeners) {
            mouseListener.mouseDown(x, y, element);
        }
    }

    /**
     * @param x
     * @param y
     * @param html on which the mouse went up
     */
    private void fireMouseUp(double x, double y, String html) {
        IElement element = BrowserUtils.extractElement(html);
        for (IMouseListener mouseListener : mouseListeners) {
            mouseListener.mouseUp(x, y, element);
        }
    }

    /**
     * @param y
     * @param x
     * @param html
     */
    private void fireClicked(Double x, Double y, String html) {
        IElement element = BrowserUtils.extractElement(html);
        for (IMouseListener mouseListener : mouseListeners) {
            mouseListener.clicked(x, y, element);
        }
    }

    synchronized protected void fireDragStart(long offsetX, long offsetY,
                                              IElement element, String mimeType, String data) {
        for (IDNDListener dndListener : dndListeners) {
            dndListener.dragStart(offsetX, offsetY, element, mimeType, data);
        }
    }

    synchronized protected void fireDrop(long offsetX, long offsetY,
                                         IElement element, String mimeType, String data) {
        for (IDNDListener dndListener : dndListeners) {
            dndListener.drop(offsetX, offsetY, element, mimeType, data);
        }
    }

    protected void fireFocusGained(IElement element) {
        for (IFocusListener focusListener : focusListeners) {
            focusListener.focusGained(element);
        }
    }

    protected void fireFocusLost(IElement element) {
        for (IFocusListener focusListener : focusListeners) {
            focusListener.focusLost(element);
        }
    }

    public void addAnchorListener(IAnchorListener anchorListener) {
        anchorListeners.add(anchorListener);
    }

    public void removeAnchorListener(IAnchorListener anchorListener) {
        anchorListeners.remove(anchorListener);
    }

    public void addMouseListener(IMouseListener mouseListener) {
        mouseListeners.add(mouseListener);
    }

    public void removeMouseListener(IMouseListener mouseListener) {
        mouseListeners.remove(mouseListener);
    }

    public void addFocusListener(IFocusListener focusListener) {
        this.focusListeners.add(focusListener);
    }

    public void removeFocusListener(IFocusListener focusListener) {
        this.focusListeners.remove(focusListener);
    }

    public void addDNDListener(IDNDListener dndListener) {
        this.dndListeners.add(dndListener);
    }

    public void removeDNDListener(IDNDListener dndListener) {
        this.dndListeners.remove(dndListener);
    }
}
