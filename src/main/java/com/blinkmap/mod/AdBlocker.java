package com.blinkmap.mod;

import android.content.Context;
import android.content.SharedPreferences;

public final class AdBlocker {
    private static final String PREFS_NAME = "blinkmod_settings";
    private static final String KEY_ADBLOCK_ENABLED = "adblock_enabled";
    private static final String KEY_BLOCK_ANALYTICS = "block_analytics_enabled";

    private AdBlocker() {}

    public static boolean isAdBlockEnabled() {
        SharedPreferences p = getPrefs();
        return p == null || p.getBoolean(KEY_ADBLOCK_ENABLED, true);
    }

    public static boolean isBlockAnalyticsEnabled() {
        SharedPreferences p = getPrefs();
        return p == null || p.getBoolean(KEY_BLOCK_ANALYTICS, true);
    }

    private static SharedPreferences getPrefs() {
        Context c = StepHooks.getApp();
        if (c != null) {
            return c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object appObj = at.getMethod("currentApplication").invoke(null);
            if (appObj instanceof Context) {
                return ((Context) appObj).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
