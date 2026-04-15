# Add project specific ProGuard rules here.

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# App Models — must not be obfuscated so Firestore can deserialize them
-keep class com.queueless.plus.models.** { *; }

# ZXing QR
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.** { *; }

# AndroidX
-keep class androidx.** { *; }

# Suppress warnings
-dontwarn com.google.firebase.**
-dontwarn kotlinx.coroutines.**
