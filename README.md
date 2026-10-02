# Lloyd ERP Attendance Pro Max 🎓⚡

A modern, high-performance attendance tracker, Apple Dynamic Island-styled notification engine, and Android Home Screen widget for Lloyd College students (`erp.lloydcollege.in`).

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white)
![UI/UX](https://img.shields.io/badge/Design-Swiss%20Minimalism-6366F1?style=flat)
![Security](https://img.shields.io/badge/Auth-EncryptedSharedPreferences-10B981?style=flat)

---

## ✨ Key Features

### 1. 📱 Native Android Home Screen Widget
- **Live Attendance Percentage & Status**: Real-time attendance display with color-coded safety indicators (`≥ 75% SAFE` vs `< 75% SHORTAGE`).
- **Dynamic Bunk Buffer Calculator**: Instantly computes how many upcoming lectures you can safely bunk without falling below 75%, or how many consecutive classes you must attend to recover.
- **One-Tap Background Sync**: Interactive refresh button directly on the widget to update attendance on-demand without opening the app.

### 2. 🔔 Apple Dynamic Island / Heads-Up Attendance Alerts
- **Real-Time Notification Banner**: Instant notification whenever a teacher marks your attendance.
- **Detailed Context**: Shows the faculty member's name, subject name, lecture period, and whether you were marked **PRESENT ✅** or **ABSENT ❌**.
- **Live Margin Update**: Updates your overall attendance percentage immediately on the banner.

### 3. 📅 All Classes Log (Multi-Criteria Filter & Chronological Grouping)
- **Zero Mock Data**: Fetches 100% real semester class logs directly from `/api/attendance/student`.
- **Multi-Criteria Filtering**:
  - **Status Filter**: `All`, `Present`, `Absent` with dynamic counts.
  - **Month Filter**: Seamlessly filter between semesters and months (`Oct 2026`, `Sep 2026`, `Aug 2026`).
  - **Subject Filter**: Filter lectures by course name chips.
- **Chronological Date Grouping**: Lectures are cleanly structured under date header cards (`📅 Thursday, Oct 01, 2026 • 5 Lectures (0P / 5A)`).
- **Lecture Cards**: Display lecture period, faculty member, marked timestamp, and color-coded status badges.

### 4. 📚 Subject-Wise Attendance Breakdown
- Complete subject catalog with attended, missed, and total lectures.
- Visual horizontal progress bars indicating safety margin.
- Subject-specific bunk and attendance requirements.

### 5. 🎯 Bunk & Goal Simulator
- Interactive delta slider (`-10` missed to `+15` attended).
- Real-time percentage projection and safety advice before you decide to attend or miss a lecture.

### 6. ⚡ Fast & Persistent Authentication
- **Permanent Local Persistence**: Credentials and session tokens are securely encrypted using AndroidX `EncryptedSharedPreferences` backed by the Android Keystore.
- **Zero-Friction Re-auth**: Transparent, silent token refresh and auto-relogin so the session never expires.
- **Optimized Networking**: Connection pooling with keep-alive across HTTP/TLS requests.

---

## 🛠️ Tech Stack & Architecture

- **Platform**: Android (Java 17 / SDK 34 / Android 14+ / Android 16 ready)
- **Architecture**: Clean MVC with repository pattern and OkHttp connection pooling
- **Security**: AES-256 GCM encrypted shared preferences (`androidx.security:security-crypto`)
- **Background Sync**: Android Jetpack `WorkManager` with network constraints
- **UI Design Intelligence**: Swiss minimalist glassmorphism (`#0B1120` deep dark slate, `#1E293B` elevated cards, `#10B981` emerald, `#EF4444` rose)
- **Web Client**: Included responsive PWA (`web/index.html`)

---

## 🚀 Building & Installation

### Prerequisites
- Android SDK 34+
- Java JDK 17+
- Gradle 8.10+

### Build APK
```bash
cd android
./gradlew assembleDebug
```

### Install via ADB
```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License
MIT License. Developed for Lloyd College students.
