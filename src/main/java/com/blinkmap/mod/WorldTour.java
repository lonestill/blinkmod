package com.blinkmap.mod;

import android.content.Context;
import android.os.PowerManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public final class WorldTour {
    public static final int MODE_COUNTRIES_ONLY = 0;
    public static final int MODE_ALL_CITIES = 1;
    public static final int MODE_CAPITALS = 2;
    public static final int MODE_GLOBE_GRID = 3;
    public static final int MODE_TOP1_BOOST = 4;
    public static final int MODE_MISSING_ONLY = 5;
    public static final int MODE_CITIES = MODE_ALL_CITIES;

    public static final class City {
        public final String name;
        public final double lat;
        public final double lon;

        public City(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    public static final City[] WORLD_CAPITALS = new City[] {
        new City("Москва (Россия)", 55.7558, 37.6173),
        new City("Санкт-Петербург (Россия)", 59.9311, 30.3609),
        new City("Минск (Беларусь)", 53.9006, 27.5590),
        new City("Астана (Казахстан)", 51.1694, 71.4491),
        new City("Ташкент (Узбекистан)", 41.2995, 69.2401),
        new City("Баку (Азербайджан)", 40.4093, 49.8671),
        new City("Ереван (Армения)", 40.1792, 44.4991),
        new City("Тбилиси (Грузия)", 41.7151, 44.8271),
        new City("Лондон (Великобритания)", 51.5074, -0.1278),
        new City("Париж (Франция)", 48.8566, 2.3522),
        new City("Берлин (Германия)", 52.5200, 13.4050),
        new City("Рим (Италия)", 41.9028, 12.4964),
        new City("Мадрид (Испания)", 40.4168, -3.7038),
        new City("Амстердам (Нидерланды)", 52.3676, 4.9041),
        new City("Вена (Австрия)", 48.2082, 16.3738),
        new City("Варшава (Польша)", 52.2297, 21.0122),
        new City("Стокгольм (Швеция)", 59.3293, 18.0686),
        new City("Хельсинки (Финляндия)", 60.1699, 24.9384),
        new City("Токио (Япония)", 35.6762, 139.6503),
        new City("Сеул (Южная Корея)", 37.5665, 126.9780),
        new City("Пхеньян (Северная Корея)", 39.0392, 125.7625),
        new City("Пекин (Китай)", 39.9042, 116.4074),
        new City("Тайбей (Тайвань)", 25.0330, 121.5654),
        new City("Сингапур", 1.3521, 103.8198),
        new City("Бангкок (Таиланд)", 13.7563, 100.5018),
        new City("Нью-Дели (Индия)", 28.6139, 77.2090),
        new City("Дубай (ОАЭ)", 25.2048, 55.2708),
        new City("Эр-Рияд (Саудовская Аравия)", 24.7136, 46.6753),
        new City("Тель-Авив (Израиль)", 32.0853, 34.7818),
        new City("Вашингтон (США)", 38.9072, -77.0369),
        new City("Нью-Йорк (США)", 40.7128, -74.0060),
        new City("Оттава (Канада)", 45.4215, -75.6972),
        new City("Мехико (Мексика)", 19.4326, -99.1332),
        new City("Бразилиа (Бразилия)", -15.7975, -47.8919),
        new City("Буэнос-Айрес (Аргентина)", -34.6037, -58.3816),
        new City("Каир (Египет)", 30.0444, 31.2357),
        new City("Претория (ЮАР)", -25.7479, 28.2293),
        new City("Канберра (Австралия)", -35.2809, 149.1300),
        new City("Веллингтон (Новая Зеландия)", -41.2865, 174.7762),
        new City("Гонолулу (Гавайи)", 21.3069, -157.8583),
        new City("Нукуалофа (Тонга)", -21.1394, -175.2018)
    };
    public static final City[] WORLD_CITIES = WORLD_CAPITALS;

    public static City[] loadFromAsset(Context context, String assetName, City[] fallback) {
        ArrayList<City> list = new ArrayList<City>();
        if (context != null) {
            try {
                InputStream is = context.getAssets().open(assetName);
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3) {
                        String name = parts[0];
                        double lat = Double.parseDouble(parts[1]);
                        double lon = Double.parseDouble(parts[2]);
                        list.add(new City(name, lat, lon));
                    }
                }
                br.close();
            } catch (Throwable ignored) {}
        }
        if (list.isEmpty()) {
            return fallback;
        }
        return list.toArray(new City[0]);
    }

    public static City[] generateGlobeGrid(int latStep, int lonStep) {
        ArrayList<City> list = new ArrayList<City>();
        for (int lat = -70; lat <= 70; lat += latStep) {
            for (int lon = -180; lon < 180; lon += lonStep) {
                String name = (lat >= 0 ? lat + "°N" : (-lat) + "°S") + ", " +
                              (lon >= 0 ? lon + "°E" : (-lon) + "°W");
                list.add(new City(name, (double) lat, (double) lon));
            }
        }
        return list.toArray(new City[0]);
    }

    private static volatile boolean running = false;
    private static volatile int currentMode = MODE_COUNTRIES_ONLY;
    private static volatile int currentIndex = 0;
    private static volatile int totalCount = 0;
    private static volatile String currentPlace = "";
    private static volatile Thread runnerThread = null;

    private WorldTour() {}

    public static boolean isRunning() {
        return running;
    }

    public static int getCurrentIndex() {
        return currentIndex;
    }

    public static int getTotalCount() {
        return totalCount;
    }

    public static String getCurrentPlace() {
        return currentPlace;
    }

    public static String getStatusText() {
        if (!running) {
            if (currentIndex > 0 && currentIndex >= totalCount) {
                return "Турне завершено (" + totalCount + " точек)";
            }
            if (currentMode == MODE_MISSING_ONLY && totalCount == 0) {
                return "Все страны уже посещены! Топ-1 взят!";
            }
            return "Не активно";
        }
        return "В пути: " + currentPlace + " (" + (currentIndex + 1) + "/" + totalCount + ")";
    }

    public static synchronized void start(final Context context, final int mode, final long delayMs, final Runnable onProgress) {
        startInternal(context, mode, null, delayMs, onProgress);
    }

    public static synchronized void startCustom(final Context context, final City[] customTargets, final long delayMs, final Runnable onProgress) {
        startInternal(context, MODE_MISSING_ONLY, customTargets, delayMs, onProgress);
    }

    private static synchronized void startInternal(final Context context, final int mode, final City[] explicitTargets, final long delayMs, final Runnable onProgress) {
        stop();

        currentMode = mode;
        final City[] targets;
        if (explicitTargets != null) {
            targets = explicitTargets;
        } else if (mode == MODE_COUNTRIES_ONLY) {
            targets = loadFromAsset(context, "world_countries.dat", WORLD_CAPITALS);
        } else if (mode == MODE_ALL_CITIES) {
            targets = loadFromAsset(context, "world_cities.dat", WORLD_CAPITALS);
        } else if (mode == MODE_GLOBE_GRID) {
            targets = generateGlobeGrid(15, 20);
        } else if (mode == MODE_TOP1_BOOST) {
            targets = loadFromAsset(context, "top1_boost.dat", WORLD_CAPITALS);
        } else if (mode == MODE_MISSING_ONLY) {
            java.util.List<City> missing = VisitedCountriesManager.getMissingCountries(context);
            targets = missing.isEmpty() ? new City[0] : missing.toArray(new City[0]);
        } else {
            targets = WORLD_CAPITALS;
        }

        totalCount = targets.length;
        currentIndex = 0;
        if (targets.length == 0) {
            running = false;
            currentPlace = (mode == MODE_MISSING_ONLY) ? "Все страны уже посещены! Топ-1 взят!" : "Список пуст";
            if (onProgress != null) {
                try {
                    onProgress.run();
                } catch (Throwable ignored) {}
            }
            return;
        }
        running = true;

        runnerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                City lastCity = null;
                PowerManager.WakeLock wakeLock = null;
                if (context != null) {
                    try {
                        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                        if (pm != null) {
                            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BlinkMod:WorldTour");
                            wakeLock.acquire(30 * 60 * 1000L); // 30 minutes max
                        }
                    } catch (Throwable ignored) {}
                }

                try {
                    for (int i = 0; i < targets.length && running; i++) {
                        City city = targets[i];
                        lastCity = city;
                        currentIndex = i;
                        currentPlace = city.name;

                        // Update in-memory coordinates directly to avoid thrashing disk
                        StepHooks.customLat = city.lat;
                        StepHooks.customLon = city.lon;
                        StepHooks.driftCurLat = city.lat;
                        StepHooks.driftCurLon = city.lon;
                        StepHooks.dispatchSyntheticLocation();
                        VisitedCountriesManager.markCountryVisited(city.name);

                        if (onProgress != null) {
                            try {
                                onProgress.run();
                            } catch (Throwable ignored) {}
                        }

                        // Multi-step car driving simulation per country (~50 km/h, switches Blink avatar to vehicle mode)
                        long dwell = delayMs >= 3000L ? delayMs : 6000L;
                        int steps = Math.max(4, (int)(dwell / 1000L)); // 1 step per second like real GPS
                        long stepSleep = dwell / steps;

                        double curLat = city.lat;
                        double curLon = city.lon;
                        float curBearing = (float)(Math.abs(city.name.hashCode()) % 360);

                        for (int step = 0; step < steps && running; step++) {
                            // Realistic car driving speed: 48 - 54 km/h (~13.3 - 15.0 m/s)
                            // Speed > 7 m/s triggers car/vehicle icon on Blink map avatar
                            float speedKmh = 49.0f + (float)(Math.sin(step * 0.7) * 4.5);
                            float stepSpeed = speedKmh / 3.6f;

                            StepHooks.customLat = curLat;
                            StepHooks.customLon = curLon;
                            StepHooks.driftCurLat = curLat;
                            StepHooks.driftCurLon = curLon;
                            StepHooks.driftSpeed = stepSpeed;
                            StepHooks.driftBearing = curBearing;
                            StepHooks.dispatchSyntheticLocation();

                            Thread.sleep(stepSleep);
                            if (!running) break;

                            // Real displacement at ~50 km/h: ~13-15 meters per second
                            double distMeters = stepSpeed * (stepSleep / 1000.0);
                            double rad = Math.toRadians(curBearing);
                            double dLat = (distMeters * Math.cos(rad)) / 111139.0;
                            double dLon = (distMeters * Math.sin(rad)) / (111139.0 * Math.max(0.15, Math.cos(Math.toRadians(curLat))));

                            curLat += dLat;
                            curLon += dLon;
                            // Smooth road turning (~3.5 deg per second)
                            curBearing = (float)((curBearing + 3.5f) % 360.0f);
                        }
                    }
                } catch (InterruptedException ignored) {
                } catch (Throwable e) {
                    currentPlace = "Ошибка: " + e.getClass().getSimpleName();
                } finally {
                    running = false;
                    if (wakeLock != null && wakeLock.isHeld()) {
                        try {
                            wakeLock.release();
                        } catch (Throwable ignored) {}
                    }
                    if (lastCity != null && context != null) {
                        try {
                            StepHooks.setCustomLocation(lastCity.lat, lastCity.lon);
                        } catch (Throwable ignored) {}
                    }
                    if (onProgress != null) {
                        try {
                            onProgress.run();
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }, "BlinkMod-WorldTour");

        runnerThread.start();
    }

    public static synchronized void stop() {
        running = false;
        if (runnerThread != null && runnerThread.isAlive()) {
            runnerThread.interrupt();
            runnerThread = null;
        }
    }
}
