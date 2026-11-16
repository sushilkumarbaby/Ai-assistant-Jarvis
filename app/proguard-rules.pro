# Proguard rules for JARVIS AI Assistant (Play Store release)
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# Gson rules
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-keep class com.jarvis.assistant.ai.models.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Voice Interaction Service & Session
-keep class com.jarvis.assistant.service.** { *; }
-keep class com.jarvis.assistant.ui.** { *; }
