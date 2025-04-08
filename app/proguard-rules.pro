# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
# https://developer.android.com/guide/developing/tools/proguard.html

# Firebase Authentication
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Firebase Firestore
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.firestore.** { *; }

# Firebase Realtime Database
-keepattributes Signature
-keepclassmembers class com.focusflow.** {
  *;
}

# Domain models and data classes
-keep class com.focusflow.domain.model.** { *; }
-keep class com.focusflow.social.** { *; }
-keep class com.focusflow.ai.** { *; }
-keep class com.focusflow.analytics.** { *; }

# Firebase Cloud Messaging
-keep class com.google.firebase.messaging.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
