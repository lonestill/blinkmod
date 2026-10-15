package com.blinkmap.mod;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

/**
 * Core runtime hook router for BlinkMod.
 *
 * Intercepts and overrides critical framework and application events directly
 * from bytecode-level hooks injected into Blink's decompiled smali:
 * - Geolocation pipeline (GPS spoofing, vehicle drift, map pointer positioning)
 * - Step synchronization and display
 * - Battery charge spoofing and suppression
 * - Rate limiting and transmission pause
 *
 * CRITICAL COMPATIBILITY NOTE:
 * The methods {@link #isSpoofingActive()} and {@link #hookUserLatLng} are called
 * directly from original smali classes (e.g. UserPointerController, a15, ef0, syh).
 * Do NOT remove or modify their signatures.
 */
public final class StepHooks {
    private static volatile Context app;

    // --- Battery Hooks ---
    public static volatile boolean hideCharge;
    public static volatile int forcedCharge = -1;
    public static volatile int chargeFilterHits;

    // --- Location Transmission & Pause ---
    public static volatile boolean pauseLocation;
    public static volatile long pauseUntil;
    public static volatile int locationBlockHits;
    public static volatile long geoIntervalMs;
    public static volatile long lastGeoSent;
    public static volatile int geoRateLimitHits;
    public static volatile int locationPrecision;
    public static volatile int locationPrecisionHits;

    // --- Coordinates & Spoofing State ---
    public static volatile double customLat;
    public static volatile double customLon;
    public static volatile double driftCurLat;
    public static volatile double driftCurLon;
    public static volatile Object lastLocationCallback;
    public static volatile long lastRealLocationTime;
    public static volatile double driftAngle;
    public static volatile float driftBearing;
    public static volatile float driftSpeed = 1.25f;
    public static volatile int driftMode = 0;
    public static volatile int driftHits = 0;
    public static volatile int driftStepsAdded = 0;
    public static volatile long driftLastTime = 0;

    private StepHooks() {}

    public static void setCustomLocation(double lat, double lon) {
        customLat = lat;
        customLon = lon;
        driftCurLat = lat;
        driftCurLon = lon;
        SharedPreferences p = prefs();
        if (p != null) {
            p.edit()
                .putLong("custom_lat_bits", Double.doubleToRawLongBits(lat))
                .putLong("custom_lon_bits", Double.doubleToRawLongBits(lon))
                .apply();
        }
    }

    public static double getDriftLat(double original) {
        if (driftCurLat != 0.0) return driftCurLat;
        if (customLat != 0.0) return customLat;
        return original;
    }

    public static double getDriftLon(double original) {
        if (driftCurLon != 0.0) return driftCurLon;
        if (customLon != 0.0) return customLon;
        return original;
    }

    public static float getDriftSpeed(float original) {
        if (customLat != 0.0 || driftCurLat != 0.0) {
            return driftSpeed > 0 ? driftSpeed : 1.25f;
        }
        return original;
    }

    public static float getDriftBearing(float original) {
        if (customLat != 0.0 || driftCurLat != 0.0) {
            return driftBearing;
        }
        return original;
    }

    public static String customCoordsText() {
        if (customLat == 0.0 && customLon == 0.0) {
            return "Текущая геопозиция (GPS)";
        }
        return customLat + ", " + customLon;
    }

    public static String driftStatus() {
        if (driftMode == 0) {
            return "Выключен (реальный GPS)";
        }
        if (driftMode == 1) {
            return "На месте (дрейф ±10 м)";
        }
        if (driftMode == 2) {
            return "Ходьба 4.5 км/ч (+" + driftStepsAdded + " шагов)";
        }
        if (driftMode == 3) {
            return "Бег 12 км/ч (+" + driftStepsAdded + " шагов)";
        }
        if (driftMode == 4) {
            return "Авто 45 км/ч (маршрут)";
        }
        return "Дрейф активен";
    }

    public static void setDriftMode(int mode) {
        driftMode = mode;
        SharedPreferences p = prefs();
        if (p != null) {
            p.edit().putInt("drift_mode", mode).apply();
        }
    }

    public static boolean setCustomLocationFromClipboard(Context context) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData clip = cm.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text != null) {
                        String s = text.toString().replace(',', ' ').replace(';', ' ').trim();
                        String[] parts = s.split("\\s+");
                        if (parts.length >= 2) {
                            double lat = Double.parseDouble(parts[0]);
                            double lon = Double.parseDouble(parts[1]);
                            if (lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0) {
                                setCustomLocation(lat, lon);
                                Toast.makeText(context, "Точка задана: " + lat + ", " + lon, Toast.LENGTH_SHORT).show();
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        Toast.makeText(context, "В буфере нет координат! Пример: 55.7558, 37.6173", Toast.LENGTH_LONG).show();
        return false;
    }

    public static void setCustomLocationFromCurrent(Context context) {
        if (driftCurLat == 0.0 && driftCurLon == 0.0) {
            Toast.makeText(context, "Геопозиция ещё не получена", Toast.LENGTH_SHORT).show();
            return;
        }
        setCustomLocation(driftCurLat, driftCurLon);
        Toast.makeText(context, "Текущая точка зафиксирована!", Toast.LENGTH_SHORT).show();
    }

    public static void resetCustomLocation(Context context) {
        customLat = 0.0;
        customLon = 0.0;
        driftCurLat = 0.0;
        driftCurLon = 0.0;
        SharedPreferences p = prefs();
        if (p != null) {
            p.edit().remove("custom_lat_bits").remove("custom_lon_bits").apply();
        }
        if (context != null) {
            Toast.makeText(context, "Сброшено на реальный GPS", Toast.LENGTH_SHORT).show();
        }
    }

    public static void updateLocation(double lat, double lon) {
        if (FriendFollower.isRunning()) {
            return;
        }
        double baseLat = customLat != 0.0 ? customLat : lat;
        double baseLon = customLon != 0.0 ? customLon : lon;

        if (driftMode == 0) {
            driftCurLat = baseLat;
            driftCurLon = baseLon;
            driftSpeed = 0.0f;
            driftBearing = 0.0f;
            return;
        }

        driftHits++;
        long now = System.currentTimeMillis();

        if (driftMode == 1) {
            double t1 = (now % 300000L) / 1000.0;
            driftCurLat = baseLat + Math.sin(0.15 * t1) * 2.2486636513157897E-5;
            driftCurLon = baseLon + Math.cos(0.11 * t1) * 1.398770028068492E-4;
            driftSpeed = 0.2f;
            driftBearing = (float) ((now / 1000L) % 360L);
            return;
        }

        long last = driftLastTime;
        if (last == 0L) last = now;
        long dt = now - last;
        driftLastTime = now;
        if (dt > 60000L) dt = 60000L;
        if (dt < 0L) dt = 0L;

        double dtSec = dt / 1000.0;
        double speedMps;
        double radiusLat;
        double radiusLon;
        double stepsPerSec;

        if (driftMode == 3) {
            // Бег (~12 км/ч = 3.33 м/с)
            speedMps = 3.33;
            radiusLat = 0.00225;
            radiusLon = 0.00385;
            stepsPerSec = 3.1;
        } else if (driftMode == 4) {
            // Авто (~45 км/ч = 12.5 м/с)
            speedMps = 12.5;
            radiusLat = 0.00720;
            radiusLon = 0.01230;
            stepsPerSec = 0.0;
        } else {
            // Ходьба (~4.5 км/ч = 1.25 м/с)
            speedMps = 1.25;
            radiusLat = 0.0010798666727554458;
            radiusLon = 0.0018496725973614566;
            stepsPerSec = 1.6875;
        }

        int steps = (int) (stepsPerSec * dtSec);
        if (steps > 0) {
            driftStepsAdded += steps;
        }

        double omega = speedMps / (radiusLat * 111000.0);
        double angle = driftAngle + dtSec * omega;
        if (angle > 6.283185307179586) {
            angle -= 6.283185307179586;
        }
        driftAngle = angle;

        driftCurLat = baseLat + Math.sin(angle) * radiusLat;
        driftCurLon = baseLon + Math.cos(angle) * radiusLon;
        driftSpeed = (float) speedMps;
        driftBearing = (float) Math.toDegrees(angle);
    }

    public static void hookLocation(android.location.Location location) {
        if (location == null) return;
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        updateLocation(lat, lon);
        lastRealLocationTime = System.currentTimeMillis();
        location.setLatitude(getDriftLat(lat));
        location.setLongitude(getDriftLon(lon));
        location.setSpeed(getDriftSpeed(location.getSpeed()));
        float bearing = getDriftBearing(location.getBearing());
        if (bearing != 0.0f) {
            location.setBearing(bearing);
        }
    }

    public static boolean isSpoofingActive() {
        if (FriendFollower.isRunning()) return true;
        if (driftMode != 0) return true;
        if (customLat != 0.0 || customLon != 0.0) return true;
        return false;
    }

    public static com.blinkmap.geography.core.LatLng hookUserLatLng(com.blinkmap.geography.core.LatLng latLng) {
        return hookUserLatLng(latLng, true);
    }

    public static com.blinkmap.geography.core.LatLng hookUserLatLng(com.blinkmap.geography.core.LatLng latLng, boolean enabled) {
        if (latLng == null) return null;
        if (!enabled || !isSpoofingActive()) return latLng;
        double lat = getDriftLat(latLng.b);
        double lon = getDriftLon(latLng.c);
        if (lat == 0.0 && lon == 0.0) return latLng;
        return new com.blinkmap.geography.core.LatLng(lat, lon);
    }

    public static void dispatchSyntheticLocation() {
        Object callback = lastLocationCallback;
        if (callback == null) return;
        double lat = getDriftLat(0.0);
        double lon = getDriftLon(0.0);
        if (lat == 0.0 && lon == 0.0) return;
        try {
            android.location.Location loc = new android.location.Location("fused");
            loc.setLatitude(lat);
            loc.setLongitude(lon);
            loc.setAccuracy(3.5f);
            loc.setTime(System.currentTimeMillis());
            loc.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos());
            loc.setSpeed(getDriftSpeed(0.0f));
            loc.setBearing(getDriftBearing(0.0f));

            java.lang.reflect.Method m = callback.getClass().getMethod("fakeDispatch", android.location.Location.class);
            m.invoke(callback, loc);
        } catch (Throwable ignored) {}
    }

    public static void init(Context context) {
        if (context != null) {
            app = context.getApplicationContext();
            SharedPreferences p = prefs();
            hideCharge = p != null && p.getBoolean("hide_charge", false);
            forcedCharge = p == null ? -1 : p.getInt("forced_charge", -1);
            pauseLocation = p != null && p.getBoolean("pause_location", false);
            pauseUntil = p == null ? 0L : p.getLong("pause_until", 0L);
            geoIntervalMs = p == null ? 0L : p.getInt("geo_interval_minutes", 0) * 60000L;
            locationPrecision = p == null ? 0 : p.getInt("location_precision", 0);
            long latBits = p == null ? 0L : p.getLong("custom_lat_bits", 0L);
            long lonBits = p == null ? 0L : p.getLong("custom_lon_bits", 0L);
            if (latBits != 0L || lonBits != 0L) {
                customLat = Double.longBitsToDouble(latBits);
                customLon = Double.longBitsToDouble(lonBits);
                driftCurLat = customLat;
                driftCurLon = customLon;
            }
            if (p != null) {
                driftMode = p.getInt("drift_mode", 0);
            }
            FakeLocationPump.start();
            startLocationFallback(context);
            BadgeManager.init(context);

            try {
                android.content.IntentFilter filter = new android.content.IntentFilter();
                filter.addAction("com.blinkmap.mod.SET_LOCATION");
                android.content.BroadcastReceiver receiver = new android.content.BroadcastReceiver() {
                    @Override
                    public void onReceive(Context c, android.content.Intent intent) {
                        if (intent == null || intent.getAction() == null) return;
                        if ("com.blinkmap.mod.SET_LOCATION".equals(intent.getAction())) {
                            double lat = 0.0;
                            double lon = 0.0;
                            try {
                                if (intent.hasExtra("lat_s")) {
                                    lat = Double.parseDouble(intent.getStringExtra("lat_s"));
                                    lon = Double.parseDouble(intent.getStringExtra("lon_s"));
                                } else {
                                    lat = (double) intent.getFloatExtra("lat", (float) intent.getDoubleExtra("lat", 0.0));
                                    lon = (double) intent.getFloatExtra("lon", (float) intent.getDoubleExtra("lon", 0.0));
                                }
                            } catch (Throwable ignored) {}
                            if (lat != 0.0 || lon != 0.0) {
                                setCustomLocation(lat, lon);
                                dispatchSyntheticLocation();
                            }
                        }
                    }
                };
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    app.registerReceiver(receiver, filter, 2);
                } else {
                    app.registerReceiver(receiver, filter);
                }
            } catch (Throwable ignored) {}
        }
    }

    public static Context getApp() {
        return app;
    }

    public static void setHideCharge(boolean enabled) { hideCharge = enabled; }
    public static void setForcedCharge(int charge) {
        forcedCharge = charge < 0 ? -1 : Math.min(100, charge);
        hideCharge = false;
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt("forced_charge", forcedCharge).putBoolean("hide_charge", false).apply();
    }
    public static void setPauseLocation(boolean enabled) {
        pauseLocation = enabled;
        pauseUntil = 0L;
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean("pause_location", enabled).remove("pause_until").apply();
    }

    public static void setGeoIntervalMinutes(int minutes) {
        int safe = minutes == 5 || minutes == 15 || minutes == 30 ? minutes : 0;
        geoIntervalMs = safe * 60000L;
        lastGeoSent = 0L;
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt("geo_interval_minutes", safe).apply();
    }

    public static String geoIntervalStatus() {
        long minutes = geoIntervalMs / 60000L;
        return minutes <= 0L ? "без ограничений" : "не чаще раза в " + minutes + " мин";
    }

    public static void setLocationPrecision(int decimals) {
        locationPrecision = decimals == 3 || decimals == 2 ? decimals : 0;
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt("location_precision", locationPrecision).apply();
    }

    public static String locationPrecisionStatus() {
        if (locationPrecision == 3) return "район, примерно 100 м";
        if (locationPrecision == 2) return "грубо, примерно 1 км";
        return "точная позиция";
    }

    public static void pauseFor(long durationMillis) {
        pauseLocation = false;
        pauseUntil = System.currentTimeMillis() + Math.max(1000L, durationMillis);
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean("pause_location", false).putLong("pause_until", pauseUntil).apply();
    }

    public static void resumeLocation() {
        pauseLocation = false;
        pauseUntil = 0L;
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean("pause_location", false).remove("pause_until").apply();
    }

    public static String pauseStatus() {
        if (pauseLocation) return "бессрочно";
        long left = pauseUntil - System.currentTimeMillis();
        if (left <= 0L) return "не активна";
        long minutes = (left + 59999L) / 60000L;
        return minutes >= 60 ? (minutes / 60) + " ч " + (minutes % 60) + " мин" : minutes + " мин";
    }

    private static SharedPreferences prefs() {
        Context c = app;
        return c == null ? null : c.getSharedPreferences("blinkmod_settings", Context.MODE_PRIVATE);
    }

    public static int outgoingSteps(int original) {
        SharedPreferences p = prefs();
        if (p != null && p.getBoolean("force_100k_once", false)) {
            p.edit().putBoolean("force_100k_once", false)
                    .putInt("last_network_original", original)
                    .putInt("last_network_result", 100000)
                    .putLong("last_network_time", System.currentTimeMillis()).apply();
            return 100000;
        }
        if (p == null || !p.getBoolean("network_steps_enabled", false)) return original;
        int multiplier = clamp(p.getInt("network_steps_multiplier", 2), 1, 10);
        long changed = (long) original * multiplier;
        int result = (int) Math.min(100000L, Math.max(0L, changed));
        p.edit().putInt("last_network_original", original)
                .putInt("last_network_result", result)
                .putLong("last_network_time", System.currentTimeMillis()).apply();
        return result;
    }

    public static void arm100k() {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean("force_100k_once", true).apply();
    }

    public static void onApiStarted() {
        SharedPreferences p = prefs();
        if (p != null) p.edit()
                .putString("api_steps_state", "Отправляется запрос на 100 000 шагов")
                .putLong("api_steps_time", System.currentTimeMillis()).apply();
        log("POST steps: отправка 100 000");
    }

    /** Receives Kotlin's boxed Result without linking the Java module to Kotlin classes. */
    public static void onApiResult(Object result) {
        Throwable failure = null;
        if (result != null && result.getClass().getName().contains("Result$Failure")) {
            try {
                java.lang.reflect.Field field = result.getClass().getDeclaredField("exception");
                field.setAccessible(true);
                Object value = field.get(result);
                if (value instanceof Throwable) failure = (Throwable) value;
            } catch (Throwable ignored) {}
        }
        if (failure != null) {
            onApiError(failure);
            return;
        }
        SharedPreferences p = prefs();
        if (p != null) p.edit()
                .putString("api_steps_state", "Запрос принят, проверяем результат")
                .putLong("api_steps_time", System.currentTimeMillis()).apply();
        log("POST steps: сервер ответил успешно");
    }

    public static void onApiVerified(int steps) {
        SharedPreferences p = prefs();
        if (p != null) p.edit()
                .putString("api_steps_state", "Сервер вернул " + steps + " шагов за день")
                .putInt("api_steps_verified", steps)
                .putLong("api_steps_time", System.currentTimeMillis()).apply();
        log("GET day: " + steps + " шагов");
    }

    public static void onApiVerifyError(Throwable error) {
        SharedPreferences p = prefs();
        String name = error == null ? "неизвестная ошибка" : error.getClass().getSimpleName();
        if (p != null) p.edit()
                .putString("api_steps_state", "Запрос принят; проверка: " + name)
                .putLong("api_steps_time", System.currentTimeMillis()).apply();
        log("GET day: ошибка " + name);
    }

    public static void onApiError(Throwable error) {
        SharedPreferences p = prefs();
        String name = error == null ? "неизвестная ошибка" : error.getClass().getSimpleName();
        if (p != null) p.edit()
                .putString("api_steps_state", "Ошибка: " + name)
                .putLong("api_steps_time", System.currentTimeMillis()).apply();
        log("POST steps: ошибка " + name);
    }

    public static void onApiCheckStarted() {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putString("api_steps_state", "Проверяем подключение к API").apply();
        log("GET day: ручная проверка");
    }

    public static void clearLog() {
        SharedPreferences p = prefs();
        if (p != null) p.edit().remove("lab_log").apply();
    }

    public static void onChatSearchStarted(String query) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putString("chat_query", query == null ? "" : query)
                .putString("chat_state", "Ищем чаты...").apply();
    }

    public static void onChatSearchResult(String rows, int count) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putString("chat_results", rows == null ? "" : rows)
                .putString("chat_state", "Найдено чатов: " + count).apply();
    }

    public static void onChatSearchError(Throwable error) {
        SharedPreferences p = prefs();
        String name = error == null ? "неизвестная ошибка" : error.getClass().getSimpleName();
        if (p != null) p.edit().putString("chat_state", "Ошибка: " + name).apply();
    }

    public static void onFriendsStarted() {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putString("friends_state", "Загружаем список друзей").apply();
    }

    public static void onFriendsResult(String rows, int count) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putString("friends_results", rows == null ? "" : rows)
                .putString("friends_state", "Загружено: " + count).apply();
    }

    public static void onFriendsError(Throwable error) {
        SharedPreferences p = prefs();
        String name = error == null ? "неизвестная ошибка" : error.getClass().getSimpleName();
        if (p != null) p.edit().putString("friends_state", "Ошибка: " + name).apply();
    }

    public static void onProfileStarted() {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putString("profile_state", "Обновляем профиль").apply();
    }

    public static void onProfileResult(String name, String username, long accountId) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putString("profile_name", name == null ? "" : name)
                .putString("profile_username", username == null ? "" : username)
                .putString("profile_account_id", String.valueOf(accountId))
                .putString("profile_state", "Профиль обновлён").apply();
    }

    public static void onProfileStats(long friends, long views, long stars, long checkins) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putLong("profile_friends", friends)
                .putLong("profile_views", views).putLong("profile_stars", stars)
                .putLong("profile_checkins", checkins).apply();
    }

    public static void onProfileError(Throwable error) {
        SharedPreferences p = prefs();
        String name = error == null ? "неизвестная ошибка" : error.getClass().getSimpleName();
        if (p != null) p.edit().putString("profile_state", "Ошибка: " + name).apply();
    }

    public static void onUserLookupStarted(String username) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putString("user_lookup_query", username == null ? "" : username)
                .putString("user_lookup_state", "Ищем пользователя").apply();
    }

    public static void onUserLookupResult(String name, String username, String city, long accountId) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putString("user_lookup_name", name == null ? "" : name)
                .putString("user_lookup_username", username == null ? "" : username)
                .putString("user_lookup_city", city == null ? "" : city)
                .putString("user_lookup_id", String.valueOf(accountId))
                .putString("user_lookup_state", "Пользователь найден").apply();
    }

    public static void onUserLookupStats(long friends, long views, long stars, long checkins) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putLong("user_lookup_friends", friends)
                .putLong("user_lookup_views", views).putLong("user_lookup_stars", stars)
                .putLong("user_lookup_checkins", checkins).apply();
    }

    public static void onUserLookupError(Throwable error) {
        SharedPreferences p = prefs();
        String name = error == null ? "неизвестная ошибка" : error.getClass().getSimpleName();
        if (p != null) p.edit().putString("user_lookup_state", "Ошибка: " + name).apply();
    }

    public static void onUsernameCheckStarted(String username) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putString("username_check_state", "Проверяем @" + username).apply();
    }

    public static void onUsernameCheckResult(boolean valid, String message) {
        SharedPreferences p = prefs();
        String state = valid ? "Username доступен" : (message == null || message.length() == 0 ? "Username недоступен" : message);
        if (p != null) p.edit().putString("username_check_state", state).apply();
    }

    public static void onUsernameCheckError(Throwable error) {
        SharedPreferences p = prefs();
        String name = error == null ? "неизвестная ошибка" : error.getClass().getSimpleName();
        if (p != null) p.edit().putString("username_check_state", "Ошибка: " + name).apply();
    }

    public static void log(String message) {
        SharedPreferences p = prefs();
        if (p == null) return;
        String time = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date());
        String old = p.getString("lab_log", "");
        String all = time + "  " + message + (old.length() == 0 ? "" : "\n" + old);
        String[] lines = all.split("\n");
        StringBuilder kept = new StringBuilder();
        for (int i = 0; i < lines.length && i < 8; i++) {
            if (i > 0) kept.append('\n');
            kept.append(lines[i]);
        }
        p.edit().putString("lab_log", kept.toString()).apply();
    }

    public static int displaySteps(int original) {
        SharedPreferences p = prefs();
        if (p == null || !p.getBoolean("local_steps_enabled", false)) return original;
        int multiplier = clamp(p.getInt("local_steps_multiplier", 2), 1, 10);
        return saturating(original, multiplier);
    }

    public static float displayKilometers(float original) {
        SharedPreferences p = prefs();
        if (p == null || !p.getBoolean("local_steps_enabled", false)) return original;
        return original * clamp(p.getInt("local_steps_multiplier", 2), 1, 10);
    }

    // --- Ghost & Abuse Suite ---
    public static volatile boolean ghostVoiceActive = true;
    public static volatile boolean antiSpoofShieldActive = true;

    public static boolean isGhostVoiceActive() {
        SharedPreferences p = prefs();
        return p != null ? p.getBoolean("ghost_voice_active", true) : ghostVoiceActive;
    }

    public static void setGhostVoiceActive(boolean active) {
        ghostVoiceActive = active;
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean("ghost_voice_active", active).apply();
    }

    public static boolean isAntiSpoofShieldActive() {
        SharedPreferences p = prefs();
        return p != null ? p.getBoolean("anti_spoof_shield", true) : antiSpoofShieldActive;
    }

    public static void setAntiSpoofShieldActive(boolean active) {
        antiSpoofShieldActive = active;
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean("anti_spoof_shield", active).apply();
    }

    public static void toast(final String message) {
        if (app == null || message == null) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(app, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void startLocationFallback(Context context) {
        if (context == null) return;
        try {
            final android.location.LocationManager lm = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return;

            android.location.Location lastGps = null;
            android.location.Location lastNet = null;
            try {
                if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    lastGps = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
                }
            } catch (Throwable ignored) {}
            try {
                if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                    lastNet = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
                }
            } catch (Throwable ignored) {}

            android.location.Location best = lastGps != null ? lastGps : lastNet;
            if (best != null) {
                hookLocation(best);
            }

            android.location.LocationListener listener = new android.location.LocationListener() {
                @Override
                public void onLocationChanged(android.location.Location location) {
                    if (location != null) {
                        hookLocation(location);
                    }
                }
                @Override public void onStatusChanged(String provider, int status, android.os.Bundle extras) {}
                @Override public void onProviderEnabled(String provider) {}
                @Override public void onProviderDisabled(String provider) {}
            };

            android.os.Looper looper = android.os.Looper.getMainLooper();
            if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 1000L, 0.0f, listener, looper);
            }
            if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 2000L, 0.0f, listener, looper);
            }
        } catch (Throwable ignored) {}
    }

    private static int saturating(int value, int multiplier) {
        long result = (long) value * multiplier;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, result));
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
