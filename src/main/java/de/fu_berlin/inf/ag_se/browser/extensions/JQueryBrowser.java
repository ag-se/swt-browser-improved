package de.fu_berlin.inf.ag_se.browser.extensions;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import de.fu_berlin.inf.ag_se.browser.InternalBrowserWrapper;
import de.fu_berlin.inf.ag_se.browser.exception.ScriptExecutionException;
import de.fu_berlin.inf.ag_se.browser.functions.CallbackFunction;
import de.fu_berlin.inf.ag_se.browser.html.Element;
import de.fu_berlin.inf.ag_se.browser.html.IElement;
import de.fu_berlin.inf.ag_se.browser.html.ISelector;
import de.fu_berlin.inf.ag_se.browser.utils.IConverter;
import de.fu_berlin.inf.ag_se.browser.utils.Point;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class JQueryBrowser extends EventCatchBrowser implements IJQueryBrowser {
    private static final Logger LOGGER = Logger.getLogger(JQueryBrowser.class);

    private Point disposedScrollPosition = null;

    public JQueryBrowser(InternalBrowserWrapper internalBrowserWrapper) {
        this(internalBrowserWrapper, Collections.<BrowserExtension>emptyList());
    }

    public JQueryBrowser(InternalBrowserWrapper internalBrowserWrapper, Iterable<BrowserExtension> extensions) {
        super(internalBrowserWrapper, Iterables.concat(extensions, Arrays.asList(BrowserExtension.JQUERY_EXTENSION)));
//TODO this is called too late as the browser is already disposed
//        runOnDisposal(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    disposedScrollPosition = getScrollPosition().get();
//                } catch (Exception e1) {
//                    LOGGER.error("Error saving state of " + JQueryBrowser.this, e1);
//                }
//            }
//        });
    }

    private String getFocusStmt(ISelector selector) {
        return "$('" + selector + "').focus()";
    }

    private String getBlurStmt(ISelector selector) {
        return "$('" + selector + "').blur()";
    }

    private String getKeyUpStmt(ISelector selector) {
        return "$('" + selector + "').keyup()";
    }

    private String getKeyDownStmt(ISelector selector) {
        return "$('" + selector + "').keydown()";
    }

    private String getKeyPressStmt(ISelector selector) {
        return "$('" + selector + "').keypress()";
    }

    private String getSubmitStmt(ISelector selector) {
        return "$('" + selector + "').closest('form').submit();";
    }

    private String getValStmt(ISelector selector, String value) {
        return "$('" + selector + "').val('" + value + "');";
    }

    /**
     * function simulateKeyPress($elements, eventType) { var keyboardEvent =
     * document.createEvent("KeyboardEvent"); var initMethod = typeof
     * keyboardEvent.initKeyboardEvent !== 'undefined' ? "initKeyboardEvent" :
     * "initKeyEvent"; keyboardEvent[initMethod]( eventType, // event type :
     * keydown, keyup, keypress true, // bubbles true, // cancelable window, //
     * viewArg: should be window false, // ctrlKeyArg false, // altKeyArg false,
     * // shiftKeyArg false, // metaKeyArg 16, // keyCodeArg : unsigned long the
     * virtual key code, else 0 0 // charCodeArgs : unsigned long the Unicode
     * character associated with the depressed key, else 0 );
     * $elements.each(function() { this.dispatchEvent(keyboardEvent); }); }
     *
     * {@see http://stackoverflow.com/questions/596481/simulate-javascript-key-events}
     */
    private String getSimulateKeyPress() {
        return "function simulateKeyPress(e,t){var n=document.createEvent('KeyboardEvent');var r=typeof n.initKeyboardEvent!=='undefined'?'initKeyboardEvent':'initKeyEvent';n[r](t,true,true,window,false,false,false,false,16,0);e.each(function(){this.dispatchEvent(n)})}";
    }

    private String getForceKeyPressStmt(ISelector selector) {
        return this.getSimulateKeyPress() + "simulateKeyPress($('" + selector
                + "'), 'keydown');" + "simulateKeyPress($('" + selector
                + "'), 'keypress');" + "simulateKeyPress($('" + selector
                + "'), 'keyup');";
    }

    @Override
    public Future<Boolean> containsElement(ISelector selector) {
        return this.run("return $('" + selector.toString() + "').length > 0;",
                IConverter.CONVERTER_BOOLEAN);
    }

    @Override
    public Future<Point> getScrollPosition() {
        if (this.disposedScrollPosition != null) {
            return Futures.immediateFuture(this.disposedScrollPosition);
        }
        return JQueryBrowser.this
                .run("return [jQuery(document).scrollLeft(),jQuery(document).scrollTop()];",
                        IConverter.CONVERTER_POINT);
    }

    @Override
    public Future<Point> getScrollPosition(final ISelector selector) {
        String jQuery = "jQuery('" + selector + "')";
        if (selector instanceof ISelector.IdSelector) {
            // preferred if id contains special characters
            jQuery = "jQuery(document.getElementById(\""
                    + ((ISelector.IdSelector) selector).getId() + "\"))";
        }
        if (selector instanceof ISelector.NameSelector) {
            // preferred if name contains special characters
            jQuery = "jQuery(document.getElementsByName(\""
                    + ((ISelector.NameSelector) selector).getName() + "\")[0])";
        }
        return JQueryBrowser.this
                .run("var offset = "
                                + jQuery
                                + ".offset(); return offset ? [offset.left, offset.top] : null;",
                        IConverter.CONVERTER_POINT);
    }

    @Override
    public Future<Point> getRelativePosition(final ISelector selector) {
        return JQueryBrowser.this
                .run("var offset = jQuery('"
                                + selector
                                + "').offset();return [offset.left-jQuery(document).scrollLeft(),offset.top-jQuery(document).scrollTop()];",
                        IConverter.CONVERTER_POINT);
    }

    @Override
    public Future<Boolean> scrollTo(final int x, final int y) {
        String script = String
                .format("if(jQuery(document).scrollLeft()!=%d||jQuery(document).scrollTop()!=%d){jQuery('html, body').animate({ scrollLeft: %d, scrollTop: %d }, 0);return true;}else{return false;}",
                        x, y, x, y);
        return JQueryBrowser.this.run(script, IConverter.CONVERTER_BOOLEAN);
    }

    @Override
    public Future<Boolean> scrollTo(Point pos) {
        return this.scrollTo(pos.x, pos.y);
    }

    @Override
    public Future<Boolean> scrollTo(final ISelector selector) {
        return runWithCallback(getScrollPosition(selector), new CallbackFunction<Point, Boolean>() {
            @Override
            public Boolean apply(Point input, Exception e) {
                try {
                    return JQueryBrowser.this.scrollTo(input).get();
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    return false;
                } catch (ExecutionException e1) {
                    throw new ScriptExecutionException("Could not execute scroll to", e);
                }
            }
        });
//		return UIThreadAwareScheduledThreadPoolExecutor.getInstance().nonUIAsyncExec(JQueryBrowser.class, "Scroll To",
//                new NoCheckedExceptionCallable<Boolean>() {
//                    @Override
//                    public Boolean call() {
//                        Point pos = null;
//                        try {
//                            pos = JQueryBrowser.this.getScrollPosition(selector).get();
//                            return JQueryBrowser.this.scrollTo(pos).get();
//                        } catch (InterruptedException e) {
//                            Thread.currentThread().interrupt();
//                        } catch (ExecutionException e) {
//                            throw new ScriptExecutionException("Could not execute scroll to", e);
//                        }
//                        return false;
//                    }
//                });
    }

    @Override
    public Future<Object> focus(ISelector selector) {
        return this.run(this.getFocusStmt(selector));
    }

    @Override
    public Future<IElement> getFocusedElement() {
        return run("return jQuery(document.activeElement).clone().wrap(\"<p>\").parent().html();", IConverter.CONVERTER_STRING,
                new CallbackFunction<String, IElement>() {
                    @Override
                    public IElement apply(String input, Exception e) {
                        if (e != null) {
                            LOGGER.error("Error getting scroll position", e);
                        }
                        return new Element(input);
                    }
                });
//		return UIThreadAwareScheduledThreadPoolExecutor.getInstance().nonUIAsyncExec(JQueryBrowser.class,
//                "Get Focused Element", new NoCheckedExceptionCallable<IElement>() {
//                    @Override
//                    public IElement call() {
//                        try {
//                            String html = run("return jQuery(document.activeElement).clone().wrap(\"<p>\").parent().html();",
//                                    IConverter.CONVERTER_STRING).get();
//                            return new Element(html);
//                        } catch (RuntimeException e) {
//                            LOGGER.error("Error getting scroll position", e);
//                        } catch (ExecutionException e) {
//                            LOGGER.error("Error getting scroll position", e);
//                        } catch (InterruptedException e) {
//                            Thread.currentThread().interrupt();
//                        }
//                        return null;
//                    }
//                });
    }

    @Override
    public Future<Object> blur(ISelector selector) {
        return this.run(this.getBlurStmt(selector));
    }

    @Override
    public Future<Object> keyUp(ISelector selector) {
        return this.run(this.getKeyUpStmt(selector));
    }

    @Override
    public Future<Object> keyDown(ISelector selector) {
        return this.run(this.getKeyDownStmt(selector));
    }

    @Override
    public Future<Object> keyPress(ISelector selector) {
        return this.run(this.getKeyPressStmt(selector));
    }

    @Override
    public Future<Object> forceKeyPress(ISelector selector) {
        return this.run(this.getForceKeyPressStmt(selector));
    }

    @Override
    public Future<Object> simulateTyping(final ISelector selector,
                                         final String text) {
        // return ExecutorUtil.nonUIAsyncExec(new Callable<Object>() {
        // @Override
        // public Object call() throws Exception {
        // Rectangle browserBounds = ShellUtils
        // .getInnerArea(BootstrapBrowser.this);
        // Point elementRelativePos = BootstrapBrowser.this
        // .getRelativePosition(selector).get();
        // Point whereToClickToFocus = new Point(browserBounds.x
        // + elementRelativePos.x + 3, browserBounds.y
        // + elementRelativePos.y + 3);
        // Robot robot = ShellUtils.getRobot();
        // robot.mouseMove(whereToClickToFocus.x, whereToClickToFocus.y);
        // robot.mousePress(InputEvent.BUTTON1_MASK);
        // robot.mouseRelease(InputEvent.BUTTON1_MASK);
        // for (int i = 0; i < text.length(); i++) {
        // robot.keyPress(text.codePointAt(i));
        // }
        // return null;
        // }
        // });
        return this.run(this.getFocusStmt(selector)
                + this.getValStmt(selector, text)
                + this.getForceKeyPressStmt(selector)
                + this.getBlurStmt(selector));
    }

    @Override
    public Future<Object> val(ISelector selector, String value) {
        return this.run(this.getValStmt(selector, value));
    }

    @Override
    public Future<Object> submit(ISelector selector) {
        return this.run(this.getSubmitStmt(selector));
    }

}
