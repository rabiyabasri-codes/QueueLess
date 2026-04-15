# QueueLess+ – Smart Virtual Queue Management System

A full-stack Android application that eliminates physical waiting lines by allowing users to join and manage queues digitally with real-time updates.

---

## Project Structure

```
QueueLessPlus/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/queueless/plus/
│       │   ├── activities/
│       │   │   ├── SplashActivity.kt
│       │   │   ├── LoginActivity.kt
│       │   │   ├── RegisterActivity.kt
│       │   │   ├── DashboardActivity.kt
│       │   │   ├── QueueDetailActivity.kt
│       │   │   ├── UserStatusActivity.kt
│       │   │   ├── QRScanActivity.kt
│       │   │   ├── AdminPanelActivity.kt
│       │   │   ├── CreateQueueActivity.kt
│       │   │   └── ManageQueueActivity.kt
│       │   ├── adapters/
│       │   │   ├── QueueAdapter.kt
│       │   │   ├── QueueEntryAdapter.kt
│       │   │   ├── AdminQueueAdapter.kt
│       │   │   └── ManageEntryAdapter.kt
│       │   ├── models/
│       │   │   ├── User.kt
│       │   │   ├── Queue.kt
│       │   │   └── QueueEntry.kt
│       │   └── utils/
│       │       ├── AuthManager.kt
│       │       ├── FirestoreRepository.kt
│       │       ├── FCMService.kt
│       │       ├── SessionManager.kt
│       │       └── Extensions.kt
│       └── res/
│           ├── layout/           (10 XML layouts)
│           ├── values/           (strings, colors, themes, dimens)
│           ├── drawable/         (icons + shapes)
│           └── xml/              (backup & extraction rules)
├── firestore.rules
├── firestore.indexes.json
├── firebase.json
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## Technologies

| Layer | Technology |
|---|---|
| Language | Kotlin |
| IDE | Android Studio |
| UI | XML + Material Design 3 |
| Auth | Firebase Authentication |
| Database | Firebase Cloud Firestore |
| Push | Firebase Cloud Messaging (FCM) |
| QR Scanning | ZXing Android Embedded |
| Architecture | Single-Activity / MVVM-lite |
| Async | Kotlin Coroutines + Flow |

---

## Setup Instructions

### 1. Firebase Project Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project named **QueueLessPlus**
3. Add an Android app with package name `com.queueless.plus`
4. Download `google-services.json` and place it in the `app/` directory
5. Enable the following services:
   - **Authentication** → Email/Password sign-in
   - **Firestore Database** → Start in production mode
   - **Cloud Messaging** → No extra setup needed

### 2. Deploy Firestore Rules & Indexes

```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login
firebase login

# Initialize (select your project)
firebase init firestore

# Deploy rules and indexes
firebase deploy --only firestore:rules,firestore:indexes
```

### 3. Open in Android Studio

1. Open Android Studio
2. Select **File → Open** and choose the `QueueLessPlus/` folder
3. Let Gradle sync complete
4. Connect a device or start an emulator (API 24+)
5. Click **Run**

---

## Firestore Database Structure

### `users` collection
```
users/{userId}
  ├── userId: String
  ├── name: String
  ├── email: String
  ├── role: "user" | "admin"
  └── fcmToken: String
```

### `queues` collection
```
queues/{queueId}
  ├── queueId: String
  ├── queueName: String
  ├── description: String
  ├── location: String
  ├── avgServiceTime: Int  (minutes)
  ├── createdBy: String    (admin userId)
  ├── isActive: Boolean
  └── currentCount: Int
```

### `queueEntries` collection
```
queueEntries/{entryId}
  ├── entryId: String
  ├── userId: String
  ├── queueId: String
  ├── userName: String
  ├── timestamp: Timestamp
  ├── status: "waiting" | "completed" | "left"
  └── notified: Boolean
```

---

## Core Algorithms

### 1. Queue Position
```kotlin
// Sorted by timestamp (FIFO). Position = index + 1
val entries = getWaitingEntries(queueId)   // sorted ASC by timestamp
val position = entries.indexOfFirst { it.userId == userId } + 1
```

### 2. Estimated Wait Time
```kotlin
// waitTime = usersAhead × avgServiceTime
val usersAhead = position - 1
val waitMinutes = usersAhead * queue.avgServiceTime
```

### 3. Notification Trigger
```kotlin
// Notify when position ≤ 2
if (position <= 2 && !entry.notified) {
    // Show banner + mark notified in Firestore
    FirestoreRepository.markEntryNotified(entry.entryId)
}
```

---

## Setting an Admin User

Admins are not self-registered. After a user signs up, update their role in Firestore:

```
Firebase Console → Firestore → users → {userId} → role = "admin"
```

Alternatively, use the Firebase Admin SDK or a Cloud Function to automate role assignment.

---

## QR Code Format

QR codes encode a URL-style payload:
```
queueless://join?queueId=<QUEUE_ID>
```

Generate QR codes using any QR generator library. The `QRScanActivity` parses this format and navigates directly to the queue detail screen.

---

## Optional Enhancements

- [ ] Dark mode (add `themes.xml` night variant)
- [ ] Analytics (peak hours tracking via Firebase Analytics)
- [ ] Multi-queue support (user joins multiple queues)
- [ ] QR code generation in admin panel
- [ ] Cloud Function to send FCM when position ≤ 2
- [ ] Countdown timer refinement using server timestamp

---

## License

This project is created for academic purposes. Free to use and modify.
