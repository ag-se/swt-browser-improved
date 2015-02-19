package de.fu_berlin.inf.ag_se.browser.extensions;

import com.google.common.collect.Iterables;
import de.fu_berlin.inf.ag_se.browser.InternalBrowserWrapper;
import de.fu_berlin.inf.ag_se.browser.utils.colors.ColorUtils;
import de.fu_berlin.inf.ag_se.browser.utils.colors.RGB;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;

public class BootstrapBrowser extends JQueryBrowser implements IBootstrapBrowser {
    private static final Logger LOGGER = Logger.getLogger(BootstrapBrowser.class);


    public BootstrapBrowser(InternalBrowserWrapper internalBrowserWrapper) {
        this(internalBrowserWrapper, Collections.<BrowserExtension>emptyList());
    }

    public BootstrapBrowser(InternalBrowserWrapper internalBrowserWrapper, Iterable<BrowserExtension> extensions) {
        super(internalBrowserWrapper, Iterables.concat(extensions, Arrays.asList(BrowserExtension.BOOTSTRAP_EXTENSION)));
    }

    /**
     * Calculates a Bootstrap button's border color based on its background
     * color.
     *
     * @param backgroundColor
     * @return
     */
    public static RGB getBorderColor(RGB backgroundColor) {
        return ColorUtils.addLightness(backgroundColor, -0.05f);
    }

    /**
     * Calculates a Bootstrap button's hovered border color based on its
     * background color.
     *
     * @param backgroundColor
     * @return
     */
    public static RGB getHoverColor(RGB backgroundColor) {
        return ColorUtils.addLightness(backgroundColor, -0.10f);
    }

    /**
     * Calculates a Bootstrap button's hovered background color based on its
     * background color.
     *
     * @param backgroundColor
     * @return
     */
    public static RGB getHoverBorderColor(RGB backgroundColor) {
        return ColorUtils.addLightness(backgroundColor, -0.17f);
    }

    public static enum ButtonOption {
        DEFAULT, PRIMARY, SUCCESS, INFO, WARNING, DANGER;

        public RGB getColor() {
            switch (this) {
                case DANGER:
                    return new RGB(217, 83, 79);
                case DEFAULT:
                    return new RGB(255, 255, 255);
                case INFO:
                    return new RGB(91, 192, 222);
                case PRIMARY:
                    return new RGB(66, 139, 202);
                case SUCCESS:
                    return new RGB(92, 184, 92);
                case WARNING:
                    return new RGB(240, 173, 78);
                default:
                    return null;
            }
        }

        @Override
        public String toString() {
            switch (this) {
                case DANGER:
                    return "btn-danger";
                case DEFAULT:
                    return "btn-default";
                case INFO:
                    return "btn-info";
                case PRIMARY:
                    return "btn-primary";
                case SUCCESS:
                    return "btn-success";
                case WARNING:
                    return "btn-warning";
                default:
                    return "";
            }
        }
    }

    public static enum ButtonSize {
        LARGE, DEFAULT, SMALL, EXTRA_SMALL;

        @Override
        public String toString() {
            switch (this) {
                case LARGE:
                    return "btn-lg";
                case DEFAULT:
                    return "";
                case SMALL:
                    return "btn-sm";
                case EXTRA_SMALL:
                    return "btn-xs";
                default:
                    return "";
            }
        }

        ;
    }

    public static enum ButtonStyle {
        HORIZONTAL, DROPDOWN;
    }
}
