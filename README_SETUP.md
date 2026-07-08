# El-Motamyez Gallery — Setup Guide

## 1. Create a Supabase Project

1. Go to [https://supabase.com](https://supabase.com) and sign in (or create a free account).
2. Click **New Project**.
3. Fill in:
   - **Project name**: `ElMotamyezGallery` (or any name you like)
   - **Database password**: choose a strong password and save it
   - **Region**: pick the closest to your users
4. Select the **Free plan** and click **Create new project**.
5. Wait ~2 minutes for provisioning to complete.

---

## 2. Run the Database Schema

1. In your Supabase dashboard, go to **SQL Editor** (left sidebar).
2. Click **New query**.
3. Open the file `supabase/schema.sql` from this project.
4. Copy the entire contents and paste into the SQL editor.
5. Click **Run** (or press `Ctrl+Enter`).
6. You should see "Success. No rows returned" — the tables, RLS policies, and sample data are now created.

---

## 3. Get Your Project URL and Anon Key

1. In your Supabase dashboard, go to **Project Settings** (gear icon, bottom-left).
2. Click the **API** tab.
3. Copy:
   - **Project URL** — looks like `https://xxxxxxxxxxxx.supabase.co`
   - **anon / public key** — the long JWT string under "Project API keys"

---

## 4. Configure the App

Open the file:

```
composeApp/src/commonMain/kotlin/com/elmotamyez/gallery/data/remote/SupabaseClient.kt
```

Replace the placeholder values:

```kotlin
const val SUPABASE_URL = "YOUR_SUPABASE_URL"   // <- paste your Project URL here
const val SUPABASE_KEY = "YOUR_SUPABASE_ANON_KEY"  // <- paste your anon key here
```

Optionally also update `local.properties` with the same values (for reference).

---

## 5. Running Each Platform

### Desktop (JVM) — fastest way to test

```bash
./gradlew :composeApp:run
```

On Windows:
```cmd
gradlew.bat :composeApp:run
```

The desktop window will open immediately.

### Android

1. Open the project root in **Android Studio** (Electric Eel or newer).
2. Let Gradle sync complete.
3. Select the `composeApp` run configuration and pick an emulator or physical device.
4. Click **Run** (Shift+F10).

### iOS (macOS only)

1. First build the Kotlin framework:
   ```bash
   ./gradlew :composeApp:assembleXCFramework
   ```
2. Open `iosApp/iosApp.xcodeproj` in **Xcode 15+**.
3. Select your simulator or connected device.
4. Click **Run** (Cmd+R).

> **Note:** iOS builds require a Mac with Xcode installed. Cross-compiling from Windows is not supported by Apple's toolchain.

---

## Project Structure Overview

```
ElMotamyezGallery/
├── composeApp/src/
│   ├── commonMain/   — shared UI, ViewModels, data layer (all platforms)
│   ├── androidMain/  — Android entry point + iText7 PDF export
│   ├── desktopMain/  — Desktop entry point + iText7 PDF export (opens file after save)
│   └── iosMain/      — iOS entry point + UIKit print stub
├── iosApp/           — Xcode project wrapper
├── supabase/         — SQL schema + seed data
└── gradle/           — version catalog (libs.versions.toml)
```

## Tech Stack

| Layer | Library |
|---|---|
| UI | Compose Multiplatform 1.7.3 |
| Language | Kotlin 2.1.0 |
| Backend | supabase-kt 3.0.2 (Postgrest) |
| HTTP | Ktor Client 3.0.3 |
| DI | Koin 4.0.0 |
| Navigation | Voyager 1.1.0-beta03 |
| PDF (Android/Desktop) | iText7 |
| PDF (iOS) | UIKit print APIs (stub provided) |
