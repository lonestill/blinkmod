package com.blinkmap.mod;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;

public final class WorldTour {
    public static final int MODE_CITIES = 0;
    public static final int MODE_GLOBE_GRID = 1;

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

    public static final City[] WORLD_CITIES = new City[] {
        // Россия и СНГ
        new City("Москва", 55.7558, 37.6173),
        new City("Санкт-Петербург", 59.9311, 30.3609),
        new City("Новосибирск", 55.0084, 82.9357),
        new City("Екатеринбург", 56.8389, 60.6057),
        new City("Казань", 55.8304, 49.0661),
        new City("Нижний Новгород", 56.2965, 43.9361),
        new City("Самара", 53.1959, 50.1002),
        new City("Ростов-на-Дону", 47.2357, 39.7015),
        new City("Красноярск", 56.0153, 92.8932),
        new City("Иркутск", 52.2870, 104.3050),
        new City("Владивосток", 43.1198, 131.8869),
        new City("Хабаровск", 48.4827, 135.0838),
        new City("Сочи", 43.6028, 39.7342),
        new City("Калининград", 54.7104, 20.4522),
        new City("Мурманск", 68.9585, 33.0827),
        new City("Якутск", 62.0355, 129.6755),
        new City("Петропавловск-Камчатский", 53.0167, 158.6500),
        new City("Минск", 53.9006, 27.5590),
        new City("Киев", 50.4501, 30.5234),
        new City("Астана", 51.1694, 71.4491),
        new City("Алматы", 43.2220, 76.8512),
        new City("Ташкент", 41.2995, 69.2401),
        new City("Самарканд", 39.6542, 66.9597),
        new City("Бишкек", 42.8746, 74.5698),
        new City("Душанбе", 38.5598, 68.7870),
        new City("Ашхабад", 37.9601, 58.3261),
        new City("Баку", 40.4093, 49.8671),
        new City("Ереван", 40.1792, 44.4991),
        new City("Тбилиси", 41.7151, 44.8271),
        new City("Кишинёв", 47.0105, 28.8638),

        // Европа
        new City("Лондон", 51.5074, -0.1278),
        new City("Манчестер", 53.4808, -2.2426),
        new City("Эдинбург", 55.9533, -3.1883),
        new City("Дублин", 53.3498, -6.2603),
        new City("Париж", 48.8566, 2.3522),
        new City("Лион", 45.7640, 4.8357),
        new City("Марсель", 43.2965, 5.3698),
        new City("Берлин", 52.5200, 13.4050),
        new City("Мюнхен", 48.1351, 11.5820),
        new City("Франкфурт", 50.1109, 8.6821),
        new City("Гамбург", 53.5511, 9.9937),
        new City("Рим", 41.9028, 12.4964),
        new City("Милан", 45.4642, 9.1900),
        new City("Неаполь", 40.8518, 14.2681),
        new City("Мадрид", 40.4168, -3.7038),
        new City("Барселона", 41.3851, 2.1734),
        new City("Валенсия", 39.4699, -0.3763),
        new City("Лиссабон", 38.7223, -9.1393),
        new City("Порту", 41.1579, -8.6291),
        new City("Амстердам", 52.3676, 4.9041),
        new City("Роттердам", 51.9244, 4.4777),
        new City("Брюссель", 50.8503, 4.3517),
        new City("Вена", 48.2082, 16.3738),
        new City("Берн", 46.9480, 7.4474),
        new City("Цюрих", 47.3769, 8.5417),
        new City("Женева", 46.2044, 6.1432),
        new City("Прага", 50.0755, 14.4378),
        new City("Варшава", 52.2297, 21.0122),
        new City("Краков", 50.0647, 19.9450),
        new City("Будапешт", 47.4979, 19.0402),
        new City("Братислава", 48.1486, 17.1077),
        new City("Бухарест", 44.4268, 26.1025),
        new City("София", 42.6977, 23.3219),
        new City("Белград", 44.7866, 20.4489),
        new City("Загреб", 45.8150, 15.9819),
        new City("Любляна", 46.0569, 14.5058),
        new City("Сараево", 43.8563, 18.4131),
        new City("Скопье", 41.9981, 21.4254),
        new City("Тирана", 41.3275, 19.8187),
        new City("Афины", 37.9838, 23.7275),
        new City("Стокгольм", 59.3293, 18.0686),
        new City("Гётеборг", 57.7089, 11.9746),
        new City("Осло", 59.9139, 10.7522),
        new City("Берген", 60.3913, 5.3221),
        new City("Хельсинки", 60.1699, 24.9384),
        new City("Копенгаген", 55.6761, 12.5683),
        new City("Рейкьявик", 64.1466, -21.9426),
        new City("Рига", 56.9496, 24.1052),
        new City("Вильнюс", 54.6872, 25.2797),
        new City("Таллин", 59.4370, 24.7535),
        new City("Стамбул", 41.0082, 28.9784),
        new City("Анкара", 39.9334, 32.8597),
        new City("Никосия", 35.1856, 33.3823),

        // Азия
        new City("Токио", 35.6762, 139.6503),
        new City("Осака", 34.6937, 135.5023),
        new City("Киото", 35.0116, 135.7681),
        new City("Саппоро", 43.0618, 141.3545),
        new City("Сеул", 37.5665, 126.9780),
        new City("Пусан", 35.1796, 129.0756),
        new City("Пекин", 39.9042, 116.4074),
        new City("Шанхай", 31.2304, 121.4737),
        new City("Гуанчжоу", 23.1291, 113.2644),
        new City("Шэньчжэнь", 22.5431, 114.0579),
        new City("Гонконг", 22.3193, 114.1694),
        new City("Тайбэй", 25.0330, 121.5654),
        new City("Сингапур", 1.3521, 103.8198),
        new City("Бангкок", 13.7563, 100.5018),
        new City("Пхукет", 7.8804, 98.3923),
        new City("Куала-Лумпур", 3.1390, 101.6969),
        new City("Джакарта", -6.2088, 106.8456),
        new City("Бали", -8.4095, 115.1889),
        new City("Манила", 14.5995, 120.9842),
        new City("Ханой", 21.0285, 105.8542),
        new City("Хошимин", 10.8231, 106.6297),
        new City("Пномпень", 11.5564, 104.9282),
        new City("Вьентьян", 17.9757, 102.6331),
        new City("Янгон", 16.8661, 96.1951),
        new City("Нью-Дели", 28.6139, 77.2090),
        new City("Мумбаи", 19.0760, 72.8777),
        new City("Бангалор", 12.9716, 77.5946),
        new City("Калькутта", 22.5726, 88.3639),
        new City("Дакка", 23.8103, 90.4125),
        new City("Исламабад", 33.6844, 73.0479),
        new City("Карачи", 24.8607, 67.0011),
        new City("Коломбо", 6.9271, 79.8612),
        new City("Катманду", 27.7172, 85.3240),
        new City("Улан-Батор", 47.8864, 106.9057),
        new City("Дубай", 25.2048, 55.2708),
        new City("Абу-Даби", 24.4539, 54.3773),
        new City("Доха", 25.2854, 51.5310),
        new City("Эр-Рияд", 24.7136, 46.6753),
        new City("Джидда", 21.4858, 39.1925),
        new City("Маскат", 23.5880, 58.3829),
        new City("Эль-Кувейт", 29.3759, 47.9774),
        new City("Манама", 26.2285, 50.5860),
        new City("Амман", 31.9454, 35.9284),
        new City("Бейрут", 33.8938, 35.5018),
        new City("Тель-Авив", 32.0853, 34.7818),
        new City("Иерусалим", 31.7683, 35.2137),
        new City("Тегеран", 35.6892, 51.3890),
        new City("Багдад", 33.3152, 44.3661),

        // Северная Америка
        new City("Вашингтон", 38.9072, -77.0369),
        new City("Нью-Йорк", 40.7128, -74.0060),
        new City("Лос-Анджелес", 34.0522, -118.2437),
        new City("Чикаго", 41.8781, -87.6298),
        new City("Хьюстон", 29.7604, -95.3698),
        new City("Майами", 25.7617, -80.1918),
        new City("Сан-Франциско", 37.7749, -122.4194),
        new City("Сиэтл", 47.6062, -122.3321),
        new City("Бостон", 42.3601, -71.0589),
        new City("Лас-Вегас", 36.1699, -115.1398),
        new City("Атланта", 33.7490, -84.3880),
        new City("Денвер", 39.7392, -104.9903),
        new City("Торонто", 43.6532, -79.3832),
        new City("Монреаль", 45.5017, -73.5673),
        new City("Ванкувер", 49.2827, -123.1207),
        new City("Оттава", 45.4215, -75.6972),
        new City("Калгари", 51.0447, -114.0719),
        new City("Мехико", 19.4326, -99.1332),
        new City("Гвадалахара", 20.6597, -103.3496),
        new City("Канкун", 21.1619, -86.8515),
        new City("Гавана", 23.1136, -82.3666),
        new City("Санто-Доминго", 18.4861, -69.9312),
        new City("Сан-Хуан", 18.4655, -66.1057),
        new City("Гватемала", 14.6349, -90.5069),
        new City("Сан-Хосе (КР)", 9.9281, -84.0907),
        new City("Панама", 8.9824, -79.5199),
        new City("Кингстон", 17.9714, -76.7936),

        // Южная Америка
        new City("Бразилиа", -15.8267, -47.9218),
        new City("Сан-Паулу", -23.5505, -46.6333),
        new City("Рио-де-Жанейро", -22.9068, -43.1729),
        new City("Сальвадор", -12.9777, -38.5016),
        new City("Буэнос-Айрес", -34.6037, -58.3816),
        new City("Кордова", -31.4201, -64.1888),
        new City("Сантьяго", -33.4489, -70.6693),
        new City("Лима", -12.0464, -77.0428),
        new City("Куско", -13.5319, -71.9675),
        new City("Богота", 4.7110, -74.0721),
        new City("Медельин", 6.2442, -75.5812),
        new City("Картахена", 10.3910, -75.4794),
        new City("Каракас", 10.4806, -66.9036),
        new City("Кито", -0.1807, -78.4678),
        new City("Гуаякиль", -2.1894, -79.8891),
        new City("Ла-Пас", -16.4897, -68.1193),
        new City("Асунсьон", -25.2637, -57.5759),
        new City("Монтевидео", -34.9011, -56.1645),
        new City("Джорджтаун", 6.8013, -58.1551),
        new City("Парамарибо", 5.8520, -55.2038),

        // Африка
        new City("Каир", 30.0444, 31.2357),
        new City("Александрия", 31.2001, 29.9187),
        new City("Кейптаун", -33.9249, 18.4241),
        new City("Претория", -25.7479, 28.2293),
        new City("Йоханнесбург", -26.2041, 28.0473),
        new City("Дурбан", -29.8587, 31.0218),
        new City("Найроби", -1.2921, 36.8219),
        new City("Момбаса", -4.0435, 39.6682),
        new City("Аддис-Абеба", 9.0320, 38.7575),
        new City("Лагос", 6.5244, 3.3792),
        new City("Абуджа", 9.0765, 7.3986),
        new City("Аккра", 5.6037, -0.1870),
        new City("Дакар", 14.7167, -17.4677),
        new City("Касабланка", 33.5731, -7.5898),
        new City("Марракеш", 31.6295, -7.9811),
        new City("Алжир", 36.7538, 3.0588),
        new City("Тунис", 36.8065, 10.1815),
        new City("Триполи", 32.8872, 13.1913),
        new City("Хартум", 15.5007, 32.5599),
        new City("Киншаса", -4.4419, 15.2663),
        new City("Луанда", -8.8390, 13.2894),
        new City("Дар-эс-Салам", -6.7924, 39.2083),
        new City("Кампала", 0.3476, 32.5825),
        new City("Кигали", -1.9706, 30.1044),
        new City("Антананариву", -18.8792, 47.5079),
        new City("Виндхук", -22.5609, 17.0658),
        new City("Габороне", -24.6282, 25.9231),
        new City("Мапуту", -25.9692, 32.5732),

        // Австралия и Океания
        new City("Сидней", -33.8688, 151.2093),
        new City("Мельбурн", -37.8136, 144.9631),
        new City("Брисбен", -27.4698, 153.0251),
        new City("Перт", -31.9505, 115.8605),
        new City("Аделаида", -34.9285, 138.6007),
        new City("Канберра", -35.2809, 149.1300),
        new City("Окленд", -36.8485, 174.7633),
        new City("Веллингтон", -41.2865, 174.7762),
        new City("Крайстчерч", -43.5321, 172.6362),
        new City("Порт-Морсби", -9.4438, 147.1803),
        new City("Сува (Фиджи)", -18.1416, 178.4419),
        new City("Гонолулу", 21.3069, -157.8583),
        new City("Папеэте (Таити)", -17.5516, -149.5692),
        new City("Апиа", -13.8333, -171.7667),
        new City("Нукуалофа", -21.1394, -175.2018)
    };

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
    private static volatile int currentMode = MODE_CITIES;
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
        if (mode == MODE_GLOBE_GRID) {
            targets = generateGlobeGrid(15, 20); // 10 x 18 = 180 global points covering land & oceans
        } else {
            targets = WORLD_CITIES;
        }

        totalCount = targets.length;
        currentIndex = 0;
        running = true;

        runnerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < targets.length && running; i++) {
                        City city = targets[i];
                        currentIndex = i;
                        currentPlace = city.name;

                        StepHooks.setCustomLocation(city.lat, city.lon);
                        StepHooks.dispatchSyntheticLocation();

                        SharedPreferences p = context == null ? null :
                                context.getSharedPreferences("blinkmod_settings", Context.MODE_PRIVATE);
                        if (p != null) {
                            p.edit()
                                .putString("world_tour_status", getStatusText())
                                .putInt("world_tour_index", i)
                                .putInt("world_tour_total", targets.length)
                                .putString("world_tour_place", city.name)
                                .apply();
                        }

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
                    SharedPreferences p = context == null ? null :
                            context.getSharedPreferences("blinkmod_settings", Context.MODE_PRIVATE);
                    if (p != null) {
                        p.edit().putString("world_tour_status", "Ошибка: " + e.getClass().getSimpleName()).apply();
                    }
                } finally {
                    running = false;
                    SharedPreferences p = context == null ? null :
                            context.getSharedPreferences("blinkmod_settings", Context.MODE_PRIVATE);
                    if (p != null) {
                        p.edit().putString("world_tour_status", getStatusText()).apply();
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
