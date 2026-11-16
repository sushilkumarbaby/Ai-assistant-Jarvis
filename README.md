# 🤖 JARVIS 4.0 — Android Native Digital Assistant App

A production-ready Native Android Assistant App (Kotlin + Jetpack Compose) engineered to directly replace **Google Gemini** / **Google Assistant** (or Siri) on Android devices, complete with Play Store deployment readiness.

---

## ⚡ How It Replaces Gemini on the Home Screen

Android enables custom voice assistants via the **`VoiceInteractionService`** and **`android.intent.action.ASSIST`** APIs.

Once installed on an Android device:
1. Go to **Settings → Apps → Default Apps → Digital Assistant App** (or *Assist & voice input*).
2. Select **JARVIS 4.0** as the Default Assistant.
3. Now, whenever you:
   * **Long-press the Power Button** (or Home Button)
   * **Swipe up from the bottom corner** (Android Assistant gesture)
4. The **JARVIS Floating HUD Overlay** immediately slides up over whatever app you are using, listens to your voice command with the spinning Arc Reactor, queries Gemini AI, answers aloud with a refined British voice, and commits context to long-term memory!

---

## 🏗️ Architecture & Stack

* **Language:** Kotlin 2.0+
* **UI Framework:** Jetpack Compose (Material 3 + Custom Canvas Vector Arc Reactor)
* **Target SDK:** Android 15 (API 35) — 100% compliant with Google Play Console requirements
* **Min SDK:** Android 8.0 (API 26) — Compatible with 99%+ of Android devices
* **System Assistant Integration:**
  * `JarvisVoiceInteractionService` (`android.service.voice.VoiceInteractionService`)
  * `JarvisVoiceInteractionSessionService`
  * `AssistOverlayActivity` (`android.intent.action.ASSIST`)
* **AI Engine:** Google Gemini 1.5/2.0/2.5 Flash API with intelligent memory injection
* **Voice Engine:** Android `SpeechRecognizer` & `TextToSpeech`
* **Long-Term Memory:** Thread-safe persistent memory matrix

---

## 📦 How to Build for Google Play Store (.aab / .apk)

### Method 1: Android Studio (1-Click)
1. Open this folder (`jarvis-android-app`) in **Android Studio** (Ladybug or newer).
2. Let Gradle sync dependencies automatically.
3. To test on your connected phone or emulator: Click **Run ▶**.
4. To generate the Play Store bundle:
   * Go to **Build → Generate Signed Bundle / APK...**
   * Select **Android App Bundle (.aab)**.
   * Provide your Keystore (or create a new one) and click **Create**.
   * Upload the generated `.aab` file to **Google Play Console**!

### Method 2: Command Line (Gradle)
```bash
./gradlew bundleRelease
```
The output `.aab` will be generated at:
`app/build/outputs/bundle/release/app-release.aab`

### Method 3: GitHub Actions (Free Cloud Build)
This project includes `.github/workflows/build-playstore-aab.yml`. Simply push this repository to GitHub, and GitHub Actions will automatically compile and produce the signed `.aab` and `.apk` under the **Actions** tab artifacts!

---

## 🔐 Google Gemini API Setup
Inside the app's **Config** tab:
1. Paste your free Gemini API Key from [Google AI Studio](https://aistudio.google.com/).
2. Set your custom Title / Name (default: *Sir*).
3. Adjust voice speech rate and pitch.
