# HealthBridge

HealthBridge is a tiny Android bridge that writes nutrition data into **Health Connect** from a deep link.

The Android app registers URIs like:

```text
healthbridge://nutrition?name=...&kcal=...&protein=...&fat=...&carbs=...&meal=snack&autocommit=1
```

When opened, the app:

1. parses calories/macros and meal metadata;
2. requests only `WRITE_NUTRITION`;
3. inserts a `NutritionRecord` into Health Connect;
4. uses `clientRecordId` so opening the same link again does not create a duplicate record.

Health data stays local to the Android device. The optional MCP helper only generates a link; it does not get Health Connect permissions.

## Build

Requirements:

- JDK 17+
- Android SDK Platform 37
- Gradle 9.5+

### Linux / CLI

From the repository root:

```bash
./build.sh
```

`build.sh` looks for the Android SDK in `$ANDROID_HOME`, `$ANDROID_SDK_ROOT`, `/opt/android-sdk`, or `~/Android/Sdk`, writes `android/local.properties`, and runs:

```bash
gradle -p android assembleDebug
```

APK:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

You can also build directly:

```bash
export ANDROID_HOME=/path/to/android-sdk
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > android/local.properties
gradle -p android assembleDebug
```

## GitHub Actions

Every push to `main` builds a debug APK. The workflow also supports manual `workflow_dispatch` runs and uploads `healthbridge-debug-apk` as an artifact.

## Test via adb

After installing the APK and granting Health Connect permission:

```bash
adb shell am start -a android.intent.action.VIEW \
  -d 'healthbridge://nutrition?name=Test&kcal=625&protein=29&fat=40&carbs=33&meal=breakfast&autocommit=1'
```

## Deep-link generator

```bash
python3 tools/make_link.py \
  --name 'Яичница с беконом' \
  --kcal 625 \
  --protein 29 \
  --fat 40 \
  --carbs 33 \
  --meal breakfast \
  --at '2026-08-13T08:00:00+03:00'
```

## Optional MCP server

`mcp/server.py` exposes `make_nutrition_link`. It creates a HealthBridge URI; the Android app performs the actual write.

```bash
cd mcp
uv run server.py
```

The Streamable HTTP endpoint is `/mcp` by default.

## License

MIT.
