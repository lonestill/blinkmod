package com.blinkmap.mod;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class BadgeManager {
    private static final String TAG = "BlinkMod_Badge";
    private static final String BADGES_URL = "https://raw.githubusercontent.com/lonestill/blinkmod/main/badges.json";
    private static final String PREFS_NAME = "blink_badges_prefs";
    private static final String KEY_CACHE = "badges_json_cache";
    private static final String KEY_LAST_FETCH = "badges_last_fetch";
    private static final long CACHE_TTL_MS = 30 * 1000; // 30 seconds for quick updates

    private static Context appContext = null;
    private static final Map<String, String> BADGES = new ConcurrentHashMap<>();
    private static final AtomicBoolean IS_FETCHING = new AtomicBoolean(false);
    private static long lastFetchTime = 0;

    static {
        // Safe offline default
        BADGES.put("3266908271", "mod developer");
        BADGES.put("lonestill", "mod developer");
    }

    public static void init(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();
        loadFromCache();
        refreshAsync(false);
    }

    private static SharedPreferences prefs() {
        if (appContext == null) return null;
        try {
            return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void loadFromCache() {
        try {
            SharedPreferences p = prefs();
            if (p != null) {
                lastFetchTime = p.getLong(KEY_LAST_FETCH, 0);
                String cachedJson = p.getString(KEY_CACHE, null);
                if (cachedJson != null && cachedJson.length() > 0) {
                    parseJson(cachedJson);
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to load cache: " + t.getMessage());
        }
    }

    public static void refreshAsync(final boolean force) {
        if (!force && System.currentTimeMillis() - lastFetchTime < CACHE_TTL_MS) {
            return;
        }
        if (!IS_FETCHING.compareAndSet(false, true)) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Cache-buster query param prevents GitHub raw CDN from returning stale file
                    String urlWithTs = BADGES_URL + "?t=" + System.currentTimeMillis();
                    URL url = new URL(urlWithTs);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setUseCaches(false);
                    conn.setRequestProperty("User-Agent", "BlinkMod-BadgeEngine/1.1");
                    conn.setRequestProperty("Cache-Control", "no-cache");

                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 300) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        reader.close();

                        String jsonStr = sb.toString();
                        parseJson(jsonStr);

                        lastFetchTime = System.currentTimeMillis();
                        SharedPreferences p = prefs();
                        if (p != null) {
                            p.edit()
                                    .putString(KEY_CACHE, jsonStr)
                                    .putLong(KEY_LAST_FETCH, lastFetchTime)
                                    .apply();
                        }
                        Log.i(TAG, "Badges refreshed from GitHub: " + BADGES.size() + " entries");
                    } else {
                        Log.w(TAG, "Failed to fetch badges, HTTP code: " + code);
                    }
                    conn.disconnect();
                } catch (Throwable t) {
                    Log.w(TAG, "Error fetching badges from GitHub: " + t.getMessage());
                } finally {
                    IS_FETCHING.set(false);
                }
            }
        }, "BlinkMod-BadgeFetcher").start();
    }

    private static void parseJson(String jsonStr) {
        if (jsonStr == null) return;
        try {
            // Strip trailing commas before } or ] so lenient parsing works even if user edited JSON manually
            jsonStr = jsonStr.replaceAll(",\\s*([\\}\\]])", "$1");

            JSONObject root = new JSONObject(jsonStr);
            JSONObject badgesObj = root.has("badges") ? root.optJSONObject("badges") : root;
            if (badgesObj == null) return;

            Iterator<String> keys = badgesObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object val = badgesObj.get(key);
                String badgeText = null;
                if (val instanceof String) {
                    badgeText = (String) val;
                } else if (val instanceof JSONObject) {
                    JSONObject obj = (JSONObject) val;
                    if (obj.has("badge")) badgeText = obj.optString("badge");
                    else if (obj.has("title")) badgeText = obj.optString("title");
                    else if (obj.has("text")) badgeText = obj.optString("text");
                    else if (obj.has("name")) badgeText = obj.optString("name");
                }
                if (badgeText != null && badgeText.length() > 0) {
                    String cleanKey = key.trim();
                    BADGES.put(cleanKey, badgeText);
                    BADGES.put(cleanKey.toLowerCase(), badgeText);
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Error parsing badges JSON: " + t.getMessage());
        }
    }

    public static String getBadge(String username, long accountId) {
        if (System.currentTimeMillis() - lastFetchTime > CACHE_TTL_MS) {
            refreshAsync(false);
        }

        if (accountId > 0) {
            String badge = BADGES.get(String.valueOf(accountId));
            if (badge != null) return badge;
        }

        if (username != null && username.length() > 0) {
            String cleanUser = username.trim().toLowerCase();
            if (cleanUser.contains(" • ")) {
                cleanUser = cleanUser.substring(0, cleanUser.indexOf(" • ")).trim();
            }
            if (cleanUser.startsWith("@")) {
                cleanUser = cleanUser.substring(1);
            }
            String badge = BADGES.get(cleanUser);
            if (badge != null) return badge;
        }

        return null;
    }

    public static String getBadge(String username, String accountIdStr) {
        long id = 0;
        if (accountIdStr != null) {
            try {
                id = Long.parseLong(accountIdStr.trim());
            } catch (Throwable ignored) {}
        }
        return getBadge(username, id);
    }

    public static Object patchProfile(Object geObj) {
        if (geObj == null) return null;
        try {
            Class<?> clazz = geObj.getClass();

            // 1. Extract accountId from ge.a (Led)
            long accountId = 0;
            try {
                Field aField = clazz.getDeclaredField("a");
                aField.setAccessible(true);
                Object ledObj = aField.get(geObj);
                if (ledObj != null) {
                    Method getValue = ledObj.getClass().getMethod("getValue");
                    Object val = getValue.invoke(ledObj);
                    if (val instanceof Long) {
                        accountId = (Long) val;
                    }
                }
            } catch (Throwable ignored) {}

            // 2. Extract username from ge.c
            Field cField = null;
            String username = null;
            try {
                cField = clazz.getDeclaredField("c");
                cField.setAccessible(true);
                Object val = cField.get(geObj);
                if (val instanceof String) {
                    username = (String) val;
                }
            } catch (Throwable ignored) {}

            // 3. Resolve badge
            String badge = getBadge(username, accountId);
            if (badge != null && cField != null && username != null && username.length() > 0) {
                // Strip previous badge if already attached
                String baseUsername = username;
                if (baseUsername.contains(" • ")) {
                    baseUsername = baseUsername.substring(0, baseUsername.indexOf(" • ")).trim();
                }
                String patchedUsername = baseUsername + " • " + badge;
                cField.set(geObj, patchedUsername);
                Log.i(TAG, "Patched username pill: @" + baseUsername + " -> " + patchedUsername);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Error in patchProfile: " + t.getMessage(), t);
        }

        return geObj;
    }
}
