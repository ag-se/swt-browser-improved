package de.fu_berlin.inf.ag_se.demoSuits.browser;

import de.fu_berlin.inf.ag_se.browser.JxBrowser;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public class JxBrowserDemo {

    private static final Logger LOGGER = Logger.getLogger(JxBrowserDemo.class);

    private JxBrowser browser;
    private String alertString = "Hello World!";
    private static String timeoutString = "15000";

    Executor executor = new Executor() {
        @Override
        public void execute(Runnable command) {
            new Thread(command).start();
        }
    };

    public void createDemo(Container container) {
        browser = new JxBrowser(container);
        browser.setAllowLocationChange(true);

        final Future<Boolean> success = browser.openBlank();
//        final Future<Boolean> success = browser.open("https://google.de", Integer.parseInt(timeoutString));
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (success.get()) {
                        log("Page loaded successfully");
                    } else {
                        log("Page load timed out");
                    }
                } catch (Exception e) {
                    log(e.getMessage());
                }
                log(browser.getUrl());
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1920, 1080);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        new JxBrowserDemo().createDemo(frame);

    }

    public static void log(final String message) {
        LOGGER.info(message);
    }

    public static void log(Throwable e) {
        log(e.getMessage());
    }
}
