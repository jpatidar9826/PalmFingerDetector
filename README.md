# PalmFingerDetector

**PalmFingerDetector** is an Android application for biometric data capture and verification. It utilizes **Google MediaPipe** for precise hand landmark detection and **CameraX** for high-quality image capture.

## 📱 Features

* **Guided Biometric Capture:**
* **Real-time Hand Detection:**
* **Smart Quality Checks:**
* **Optimized Storage:**
* **Verification Mode:**
* **Simple UI:**

---

## 🛠️ Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material3)
* **Camera:** CameraX (ProcessCameraProvider, ImageAnalysis, ImageCapture)
* **ML/AI:** Google MediaPipe (Hand Landmarker Task)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Concurrency:** Kotlin Coroutines & Flow

---

## 🚀 How to Build and Run

**Development Environment:** Android Studio Narwhal | 2025.1.1 Patch 1

### Installation Steps
1.  **Clone the Repository:**

2.  **Open in Android Studio:**

3.  **Build the Project:**

4.  **Run on Device:**

**Note:** The app requires **Camera** and **Storage** permissions. Please grant these when prompted on the first launch.

---

## 💡 Project Approach & Architecture

The application leverages **Google MediaPipe** for robust, real-time hand tracking, extracting 21 3D landmarks via **CameraX** analysis on background threads to ensure a smooth UI.

I addressed camera lifecycle challenges in Jetpack Compose by manually managing the `ProcessCameraProvider` state, preventing crashes during navigation. To optimize storage, I implemented a system where captured images are resized and compressed to JPEG format, while landmark data is serialized into JSON for efficient retrieval. Identity verification is performed by calculating the Euclidean distance between live and stored landmarks, validating users based on a precise similarity threshold.

---

## 📂 Project Structure

```text
com.example.palmdetector
├── model/              # Data classes
├── ui/
│   ├── components/     # Reusable UI
│   ├── screens/        # Composable Screens
│   └── theme/
├── utils/              # Helpers
├── viewmodel/          # BiometricViewModel (State management)
└── MainActivity.kt     # Navigation Graph & Entry Point