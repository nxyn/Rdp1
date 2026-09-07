# AI Client

A native Android AI chat client built with **Kotlin**, **Jetpack Compose**, and **OkHttp**.

Connect directly to Anthropic, OpenAI-compatible APIs, self-hosted servers, and local/LAN endpoints. All requests use native Android networking — no WebView, no browser CORS, and no third-party proxies.

## Features

- Native HTTPS/HTTP requests via OkHttp
- Anthropic Messages API + OpenAI-compatible chat completions
- SSE streaming with non-streaming fallback
- Secure API key storage (Android Keystore / EncryptedSharedPreferences)
- Model discovery + custom model ID entry
- Multiple conversations with Room persistence
- Markdown rendering, syntax-highlighted code blocks, copy buttons
- Dark / light / system themes
- Export chats (Markdown share sheet)
- Connection test + diagnostics

## Requirements

- Android Studio Ladybug (2024.2+) or newer
- JDK 17+
- Android SDK 35
- Physical device or emulator (API 26+)

## Open in Android Studio

1. Clone the repository.
2. Open Android Studio → **File → Open** → select the project root.
3. Ensure `local.properties` contains your SDK path:
   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```
4. Click **Sync Project with Gradle Files**.

## Run on a physical phone

1. Enable **Developer options** and **USB debugging** on your device.
2. Connect the phone via USB (or wireless debugging).
3. In Android Studio, select your device in the run target dropdown.
4. Click **Run ▶** (or `./gradlew installDebug`).

## Run on an emulator

1. Android Studio → **Device Manager → Create Device**.
2. Choose a phone profile and a system image (API 26+).
3. Start the emulator.
4. Click **Run ▶**.

For local server testing from the emulator, use `http://10.0.2.2:PORT/v1` to reach your host machine.

## Build a debug APK

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Build a release APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

Sign the APK before distribution (Android Studio → **Build → Generate Signed App Bundle / APK**).

## Build an AAB for Google Play

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

## Configuration

1. Launch the app and complete first-run setup (or open **Settings** later).
2. Choose provider: **Anthropic** or **OpenAI-compatible**.
3. Set **Base URL**, **API Key**, and **Model ID** (fetch models or enter a custom model).
4. Tap **Test Connection** to verify credentials.

### Example base URLs

| Provider | Default |
|----------|---------|
| Anthropic | `https://api.anthropic.com` |
| OpenAI-compatible | `https://api.openai.com/v1` |
| Self-hosted | `http://192.168.1.50:8000/v1` |
| Emulator → host | `http://10.0.2.2:8000/v1` |

## Architecture

```
app/src/main/java/com/nxyn/aiclient/
├── data/api/          # OkHttp providers (Anthropic, OpenAI-compatible)
├── data/database/     # Room conversations + messages
├── data/preferences/  # DataStore settings + encrypted API keys
├── domain/            # Models + repositories
├── ui/                # Compose screens, theme, components
└── util/              # URL helper, SSE parser, export, errors
```

## Security notes

- API keys are stored encrypted and never included in exports, logs, or error messages.
- HTTP endpoints are supported for local development; the app warns that HTTP is insecure.
- TLS certificate verification is **not** disabled globally.

## License

MIT
