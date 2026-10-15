package com.blinkmap.mod;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AchievementFarmer {
    private static final String BASE_URL = "https://pin.apiblink.ru";
    private static final AtomicBoolean isRunning = new AtomicBoolean(false);

    public static final String[] SEARCH_QUERIES = {
            "church", "mosque", "synagogue", "temple", "museum", "gallery", "theatre", "cinema", "monument", "ruins", "castle",
            "hospital", "clinic", "dentist", "doctor", "pharmacy", "veterinary", "beauty", "hairdresser", "barber", "spa", "massage",
            "cafe", "bar", "pub", "restaurant", "bakery", "butcher", "coffee", "tea", "chocolate", "beer", "wine", "tobacco", "ice cream",
            "supermarket", "convenience", "clothes", "fashion", "shoes", "electronics", "computer", "phone", "books", "antiques",
            "furniture", "toys", "baby", "jewelry", "pawnshop", "second_hand", "florist", "optics", "bicycle", "photo", "laundry",
            "gym", "fitness", "sports", "swimming", "pool", "stadium", "tennis", "basketball", "soccer", "billiards", "bowling",
            "casino", "park", "camp", "beach", "marina", "attraction",
            "airport", "aerodrome", "helipad", "fuel", "gas", "charging_station", "car", "car_wash", "car_repair", "bank", "atm",
            "school", "college", "university", "library", "post_office", "police", "townhall", "hotel", "motel", "hostel", "sauna"
    };

    public interface FarmingListener {
        void onProgress(int current, int total, String category, String placeName);
        void onDone(int successCount, int total);
        void onError(String message);
    }

    public static boolean isFarmingRunning() {
        return isRunning.get();
    }

    public static void stop() {
        isRunning.set(false);
    }

    public static void startFarming(final Context context, final FarmingListener listener) {
        if (!isRunning.compareAndSet(false, true)) {
            if (listener != null) listener.onError("Фарм уже выполняется!");
            return;
        }

        final Handler mainHandler = new Handler(Looper.getMainLooper());
        final String token = StepHooks.getAuthToken();
        if (token == null || token.isEmpty()) {
            isRunning.set(false);
            if (listener != null) {
                listener.onError("Токен авторизации не найден.\nОткройте вкладку «Профиль» и нажмите «Обновить данные».");
            }
            return;
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int successCount = 0;
                int totalCategories = 0;
                try {
                    // Step 1: Scan OSM places for categories
                    Map<String, JSONObject> placesByCategory = new LinkedHashMap<>();

                    // Initial query
                    scanPlaces(token, "", placesByCategory);

                    for (int i = 0; i < SEARCH_QUERIES.length; i++) {
                        if (!isRunning.get()) break;
                        String q = SEARCH_QUERIES[i];
                        scanPlaces(token, q, placesByCategory);
                        Thread.sleep(60);
                    }

                    totalCategories = placesByCategory.size();
                    if (totalCategories == 0) {
                        throw new Exception("Не удалось найти категории мест через API checkin");
                    }

                    int current = 0;
                    for (Map.Entry<String, JSONObject> entry : placesByCategory.entrySet()) {
                        if (!isRunning.get()) break;
                        current++;
                        final int curIdx = current;
                        final int totIdx = totalCategories;
                        final String cat = entry.getKey();
                        JSONObject place = entry.getValue();
                        final String name = place.optString("name", "Место");
                        String placeId = place.optString("id", "");
                        double lat = place.optDouble("latitude", 21.7778);
                        double lon = place.optDouble("longitude", -72.2611);
                        String addr = place.optString("address", "");

                        if (listener != null) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onProgress(curIdx, totIdx, cat, name);
                                }
                            });
                        }

                        boolean ok = postCheckin(token, cat, placeId, name, addr, lat, lon);
                        if (ok) successCount++;
                        Thread.sleep(120);
                    }

                    final int finalSuccess = successCount;
                    final int finalTotal = totalCategories;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            isRunning.set(false);
                            if (listener != null) listener.onDone(finalSuccess, finalTotal);
                        }
                    });

                } catch (final Throwable t) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            isRunning.set(false);
                            if (listener != null) listener.onError("Ошибка: " + t.getMessage());
                        }
                    });
                } finally {
                    isRunning.set(false);
                }
            }
        }, "blink-achievement-farmer");
        thread.start();
    }

    private static void scanPlaces(String token, String query, Map<String, JSONObject> outMap) {
        HttpURLConnection conn = null;
        try {
            String encodedQ = URLEncoder.encode(query, "UTF-8");
            URL url = new URL(BASE_URL + "/api/checkin/places?latitude=21.7778&longitude=-72.2611&search=" + encodedQ);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", token);
            conn.setRequestProperty("User-Agent", "okhttp/4.12.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() == 200) {
                String resp = readString(conn.getInputStream());
                JSONArray arr = new JSONArray(resp);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.optJSONObject(i);
                    if (item == null) continue;
                    JSONObject place = item.optJSONObject("place");
                    if (place == null) continue;
                    String cat = place.optString("categoryID", "");
                    String pid = place.optString("id", "");
                    if (!cat.isEmpty() && !pid.isEmpty() && !"city".equalsIgnoreCase(cat) && !outMap.containsKey(cat)) {
                        outMap.put(cat, place);
                    }
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static boolean postCheckin(String token, String categoryId, String placeId, String name, String address, double lat, double lon) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + "/api/checkin/v2");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "okhttp/4.12.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("latitude", lat);
            body.put("longitude", lon);
            body.put("categoryID", categoryId);
            body.put("placeID", placeId);
            body.put("placeName", name);
            body.put("placeAddress", address != null ? address : "");
            body.put("privacy", 0);
            body.put("tags", new JSONArray());

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            OutputStream os = conn.getOutputStream();
            os.write(bytes);
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                String resp = readString(conn.getInputStream());
                return resp.contains("checkin");
            }
        } catch (Throwable ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return false;
    }

    private static String readString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}
