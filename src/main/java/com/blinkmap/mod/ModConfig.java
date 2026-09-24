package com.blinkmap.mod;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Global configuration and feature flags for BlinkMod.
 * Manages Public (safe/release) vs Dev (research/testing) build flavors.
 */
public final class ModConfig {
    /**
     * Build flavor flag:
     * true  = Developer/Private build (Top-1 Sniper, Missing Countries Raid, Blink Lab enabled)
     * false = Public/Release build (Standard spoofing, safe travel modes, Lab hidden)
     */
    public static final boolean IS_DEV_BUILD = true;

    public static final String VERSION_PUBLIC = "2.0";
    public static final String VERSION_DEV = "2.0 Pro";

    public static final String PREFS_NAME = "blink_mod_prefs";
    public static final String PREF_DEV_UNLOCKED = "dev_mode_unlocked";

    private ModConfig() {}

    /**
     * Determines whether developer tools and aggressive boost features are accessible.
     */
    public static boolean isDevMode(Context context) {
        if (IS_DEV_BUILD) return true;
        if (context == null) return false;
        try {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getBoolean(PREF_DEV_UNLOCKED, false);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Toggles dev mode unlock state for public builds (via easter egg).
     */
    public static boolean toggleDevMode(Context context) {
        if (context == null) return false;
        try {
            SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean current = sp.getBoolean(PREF_DEV_UNLOCKED, false);
            boolean newState = !current;
            sp.edit().putBoolean(PREF_DEV_UNLOCKED, newState).apply();
            return newState;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Returns formatted display version string.
     */
    public static String getVersionDisplay(Context context) {
        if (isDevMode(context)) {
            return "версия " + VERSION_DEV;
        }
        return "версия " + VERSION_PUBLIC;
    }
}
