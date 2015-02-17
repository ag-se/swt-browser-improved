package de.fu_berlin.inf.ag_se.widgets.browser.extended;

import de.fu_berlin.inf.ag_se.utils.IConverter;
import de.fu_berlin.inf.ag_se.widgets.browser.Browser;
import de.fu_berlin.inf.ag_se.widgets.browser.IBrowser;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.extensions.IBrowserExtension;
import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Composite;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * This {@link IBrowser} behaves like the {@link Browser} but allows {@link de.fu_berlin.inf.ag_se.widgets.browser.extended.extensions.IBrowserExtension}s
 * to be automatically loaded when the requested {@link java.net.URI} was loaded.
 *
 * @author bkahlert
 */
public class ExtendedBrowser extends Browser {

    private static final Logger LOGGER = Logger
            .getLogger(ExtendedBrowser.class);

    private final IBrowserExtension[] extensions;

    public ExtendedBrowser(Composite parent, int style,
                           IBrowserExtension[] extensions) {
        super(parent, style);
        this.extensions = extensions;

        /*
         * TODO FIX BUG: afterCompletion is called after the DOMReady scripts.
         * PageLoad might need to access something loaded through extensions.
         */
        executeAfterCompletion(new Runnable() {
            @Override
            public void run() {
                for (IBrowserExtension extension : ExtendedBrowser.this.extensions) {
                    if (!addExtensionOnce(extension)) {
                        LOGGER.error("Error loading " + extension);
                    }
                }
            }
        });
    }

    private Boolean hasExtension(IBrowserExtension extension) {
        return syncRun(extension.getVerificationScript(),
                IConverter.CONVERTER_BOOLEAN);
    }

    private Boolean addExtension(IBrowserExtension extension) {
        for (Class<? extends IBrowserExtension> dependencyClass : extension
                .getDependencies()) {
            try {
                IBrowserExtension dependency = dependencyClass.newInstance();
                if (!this.addExtension(dependency)) {
                    LOGGER.error("Dependency "
                            + dependency.getName()
                            + " could not be loaded. Still trying to add extension "
                            + extension.getName());
                }
            } catch (IllegalAccessException e) {
                LOGGER.warn(
                        "Cannot instantiate dependency "
                                + dependencyClass.getSimpleName()
                                + ". Skipping.", e);
            } catch (InstantiationException e) {
                LOGGER.warn(
                        "Cannot instantiate dependency "
                                + dependencyClass.getSimpleName()
                                + ". Skipping.", e);
            }
        }

        boolean success = true;
        for (File jsExtension : extension.getJsExtensions()) {
            try {
                // by running the extension directly we execute it synchronously
                // otherwise a loader library would be necessary to satisfy the
                // loading dependencies
                runContentAsScriptTag(jsExtension);
            } catch (RuntimeException e) {
                LOGGER.error(
                        "Could not load the JS extension \""
                                + extension.getName() + "\".", e);
                success = false;
            } catch (IOException e) {
                LOGGER.error(
                        "Could not load the JS extension \""
                                + extension.getName() + "\".", e);
                success = false;
            }
        }
        for (URI cssExtension : extension.getCssExtensions()) {
            try {
                injectCssURI(cssExtension);
            } catch (RuntimeException e) {
                LOGGER.error(
                        "Could not load the JS extension \""
                                + extension.getName() + "\".", e);
                success = false;
            }
        }

        return success;
    }

    private Boolean addExtensionOnce(IBrowserExtension extension) {
        if (!this.hasExtension(extension)) {
            return this.addExtension(extension);
        }
        return true;
    }
}
