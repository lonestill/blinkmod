# BLINKMOD DEV PROMPT (MODULAR)

Этот файл можно подключать как дополнительный промпт или использовать в контексте IDE/CLI.

## Спецификация ключевых компонентов

| Компонент | Файл | Ответственность |
|---|---|---|
| `ModSettingsActivity` | `com/blinkmap/mod/ModSettingsActivity.java` | Нативный UI меню мода: переключатели спуфинга, шагов, туров, скрытия батареи |
| `BlinkModButton` | `com/blinkmap/mod/BlinkModButton.java` | Плавающая кнопка оверлея поверх штатного экрана Blink |
| `FriendFollower` | `com/blinkmap/mod/FriendFollower.java` | Сталкер-мод: следование за маркером контакта, орбита, M3-пикер |
| `WorldTour` | `com/blinkmap/mod/WorldTour.java` | Кинематика автомобиля (~50 км/ч, плавный bearing) и авто-проезд по 232 странам |
| `VisitedCountriesManager` | `com/blinkmap/mod/VisitedCountriesManager.java` | Сопоставление локальной SQLite `countries_1.db` и эндпоинта `/api/footprints/countries` |
| `StepHooks` | `com/blinkmap/mod/StepHooks.java` | Точка перехвата шагов (`outgoingSteps`), координат (`hookUserLatLng`), троттлинга geo |
| `TokenLoginOverlay` | `com/blinkmap/mod/TokenLoginOverlay.java` | Вход по токену в обход SMS и Google Play Services |
| `MapPicker` | `com/blinkmap/mod/MapPicker.java` | Интерактивный выбор точки прямо по тапу на карте |
| `MarkerStore` | `com/blinkmap/mod/MarkerStore.java` | Чтение закэшированных маркеров друзей из базы приложения |

## Паттерн добавления новой фичи
1. Создать/модифицировать класс в `blinkmod_repo/src/main/java/com/blinkmap/mod/`.
2. Если нужен перехват в методах Blink — найти метод в `blinkmod_v1.1_src/smali*` и добавить однострочный `invoke-static {pX} Lcom/blinkmap/mod/StepHooks;->myHook(...)V`.
3. Запустить `./scripts/build-overlay.sh dev` для проверки компиляции и синхронизации.
4. Собрать APK через `./build_and_deploy.sh skip dev` в `blink_mod_menu`.
