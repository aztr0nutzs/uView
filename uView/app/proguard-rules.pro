# Sentinel Home — ProGuard rules

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }

# Keep Room entities and DAOs
-keep class com.sentinel.app.data.local.entities.** { *; }
-keep class com.sentinel.app.data.local.dao.** { *; }

# Keep domain models used by Room TypeConverters
-keep class com.sentinel.app.domain.model.** { *; }

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keep @kotlinx.serialization.Serializable class * { *; }

# Keep ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Timber
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
