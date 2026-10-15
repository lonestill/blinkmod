package com.blinkmap.mod;

import android.content.Context;

/**
 * Global configuration and feature flags for BlinkMod.
 */
public final class ModConfig {
    public static final boolean IS_DEV_BUILD = false;
    public static final String VERSION_PUBLIC = "2.1 Test";

    public static final String PREFS_NAME = "blink_mod_prefs";

    private ModConfig() {}

    public static boolean isDevMode(Context context) {
        return false;
    }

    public static boolean toggleDevMode(Context context) {
        return false;
    }

    public static String getVersionDisplay(Context context) {
        return "версия " + VERSION_PUBLIC;
    }
}
