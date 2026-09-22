# Справочник API и хуков BlinkMod

Данный документ содержит полное описание публичных и хуковых методов класса `StepHooks`, сетевых эндпоинтов Blink и структуры базы данных стран.

---

## 1. Спецификация методов `StepHooks`

Все методы класса `com.blinkmap.mod.StepHooks` являются статическими и потокобезопасными (`volatile` переменные состояния).

### 1.1. Геолокационный шлюз

#### `public static boolean isSpoofingActive()`
- **Сигнатура в Smali**: `()Z`
- **Назначение**: Проверяет, активен ли в данный момент режим подмены координат (дрифт, ручные координаты, мировой тур).
- **Возвращает**: `true`, если координаты должны браться из мода вместо системного GPS.

#### `public static com.blinkmap.geography.core.LatLng hookUserLatLng(LatLng latLng)`
#### `public static com.blinkmap.geography.core.LatLng hookUserLatLng(LatLng latLng, boolean enabled)`
- **Сигнатура в Smali**: `(Lcom/blinkmap/geography/core/LatLng;Z)Lcom/blinkmap/geography/core/LatLng;`
- **Назначение**: Перехватывает объект `LatLng`, используемый внутренним движком карты Blink при отрисовке маркера пользователя.
- **Возвращает**: Новый экземпляр `LatLng` с координатами спуфинга, либо исходный объект, если спуфинг не активен.

#### `public static void setCustomLocation(double lat, double lon)`
- **Назначение**: Устанавливает фиксированные координаты пользователя и сохраняет их в `SharedPreferences`.

#### `public static double getDriftLat(double original)` / `getDriftLon(double original)`
- **Назначение**: Возвращает текущую широту/долготу с учётом активного дрифта или фиксированных координат.

#### `public static float getDriftSpeed(float original)`
- **Назначение**: Возвращает текущую скорость движения. При активном WorldTour возвращает расчетную скорость автомобиля (~$13.8$ м/с) для корректного переключения иконки на карте.

#### `public static float getDriftBearing(float original)`
- **Назначение**: Возвращает угол поворота (курсовой азимут) для ориентации стрелки движения.

#### `public static void hookLocation(android.location.Location location)`
- **Назначение**: Модифицирует системный объект `android.location.Location` перед передачей его в обработчики Blink.

---

### 1.2. Шаги и телеметрия

#### `public static int outgoingSteps(int original)`
- **Сигнатура в Smali**: `(I)I`
- **Назначение**: Вызывается конструктором сетевого пакета шагов (`f4o.<init>`). Применяет множитель шагов (`network_steps_multiplier`) либо разовую отправку 100k шагов (`force_100k_once`).

#### `public static void arm100k()`
- **Назначение**: Устанавливает флаг `force_100k_once = true` для немедленной отправки пакета на 100 000 шагов.

#### `public static int displaySteps(int original)` / `displayKilometers(float original)`
- **Назначение**: Локальный визуальный множитель для отображения в интерфейсе приложения.

---

### 1.3. Батарея и пауза

#### `public static void setForcedCharge(int charge)`
- **Назначение**: Задаёт принудительное значение заряда аккумулятора ($-1$ — реальный, $0..100$ — фиксированный).

#### `public static void setHideCharge(boolean hide)`
- **Назначение**: Принудительно передаёт `charge` в сетевых запросах как `null`.

#### `public static void pauseFor(long durationMillis)`
- **Назначение**: Блокирует отправку сетевых запросов `POST /api/geo/android` на заданное время.

---

## 2. Сетевые эндпоинты Blink (Footprints & Geo)

| Метод | URL | Описание |
|---|---|---|
| `POST` | `/api/geo/android` | Основная телеметрия позиции (координаты, скорость, азимут, уровень заряда) |
| `POST` | `/api/steps/android` | Отправка батчей с шагами (`f4o` списки) |
| `GET` | `/api/footprints/countries` | Список посещенных стран пользователя |
| `GET` | `/api/footprints/stats/world` | Детальная статистика следов: страны, города, полигоны, флаги |
| `GET` | `/api/footprints/stats/top/world` | Глобальный мировой лидерборд по количеству посещенных стран |
| `GET` | `/api/footprints/stats/country` | Статистика по конкретной стране |

---

## 3. База данных стран (`countries_1.db`)

Вшитая база данных SQLite (`assets/countries_1.db`) содержит официальный перечень признаваемых приложением стран и территорий.

**Таблица `CountryEntity`**:
- `id` (INTEGER, PRIMARY KEY) — внутренний идентификатор страны.
- `country_code` (TEXT) — двухбуквенный ISO-код (например, `RU`, `US`, `FR`).
- `name_rus` (TEXT) — русское наименование.
- `name_en` (TEXT) — английское наименование.
- `geom` / `bounds` — полигональные границы для расчёта пересечения координат.

Всего в базе зафиксировано **232 официальные записи**. Максимально возможный результат в мировом лидерборде ограничен количеством уникальных сущностей в этой таблице.
