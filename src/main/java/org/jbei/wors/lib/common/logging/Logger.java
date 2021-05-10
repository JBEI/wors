package org.jbei.wors.lib.common.logging;

import org.jbei.wors.lib.utils.Utils;
import org.slf4j.LoggerFactory;

/**
 * Logger for ICE.
 *
 * @author Hector Plahar
 */
public class Logger {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("org.jbei.ice");

    public static void error(String message) {
        LOGGER.error(message);
    }

    public static void error(String message, Throwable e) {
        LOGGER.error(message, e);
    }

    public static void error(Throwable e) {
        String message = Utils.stackTraceToString(e);
        LOGGER.error(e.getMessage(), e);
    }

    public static boolean isDebugEnabled() {
        return LOGGER.isDebugEnabled();
    }

    public static void warn(String message) {
        LOGGER.warn(message);
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void debug(String message) {
        LOGGER.debug(message);
    }
}
