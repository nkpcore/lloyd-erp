# Lloyd ERP — Attendance Pro Max 🎓⚡

A modern, high-performance Android client and companion web admin portal built for students at Lloyd College (`erp.lloydcollege.in`). Completely modernized with **Kotlin**, **Jetpack Compose**, **Material 3 Expressive**, and an in-app **OTA GitHub Releases Updater**.

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white)
![UI/UX](https://img.shields.io/badge/Design-Material%203%20Expressive-F6BD60?style=flat)
![Language](https://img.shields.io/badge/Language-Kotlin%20%2B%20Compose-7F52FF?style=flat&logo=kotlin)
![Security](https://img.shields.io/badge/Auth-Hardware%20Keystore%20Vault-10B981?style=flat)

---

## 🎨 Material 3 Expressive Design System

The application features a warm designer aesthetic with `dynamicColor = false` to guarantee consistent brand identity across all Android 12+ and Android 16 devices:

- 🍯 **Honey Bronze** (`#F6BD60`) — Primary accent, hero badges, interactive toggles.
- 🌾 **Linen** (`#F7EDE2`) — Crisp surface background, cards, text contrast.
- 🌸 **Cotton Rose** (`#F5CAC3`) — Warning indicators, secondary accents.
- 🌿 **Muted Teal** (`#84A59D`) — Safe attendance badges ($>75\%$), success toasts.
- 🪸 **Light Coral** (`#F28482`) — Critical shortage alerts, danger indicators.

---

## ✨ Key Features

### 1. 🧭 Streamlined 3-Tab Compose Navigation
- **Attendance Tab**: Live summary gauge, dynamic bunk buffer cards, goal simulator, and detailed subject attendance list.
- **Daily Logs Tab**: Comprehensive chronological lecture log with multi-criteria filtering (Status: All/Present/Absent; Month chips; Subject filters).
- **Profile Tab**: Authenticated student credentials, Hardware Keystore vault status, student academic report export, live in-app OTA update card, and developer credits.

### 2. 🔍 Subject & Faculty Deep-Dive
- **Hero Progress Gauge**: Circular progress with dynamic color coding based on safe/danger thresholds.
- **Bunk Advisor**: Real-time projection telling you exactly how many upcoming classes can be safely bunked or must be attended.
- **Interactive What-If Simulator**: Live projection slider for planned attended and missed lectures.
- **Faculty Ledger**: Teacher initials avatar, faculty name, and reverse-chronological lecture ledger for that subject.

### 3. 🚀 In-App GitHub Releases OTA Updater
- Automated background check querying `https://api.github.com/repos/nkpcore/lloyd-erp/releases/latest`.
- Compares semantic versioning between installed runtime and GitHub releases.
- Automated APK download and clean installation Intent via AndroidX `FileProvider` (`com.lloyd.attendance.fileprovider`).

### 4. 📊 Student Fleet Telemetry & Web Admin Dashboard
- **Telemetry Reporter**: Dispatches anonymous device metadata, app version, OS API runtime, and timestamp to administrative endpoints.
- **Web Admin Dashboard** (`web/admin/`):
  - Real-time fleet KPI metrics (Total Users, Active Today, Version Adoption, Modern OS Share).
  - Version adoption bar distribution charts.
  - Searchable and filterable student installation table.

### 5. 🛡️ Strict Zero Hardcoding & Security Policy
- **100% Real Live Data**: All student attendance, subject catalogs, lecture logs, and credentials strictly originate from authenticated ERP endpoints (`/api/auth/login`, `/api/student/me/monthly-attendance`, `/api/attendance/student`).
- **Hardware Keystore Vault**: Encrypted credentials stored via AES-256-GCM.
- **BOLA Protection**: `student_id` is locked to the authenticated user ID and cannot be arbitrarily overwritten.
- **Clean Academic Exports**: Exported student reports (PDF / Text / CSV) are official academic documents free of watermarks or author injections.

---

## 🛠️ Architecture & Tech Stack

- **Platform**: Android (Target SDK 36 / Min SDK 26 / Android 14+ / Android 16 ready)
- **UI Framework**: 100% Jetpack Compose with Material 3 Expressive
- **State Management**: AndroidX `ViewModel`, `StateFlow`, Kotlin Coroutines
- **Networking**: OkHttp 4.12 with connection pooling and Gzip compression
- **Security**: AndroidX `security-crypto` (MasterKey Keystore)
- **Background Sync**: Jetpack `WorkManager`
- **Companion Web Portal**: Vanilla HTML5, CSS3 Tokens, and ES6+ in `web/admin/`

---

## 🚀 Building & Testing

### Prerequisites
- Android SDK 34+ (Build-Tools 36.0.0)
- Java JDK 17+
- Gradle 8.10+

### Run Unit Tests
```bash
cd android
./gradlew testDebugUnitTest
```

### Build Debug APK
```bash
cd android
./gradlew assembleDebug
```
The output APK is generated at:
`android/app/build/outputs/apk/debug/app-debug.apk`

---

## 👨‍💻 Author & Attribution

**Crafted by Nikhil Pandey**  
Repository: [nkpcore/lloyd-erp](https://github.com/nkpcore/lloyd-erp)  
License: MIT
