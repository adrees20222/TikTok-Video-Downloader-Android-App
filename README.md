# Downloader for TT 🚀

A premium, watermark-free video downloader for Android, built using **Kotlin**, **Jetpack Compose**, and **Material 3** guidelines. Highly optimized for responsiveness, visuals, and compliance with Google Play Protect standards.

---

## ✨ Features

- 📹 **Watermark-Free Downloads:** Retrieve the original HD videos directly in high quality.
- 🔗 **Share to Download:** Share a link directly from the official app to this app to initiate an instant download.
- ⚡ **Highly Responsive UI:** Designed with custom linear gradients (neon cyan and pink accents) and fluid glassmorphic cards.
- 📥 **Background Queueing:** Utilizes Android's native `DownloadManager` for progress notifications and robust background downloading.
- 🎬 **Instant Gallery Visibility:** Saves videos directly into the public `Movies/` folder so they instantly index and show up in the phone's default Gallery and Google Photos.
- 🗂️ **Local History Management:** Log of past downloads with options to play inside the app, share to other apps (WhatsApp, Telegram, etc.), or delete from storage.
- 🔒 **Privacy-First & Secure:** Requires **zero dangerous permissions** (not even storage permission on modern APIs) and communicates strictly over encrypted HTTPS protocols.

---

## 🛠️ Tech Stack

- **UI Framework:** Jetpack Compose (Declarative UI)
- **Design System:** Material Design 3
- **Networking:** OkHttp (for fast, synchronous API calls)
- **Image Loading:** Coil Compose (cached thumbnail loading)
- **Media API:** TikWM API integration
- **Download Engine:** Android System `DownloadManager` & `FileProvider`

---

## 🚀 How to Build & Run

### Prerequisites
- [Android Studio Ladybug (or higher)](https://developer.android.com/studio)
- Android SDK 26 (Android 8.0) or higher (Target SDK 34/35)

### Steps
1. **Clone the repository:**
   ```bash
   git clone https://github.com/adrees20222/TikTok-Video-Downloader-Android-App.git
   ```
2. **Open in Android Studio:**
   - Launch Android Studio and click **Open...**
   - Navigate to the cloned folder and select it.
3. **Sync and Run:**
   - Wait for Android Studio to finish indexing and syncing Gradle files.
   - Connect your Android device or start an emulator.
   - Click the green **Run (Play)** button at the top.

---

## 📜 License
This project is open-source and available under the [MIT License](LICENSE).
