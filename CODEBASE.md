# BlinkMod — Кодовая база проекта

Технический справочник и архитектурный индекс клиентской модификации BlinkMod (`app.blinkmod` / `com.blinkmap`).

---

## 1. Обзор проекта

- **Цель:** Нативная модификация Android-клиента картографической соцсети Blink (Zenly-подобный трекер друзей).
- **Пакет приложения:** `app.blinkmod` (в оригинале `com.blinkmap`).
- **Текущий релиз:** v2.0 (артефакт сборки: `~/Desktop/BlinkMod_v2.0.apk`).
- **Стек:** Java 8 / Android SDK 32–35, Smali байткод, Apktool, D8, Apksigner.
- **Главный репозиторий:** `~/Desktop/AI_Agent/blinkmod_repo` (чистые исходники оверлея, документация, сборка).
- **Сборочный стенд декомпила:** `~/Desktop/AI_Agent/blink_from_gitty/blink_mod_menu` (декомпилированный APK, smali-хуки, ключи подписи).

---

## 2. Структура репозитория (`blinkmod_repo`)

```text
blinkmod_repo/
├── CODEBASE.md                         # Этот документ (архитектурный индекс)
├── README.md                           # Общее описание возможностей и ссылки на доки
├── CHANGELOG.md                        # История версий
├── SECURITY.md                         # Политика безопасности
├── badges.json                         # Удаленная база бейджей (account_id -> badge)
├── manifest/
│   └── AndroidManifest.xml             # Декларации Activity и WidgetProvider
├── scripts/
│   └── build-overlay.sh                # Главный скрипт компиляции Java -> DEX -> Smali
├── docs/
│   ├── ARCHITECTURE.md                 # Smali-хуки, пайплайн локации, кинематика
│   ├── API_AND_HOOKS_REFERENCE.md      # Спецификация StepHooks, эндпоинты, SQLite
│   ├── FLAVORS_AND_BUILDS.md           # Разделение Public vs Dev, матрица фичей
│   └── BUILDING.md                     # Окружение, зависимости и сборка
└── src/main/
    ├── java/                           # Исходный код мода на Java
    │   ├── com/blinkmap/mod/           # Ядро функционала BlinkMod
    │   ├── com/blinkmap/geography/core/# Стабы географических типов (LatLng)
    │   └── com/maps/sdk/               # Стабы MapView и CameraController
    ├── smali/com/blinkmap/mod/
    │   └── WorkBridge.smali            # Smali-хелпер рефлексии и контекста
    ├── res/                            # Android-ресурсы оверлея
    │   ├── layout/                     # Разметка виджетов (friend, nearby, compact)
    │   ├── xml/                        # Метаданные AppWidgetProvider
    │   └── drawable/                   # Фоны и формы виджетов
    └── assets/                         # Бинарные базы геоданных
        ├── world_countries.dat         # База 232 стран для автотура
        ├── missing_countries.dat       # Дефолтный список недополученных стран
        ├── top1_boost.dat              # Точки маршрута снайпера Топ-1
        └── world_cities.dat            # Координаты городов мира
```

---

## 3. Модули и классы Java (`src/main/java/com/blinkmap/mod/`)

### Ядро и управление состоянием
- [`ModConfig.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/ModConfig.java) — Конфигурация сборки (`IS_DEV_BUILD`), константы версий, GitHub URL бейджей.
- [`StepHooks.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/StepHooks.java) — Центральный диспетчер байткод-хуков. Хранит глобальное состояние (спуфинг, шаги, заряд, троттлинг сетевых запросов).
- [`ModSettingsActivity.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/ModSettingsActivity.java) — Главное меню настроек мода. Полноценный интерфейс на Android Views: переключатели спуфинга, слайдер шагов, выбор друзей, управление турами, очистка кэша, Dev Mode (5 нажатий).
- [`BlinkModButton.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/BlinkModButton.java) — Плавающая оверлейная кнопка поверх главного экрана Blink для быстрого вызова настроек.

### Геолокация, Сталкер и Спуфинг
- [`FriendFollower.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/FriendFollower.java) — Сталкер-мод. Отслеживает координаты выбранного контакта в рантайме, позволяет двигаться точно за ним либо кружить по орбите с настраиваемым радиусом и угловой скоростью.
- [`MapPicker.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/MapPicker.java) — Интерактивный режим выбора целевой координаты прямо тапом по штатной карте Blink.
- [`FakeLocationPump.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/FakeLocationPump.java) — Фоновый генератор mock-координат для обхода внутренних проверок интервалов.
- [`MarkerStore.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/MarkerStore.java) — Локальный кеш маркеров друзей, считываемый напрямую из SQLite/репозиториев приложения.

### Мировое турне и Лидерборд (Top-1 Sniper)
- [`WorldTour.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/WorldTour.java) — Движок авто-путешествий. Реализует физику автомобиля (~50 км/ч, плавный разворот курса bearing, задержки dwell time 6–10 сек на контрольных точках) для гарантированного зачета страны бэкендом Blink без триггера антифрода.
- [`WorldTourUi.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/WorldTourUi.java) — Диалоговый интерфейс запуска туров: одиночный проезд, рейд по недостающим странам, взятие Топ-1.
- [`VisitedCountriesManager.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/VisitedCountriesManager.java) — Синхронизация и сопоставление посещенных стран между локальной базой `countries_1.db` и эндпоинтом `/api/footprints/countries`.

### Авторизация и Токены
- [`TokenLoginOverlay.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/TokenLoginOverlay.java) — Модальное окно входа по Bearer токену без SMS и Google Services. Валидирует токен через `/api/account/profile`.
- [`TokenAuthBridge.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/TokenAuthBridge.java) — Мост записи сессии в SharedPreferences и защищенное хранилище Blink.
- [`TokenExportBridge.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/TokenExportBridge.java) / [`TokenExportUi.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/TokenExportUi.java) — Извлечение текущего токена авторизации в буфер обмена для использования в скриптах.

### Виджеты рабочего стола
- [`FriendWidgetProvider.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/FriendWidgetProvider.java) — Виджет выбранного друга: заряд, скорость, расстояние, статус онлайна.
- [`CompactFriendWidgetProvider.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/CompactFriendWidgetProvider.java) — Компактная версия виджета 1x1 / 2x1.
- [`NearbyRadarWidgetProvider.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/NearbyRadarWidgetProvider.java) — Радар ближайших друзей в радиусе.
- [`FriendWidgetConfigActivity.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/FriendWidgetConfigActivity.java) — Активность выбора друга при добавлении виджета на экран.

### UI-компоненты и утилиты
- [`AvatarLoader.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/AvatarLoader.java) — Асинхронный загрузчик и кэшер круглых аватарок друзей из CDN Blink.
- [`BadgeManager.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/BadgeManager.java) — Синхронизация кастомных бейджей пользователей с GitHub.
- [`GmsCoreHelper.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/GmsCoreHelper.java) — Хелпер совместимости с microG / ReVanced GmsCore.
- [`Md3ProgressBar.java`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/src/main/java/com/blinkmap/mod/Md3ProgressBar.java) — Кастомный Material You прогресс-бар для туров.

---

## 4. Карта Smali-хуков в ядре Blink

В процессе сборки оверлей инжектируется в декомпилированный APK `blinkmod_v1.1_src`. Точки входа:

| Оригинальный класс | Точка перехвата | Инжектированный вызов | Назначение |
|---|---|---|---|
| `smali/com/blinkmap/App.smali` | `onCreate()` | `StepHooks.init(Context)` | Инициализация мода и настроек при старте процесса |
| `smali/com/blinkmap/BlinkActivity.smali` | `onResume()` | `BlinkModButton.inject(Activity)` | Добавление плавающей кнопки мода на экран |
| `smali/com/blinkmap/BlinkActivity.smali` | `onResume()` | `TokenAuthBridge.check(Activity)` | Проверка наличия сессии и показ диалога логина |
| `smali/u55.smali` | Обработка геолокации | `StepHooks.hookUserLatLng(LatLng, Z)` | Подмена координат при локальных расчетах |
| `smali_classes2/ef0.smali` | Позиционирование карты | `StepHooks.hookUserLatLng(LatLng, Z)` | Смещение маркера пользователя на карте |
| `smali_classes3/a15.smali` | Рендеринг аватара | `StepHooks.hookUserLatLng(LatLng, Z)` | Отображение маркера в точке спуфинга |
| `smali_classes2/fc9.smali` | Координатор камеры | `StepHooks.isSpoofingActive()`, `getDriftLat()` | Центрирование камеры на спуф-точке |
| `smali_classes2/qbg.smali` | Вектор к друзьям | `StepHooks.getDriftLat()`, `getDriftLon()` | Расчет дистанций от спуф-точки |
| `smali_classes3/f4o.smali` | `<init>(IJLjava/lang/String;)` | `StepHooks.outgoingSteps(int)` | Подмена шагов в сетевом пакете |
| `smali_classes3/jre.smali` | Отправка локации на бэкенд | `StepHooks.pauseLocation`, `pauseUntil` | Троттлинг и глушение `POST /api/geo/android` |
| `smali_classes3/e01.smali` | Обработчик Location | `StepHooks.hookLocation(Location)` | Модификация системного Android Location |
| `smali/lal.smali` | HTTP-перехватчик | `HttpLogger.logRequest/logResponse` | Перехват сетевых заголовков и Bearer токена |

---

## 5. Пайплайн компиляции и сборки

Сборка полностью локализована на macOS без зависимости от удаленных сред.

### Шаг 1: Компиляция Java-оверлея в DEX и синхронизация Smali
Скрипт: [`scripts/build-overlay.sh`](file:///Users/a123/Desktop/AI_Agent/blinkmod_repo/scripts/build-overlay.sh)
```bash
cd /Users/a123/Desktop/AI_Agent/blinkmod_repo
./scripts/build-overlay.sh dev     # или public
```
1. Компилирует все `.java` файлы из `src/main/java` через `javac` (release 8) с подключением `android.jar`.
2. Транслирует классы в DEX через `d8`.
3. Собирает `WorkBridge.smali` и объединяет его с `java-dex` в единый `blinkmod-overlay.dex`.
4. Декомпилирует результирующий DEX через Apktool и копирует свежие смали в `blink_mod_menu/blinkmod_v1.1_src/smali_classes9/com/blinkmap/mod/`.
5. Копирует бинарные ассеты (`world_countries.dat`, etc.) в папку ассетов APK.

### Шаг 2: Сборка, выравнивание, подпись и установка APK
Скрипт: [`blink_mod_menu/build_and_deploy.sh`](file:///Users/a123/Desktop/AI_Agent/blink_from_gitty/blink_mod_menu/build_and_deploy.sh)
```bash
cd /Users/a123/Desktop/AI_Agent/blink_from_gitty/blink_mod_menu
./build_and_deploy.sh [SERIAL] [FLAVOR]
# Пример: ./build_and_deploy.sh skip dev
```
1. Автоматически запускает `build-overlay.sh` для актуализации кода.
2. Собирает полный APK через `apktool b`.
3. Выравнивает бинарник через `zipalign -p 4`.
4. Подписывает ключом `keys/blinkmod-gmscore.jks` через `apksigner`.
5. Кладёт готовый релиз в `/Users/a123/Desktop/BlinkMod_v2.0.apk`.
6. Если подключен телефон по ADB — ставит APK и перезапускает приложение.

---

## 6. Варианты сборки (Flavors)

| Функция | Public Release | Dev Build |
|---|:---:|:---:|
| Интерактивный MapPicker | ✅ | ✅ |
| Спуфинг координат и дрифт | ✅ | ✅ |
| Подмена шагов и множитель | ✅ | ✅ |
| Скрытие заряда батареи | ✅ | ✅ |
| Виджеты рабочего стола | ✅ | ✅ |
| Вход и экспорт по токену | ✅ | ✅ |
| Friend Follower (Сталкер) | ❌ (Скрыт) | ✅ |
| World Tour (Авто-проезд) | Безопасный | Агрессивный |
| Снайпер ТОП-1 Мира | ❌ (Скрыт) | ✅ |
| Пасхалка активации Dev Mode | 5 тапов по версии в меню | Включен сразу |
