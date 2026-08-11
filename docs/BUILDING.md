# Сборка исходного оверлея

## Что включено

Репозиторий собирает `blinkmod-overlay.dex` из Java-классов BlinkMod и `WorkBridge.smali`. Это исходная часть мода, а не копия проприетарного приложения Blink.

## Зависимости

- JDK 17+
- ECJ
- Android SDK `android.jar`
- Android Build Tools с `d8`
- apktool 3.0.3+

## Команда

```bash
ANDROID_JAR="$ANDROID_HOME/platforms/android-35/android.jar" \
ECJ="$HOME/tools/ecj.jar" \
D8="$ANDROID_HOME/build-tools/35.0.0/d8" \
APKTOOL="$HOME/tools/apktool.jar" \
./scripts/build-overlay.sh
```

Результат появится в `build/blinkmod-overlay.dex`.

## Интеграция

Для полной сборки необходима собственная локальная копия Blink 1.32, декодированная apktool. Нужно:

1. Добавить ресурсные оверлеи из `src/main/res`.
2. Добавить компоненты из `manifest/blinkmod-components.xml` внутрь `<application>`.
3. Подключить вызовы `BlinkModButton` и `StepHooks` в соответствующих точках приложения.
4. Добавить собранный DEX, сменить package name для отдельной установки и объединить ARM64/density split-ресурсы.
5. Собрать, zipalign и подписать собственным ключом.

Конкретные смещения и имена обфусцированных классов зависят от версии Blink. Оригинальный APK, декомпилированные классы, аккаунты, токены и ключи подписи в репозиторий не включаются.
