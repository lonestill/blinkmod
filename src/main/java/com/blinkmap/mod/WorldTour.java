package com.blinkmap.mod;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public final class WorldTour {
    public static final int MODE_ALL_COUNTRIES = 0;
    public static final int MODE_CAPITALS = 1;
    public static final int MODE_GLOBE_GRID = 2;

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
        // Россия и СНГ
        new City("Москва (Россия)", 55.7558, 37.6173),
        new City("Санкт-Петербург (Россия)", 59.9311, 30.3609),
        new City("Новосибирск (Россия)", 55.0084, 82.9357),
        new City("Екатеринбург (Россия)", 56.8389, 60.6057),
        new City("Казань (Россия)", 55.8304, 49.0661),
        new City("Владивосток (Россия)", 43.1198, 131.8869),
        new City("Сочи (Россия)", 43.6028, 39.7342),
        new City("Минск (Беларусь)", 53.9006, 27.5590),
        new City("Астана (Казахстан)", 51.1694, 71.4491),
        new City("Алматы (Казахстан)", 43.2220, 76.8512),
        new City("Ташкент (Узбекистан)", 41.2995, 69.2401),
        new City("Бишкек (Кыргызстан)", 42.8746, 74.5698),
        new City("Душанбе (Таджикистан)", 38.5598, 68.7870),
        new City("Ашхабад (Туркменистан)", 37.9601, 58.3261),
        new City("Баку (Азербайджан)", 40.4093, 49.8671),
        new City("Ереван (Армения)", 40.1792, 44.4991),
        new City("Тбилиси (Грузия)", 41.7151, 44.8271),
        new City("Кишинёв (Молдова)", 47.0105, 28.8638),

        // Европа
        new City("Лондон (Великобритания)", 51.5074, -0.1278),
        new City("Париж (Франция)", 48.8566, 2.3522),
        new City("Берлин (Германия)", 52.5200, 13.4050),
        new City("Рим (Италия)", 41.9028, 12.4964),
        new City("Мадрид (Испания)", 40.4168, -3.7038),
        new City("Амстердам (Нидерланды)", 52.3676, 4.9041),
        new City("Брюссель (Бельгия)", 50.8503, 4.3517),
        new City("Вена (Австрия)", 48.2082, 16.3738),
        new City("Берн (Швейцария)", 46.9480, 7.4474),
        new City("Варшава (Польша)", 52.2297, 21.0122),
        new City("Прага (Чехия)", 50.0755, 14.4378),
        new City("Будапешт (Венгрия)", 47.4979, 19.0402),
        new City("Стокгольм (Швеция)", 59.3293, 18.0686),
        new City("Осло (Норвегия)", 59.9139, 10.7522),
        new City("Хельсинки (Финляндия)", 60.1699, 24.9384),
        new City("Копенгаген (Дания)", 55.6761, 12.5683),
        new City("Афины (Греция)", 37.9838, 23.7275),
        new City("Лиссабон (Португалия)", 38.7223, -9.1393),
        new City("Дублин (Ирландия)", 53.3498, -6.2603),
        new City("Рейкьявик (Исландия)", 64.1466, -21.9426),

        // Азия
        new City("Токио (Япония)", 35.6762, 139.6503),
        new City("Сеул (Южная Корея)", 37.5665, 126.9780),
        new City("Пекин (Китай)", 39.9042, 116.4074),
        new City("Шанхай (Китай)", 31.2304, 121.4737),
        new City("Гонконг", 22.3193, 114.1694),
        new City("Сингапур", 1.3521, 103.8198),
        new City("Бангкок (Таиланд)", 13.7563, 100.5018),
        new City("Нью-Дели (Индия)", 28.6139, 77.2090),
        new City("Джакарта (Индонезия)", -6.2088, 106.8456),
        new City("Куала-Лумпур (Малайзия)", 3.1390, 101.6869),
        new City("Ханой (Вьетнам)", 21.0285, 105.8542),
        new City("Манила (Филиппины)", 14.5995, 120.9842),
        new City("Дубай (ОАЭ)", 25.2048, 55.2708),
        new City("Эр-Рияд (Саудовская Аравия)", 24.7136, 46.6753),
        new City("Тель-Авив (Израиль)", 32.0853, 34.7818),
        new City("Стамбул (Турция)", 41.0082, 28.9784),

        // Америка
        new City("Вашингтон (США)", 38.9072, -77.0369),
        new City("Нью-Йорк (США)", 40.7128, -74.0060),
        new City("Лос-Анджелес (США)", 34.0522, -118.2437),
        new City("Оттава (Канада)", 45.4215, -75.6972),
        new City("Мехико (Мексика)", 19.4326, -99.1332),
        new City("Бразилиа (Бразилия)", -15.7975, -47.8919),
        new City("Рио-де-Жанейро (Бразилия)", -22.9068, -43.1729),
        new City("Буэнос-Айрес (Аргентина)", -34.6037, -58.3816),
        new City("Сантьяго (Чили)", -33.4489, -70.6693),
        new City("Богота (Колумбия)", 4.7110, -74.0721),
        new City("Лима (Перу)", -12.0464, -77.0428),

        // Африка
        new City("Каир (Египет)", 30.0444, 31.2357),
        new City("Претория (ЮАР)", -25.7479, 28.2293),
        new City("Кейптаун (ЮАР)", -33.9249, 18.4241),
        new City("Найроби (Кения)", -1.2921, 36.8219),
        new City("Лагос (Нигерия)", 6.5244, 3.3792),
        new City("Касабланка (Марокко)", 33.5731, -7.5898),
        new City("Аддис-Абеба (Эфиопия)", 9.0320, 38.7575),

        // Океания
        new City("Канберра (Австралия)", -35.2809, 149.1300),
        new City("Сидней (Австралия)", -33.8688, 151.2093),
        new City("Веллингтон (Новая Зеландия)", -41.2865, 174.7762),
        new City("Гонолулу (Гавайи)", 21.3069, -157.8583),
        new City("Сува (Фиджи)", -18.1416, 178.4419),
        new City("Нукуалофа (Тонга)", -21.1394, -175.2018)
    };

    public static City[] loadAllCountries(Context context) {
        ArrayList<City> list = new ArrayList<City>();
        if (context != null) {
            try {
                InputStream is = context.getAssets().open("world_cities.dat");
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
            return WORLD_CAPITALS;
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
    private static volatile int currentMode = MODE_ALL_COUNTRIES;
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
            return "Не активно";
        }
        return "В пути: " + currentPlace + " (" + (currentIndex + 1) + "/" + totalCount + ")";
    }

    public static synchronized void start(final Context context, final int mode, final long delayMs, final Runnable onProgress) {
        stop();

        currentMode = mode;
        final City[] targets;
        if (mode == MODE_ALL_COUNTRIES) {
            targets = loadAllCountries(context);
        } else if (mode == MODE_GLOBE_GRID) {
            targets = generateGlobeGrid(15, 20);
        } else {
            targets = WORLD_CAPITALS;
        }

        totalCount = targets.length;
        currentIndex = 0;
        running = true;

        runnerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                City lastCity = null;
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

                        if (onProgress != null) {
                            try {
                                onProgress.run();
                            } catch (Throwable ignored) {}
                        }

                        if (delayMs > 0) {
                            Thread.sleep(delayMs);
                        }
                    }
                } catch (InterruptedException ignored) {
                } catch (Throwable e) {
                    currentPlace = "Ошибка: " + e.getClass().getSimpleName();
                } finally {
                    running = false;
                    if (lastCity != null && context != null) {
                        // Persist final position cleanly once
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
