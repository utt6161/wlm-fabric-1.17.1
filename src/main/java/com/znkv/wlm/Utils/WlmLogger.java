package com.znkv.wlm.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WlmLogger {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void logError(String error) {
        LOGGER.error("[Wlm] " + error);
    }

    public static void logInfo(String info) {
        LOGGER.info("[Wlm] " + info);
    }
}
