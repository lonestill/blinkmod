package com.blinkmap.mod;

import android.content.Context;
import android.content.SharedPreferences;

/** Called from tiny smali bridges inside Blink's step request/display models. */
public final class StepHooks {
    private static volatile Context app;
    public static volatile boolean hideCharge;
    public static volatile int forcedCharge = -1;
    public static volatile int chargeFilterHits;
    public static volatile boolean pauseLocation;
    public static volatile long pauseUntil;
    public static volatile int locationBlockHits;
    public static volatile long geoIntervalMs;
    public static volatile long lastGeoSent;
    public static volatile int geoRateLimitHits;
    public static volatile int locationPrecision;
    public static volatile int locationPrecisionHits;
    private StepHooks() {}

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
        }
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

    private static void log(String message) {
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

    private static int saturating(int value, int multiplier) {
        long result = (long) value * multiplier;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, result));
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
