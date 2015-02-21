package de.fu_berlin.inf.ag_se.browser.functions;

import org.apache.log4j.Logger;

public interface CallbackFunction<V, T> {
    T apply(V input, Exception e);

    public static final CallbackFunction ERROR_LOGGING_CALLBACK = new CallbackFunction() {

        private final Logger LOGGER = Logger.getLogger(CallbackFunction.class);

        @Override
        public Void apply(Object input, Exception e) {
            if (e != null) {
                LOGGER.error("Error in async call: ", e);
            }
            return null;
        }
    };
}

