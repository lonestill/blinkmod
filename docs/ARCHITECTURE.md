# Архитектура и реверс-инжиниринг BlinkMod

Данный документ описывает внутреннее устройство, точки перехвата байткода (Smali hooks), пайплайн геолокации и сетевой стек модификации **BlinkMod**.

---

## 1. Общая схема системы

```
                ┌──────────────────────────────────────────────┐
                │             Android OS / Sensors             │
                └──────────────────────┬───────────────────────┘
                                       │ Real GPS / Step Sensor
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Blink Application Core                            │
│                                                                             │
│  [App.smali] ──> StepHooks.init(context)                                    │
│  [f4o.smali] ──> StepHooks.outgoingSteps(steps)                             │
│  [a15 / ef0 / syh.smali] ──> StepHooks.hookUserLatLng(latLng, enabled)      │
│  [UserPointerController] ──> StepHooks.isSpoofingActive() / getDriftLat()   │
│  [HttpLogger.smali] ────┬───> VisitedCountriesManager (Token & Footprints)  │
└─────────────────────────┼───────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          BlinkMod Injected Subsystem                        │
│                                                                             │
│   ┌────────────────────┐   ┌─────────────────────┐   ┌───────────────────┐  │
│   │     StepHooks      │   │      WorldTour      │   │ VisitedCountries  │  │
│   │ Runtime Hooks &    │<──│ Dynamic Auto-Tour & │<──│   Sync & Cache    │  │
│   │ State Management   │   │ Kinematics Physics  │   │ Manager (API/DB)  │  │
│   └────────────────────┘   └─────────────────────┘   └───────────────────┘  │
│              ▲                        ▲                        ▲            │
│              └────────────────────────┼────────────────────────┘            │
│                                       │                                     │
│                            ┌─────────────────────┐                          │
│                            │    WorldTourUi      │                          │
│                            │  (Settings Activity)│                          │
│                            └─────────────────────┘                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Точки инъекции байткода (Smali Hooks)

В модифицированном клиенте оригинальные Smali-классы Blink пропатчены вызовами статических методов из пакета `com.blinkmap.mod`:

| Оригинальный Smali-класс | Метод перехвата | Вызываемый метод мода | Назначение |
|---|---|---|---|
| `smali/com/blinkmap/App.smali` | `onCreate()` | `StepHooks.init(Context)` | Инициализация настроек, инспектора и бейджей при старте процесса |
| `smali_classes2/ef0.smali` | Обработка позиции | `StepHooks.hookUserLatLng(LatLng, boolean)` | Подмена координат текущего пользователя на карте |
| `smali_classes3/a15.smali` | Рендеринг маркера | `StepHooks.hookUserLatLng(LatLng, boolean)` | Перехват позиции аватара |
| `smali_classes3/syh.smali` | Карта / Геометрия | `StepHooks.hookUserLatLng(LatLng, boolean)` | Корректировка отображения пути и полигонов |
| `smali/u55.smali` | Обработка геолокации | `StepHooks.hookUserLatLng(LatLng, boolean)` | Локальный расчёт дистанций |
| `smali_classes2/fc9.smali` | Координатор карты | `StepHooks.isSpoofingActive()`, `getDriftLat()`, `getDriftLon()` | Подмена координат карты при активном спуфинге |
| `smali_classes2/qbg.smali` | Локатор друзей | `StepHooks.isSpoofingActive()`, `getDriftLat()`, `getDriftLon()` | Относительное позиционирование |
| `smali_classes6/.../UserPointerController.smali` | Диспетчер стрелки | `StepHooks.isSpoofingActive()`, `getDriftLat()`, `getDriftLon()` | Подмена стрелки направления и положения пользователя |
| `smali_classes3/f4o.smali` | `<init>(IJLjava/lang/String;)` | `StepHooks.outgoingSteps(int)` | Подмена отправляемого количества шагов в пакете телеметрии |
| `smali_classes3/uhi.smali` | Периодический синк шагов | `StepHooks.outgoingSteps(int)` | Фоновая синхронизация шагов с педометра |

> [!CAUTION]
> Методы `isSpoofingActive()Z` и `hookUserLatLng(...)` являются обязательными интерфейсными контрактами. При перекомпиляции оверлея удаление этих методов приводит к `NoSuchMethodError` и зависанию приложения на сплэш-скрине.

---

## 3. Пайплайн геолокации и кинематика транспорта

### 3.1. Механизм регистрации следов (Footprints) бэкендом Blink
Серверный процессинг Blink валидирует геолокационные пакеты перед зачислением страны в профиль пользователя (`/api/footprints/stats/world`):
1. **Телепортация без задержки игнорируется**: Одиночный запрос из новой страны с интервалом менее 2-3 секунд отсекается серверным фильтром антифрода (GPS jitter / anti-teleport filter).
2. **Время нахождения (Dwell Time)**: Для гарантированного зачёта сервер требует серию геолокационных отчётов на протяжении **5–10 секунд** из одной географической зоны.
3. **Режим транспорта (Vehicle Detection)**: 
   - Скорость $< 2.0$ м/с ($< 7$ км/ч) интерпретируется как пешеходное движение.
   - Скорость $> 7.0$ м/с ($> 25$ км/ч) активирует на бэкенде и на карте состояние движения на автомобиле (переключает статус иконки аватара).
   - В `WorldTour.java` реализован генератор движения со скоростью **49–54 км/ч** ($13.6–15.0$ м/с) с интервалом отправки 1 раз в секунду.

### 3.2. Математическая модель перемещения (WGS84 Arc Kinematics)
На каждом шаге $i \in [0, N]$ рассчитывается смещение по азимуту с плавным поворотом дуги:
$$\Delta \text{dist} = v_{\text{step}} \times \Delta t$$
$$\Delta \text{lat} = \frac{\Delta \text{dist} \times \cos(\theta)}{111139.0}$$
$$\Delta \text{lon} = \frac{\Delta \text{dist} \times \sin(\theta)}{111139.0 \times \cos(\text{lat})}$$
$$\theta_{i+1} = (\theta_i + 3.5^\circ) \pmod{360^\circ}$$

Где:
- $v_{\text{step}} \approx 13.8\text{ м/с}$ (скорость авто).
- $\theta$ — курсовой угол (bearing).
- $\Delta t \approx 1.0\text{ с}$ — интервал шага.

---

## 4. Архитектура синхронизации шагов и аномалия 100 158 шагов

### 4.1. Формат сетевого пакета шагов
Пакет телеметрии шагов инкапсулируется в объект `f4o`:
```smali
.method public constructor <init>(IJLjava/lang/String;)V
    invoke-static {p1}, Lcom/blinkmap/mod/StepHooks;->outgoingSteps(I)I
    move-result p1
    iput p1, p0, Lf4o;->a:I           # steps count
    iput-wide p2, p0, Lf4o;->c:J       # bucket timestamp (epoch millis)
    iput-object p4, p0, Lf4o;->d:Ljava/lang/String; # source identifier
```
Сериализованные объекты упаковываются в список `c4o` с типом `"sensor"` и отправляются на эндпоинт `/api/steps/android`.

### 4.2. Механизм превышения 100 000 шагов
1. Клиентская кнопка «100 000 шагов» через `WorkBridge` отправляет батч, содержащий ровно $100\,000$ шагов (`0x186a0`) для временного среза $T_1$. Серверный валидатор проверяет ограничение $\text{steps} \le 100\,000$ для **одного батча** и успешно принимает пакет.
2. В течение дня фоновый воркер `uhi.smali` периодически считывает аппаратный шагомер устройства и отправляет дельту реальных шагов пользователя (например, $158$ шагов) для среза $T_2$.
3. Бэкенд Blink выполняет суточное агрегирование:
   $$\text{Total Daily Steps} = \sum_{T \in \text{Today}} \text{steps}_T = 100\,000 + 158 = 100\,158$$
4. Поскольку агрегатор не обрезает сумму, а валидирует только входящие срезы, итоговое значение в профиле и лидерборде превышает жесткий лимит в 100k.
