# QueueLess+ — Smart Virtual Queue Management

An Android app that eliminates physical waiting lines. Users join queues digitally and track their position in real-time. Admins manage queues, orders, and users from a dedicated dashboard.

---

## How to Make It Work (Quick Start)

### Step 1 — Firebase Setup (Required)

The app runs entirely on Firebase. You must set this up first.

1. Go to [console.firebase.google.com](https://console.firebase.google.com)
2. Click **Add project** → name it `QueueLessPlus`
3. Click the **Android icon** to add an Android app
4. Enter package name: `com.queueless.plus`
5. Click **Register app**
6. Download `google-services.json`
7. Copy it into the `app/` folder (replace the existing one)

> **Without this file the app will not build or run.**

---

### Step 2 — Enable Firebase Services

In the Firebase Console, enable these three services:

| Service | Where | Setting |
|---|---|---|
| Authentication | Build → Authentication → Sign-in method | Enable **Email/Password** |
| Firestore | Build → Firestore Database | Create database → **Production mode** |
| Cloud Messaging | Build → Cloud Messaging | Enabled by default |

---

### Step 3 — Build the APK

Open **PowerShell** inside the project folder and run:

**Release APK** (for sharing with others):
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; .\gradlew assembleRelease
```

**Debug APK** (for testing on your own device):
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; .\gradlew assembleDebug
```

**APK output locations:**
```
app\build\outputs\apk\release\app-release.apk   ← share this
app\build\outputs\apk\debug\app-debug.apk        ← for testing
```

---

### Step 4 — Install on a Phone

1. Transfer the APK to the phone (USB, WhatsApp, Google Drive, etc.)
2. On the phone: **Settings → Security → Install unknown apps → Allow**
3. Tap the APK to install
4. Works on **Android 7.0+**

---

### Step 5 — Create the First Admin

Every user who registers gets the `user` role by default. To make someone an admin:

1. Open [Firebase Console](https://console.firebase.google.com) → **Firestore Database**
2. Browse to **users** collection → find the user document
3. Change the `role` field from `"user"` to `"admin"`
4. Log out and log back in — the app will now show the **Admin Dashboard**

> Alternatively, the **Admin Dashboard** has a "Manage Users" screen where existing admins can promote other users to admin directly from the app.

---

### Step 6 — Set Up Queues (Admin Only)

Once logged in as admin:

1. Go to **Admin Dashboard → Admin Panel**
2. Tap the **+** button to create a queue
3. Fill in: Queue Name, Description, Location, Average Service Time
4. Tap **Create**
5. The queue is now live and visible to all users

---

## How It Works — User Flow

```
Register / Login (email + password)
        ↓
User Dashboard — see all active queues
        ↓
Tap a queue → Queue Detail page
        ↓
Join Queue → get a position number + QR code
        ↓
Place Order (optional) → select menu items + payment method
        ↓
View Status → see live position countdown
        ↓
Admin marks order complete → user sees "Completed" status
```

---

## How It Works — Admin Flow

```
Login as admin → Admin Dashboard
        ↓
Admin Panel — create/manage queues
        ↓
Manage Queue — see all people waiting
        ↓
Mark orders: Preparing → Ready → Completed
        ↓
Scan QR — scan a user's QR to see their order
        ↓
User Management — promote users to admin
```

---

## Firestore Collections

| Collection | Purpose |
|---|---|
| `users` | User profiles and roles |
| `queues` | Queue definitions created by admins |
| `queueEntries` | Each user's position in a queue |
| `orders` | Orders placed by users |
| `menu` | Menu items managed by admin |
| `notifications` | In-app notifications |

---

## Build Commands Reference

| Command | What it does |
|---|---|
| `.\gradlew assembleRelease` | Builds signed release APK |
| `.\gradlew assembleDebug` | Builds unsigned debug APK |
| `.\gradlew installDebug` | Builds + installs on connected device/emulator |
| `.\gradlew clean` | Clears build cache |

> Always prefix with:
> ```powershell
> $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH";
> ```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | XML + Material Design 3 |
| Authentication | Firebase Auth (Email/Password) |
| Database | Firebase Firestore |
| Push Notifications | Firebase Cloud Messaging |
| QR Scanning | ZXing Android Embedded |
| Async | Kotlin Coroutines |
| Theme | DayNight (light + dark mode) |

---

## Troubleshooting

| Problem | Fix |
|---|---|
| App won't build | Make sure `app/google-services.json` exists |
| Login fails | Check Firebase Auth has Email/Password enabled |
| No queues showing | Admin must create queues first |
| "User profile not found" | Register again; Firestore write may have failed |
| App crashes on order screen | Scroll only — RecyclerViews replaced with LinearLayout |
| Dark mode not persisting | Toggle using the icon in the top toolbar |

---

## License

Created for academic purposes. Free to use and modify.
