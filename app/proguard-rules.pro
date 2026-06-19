# Add project specific ProGuard rules here.

# Keep annotations for serialization
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# Kotlinx Serialization
-keep,allowobfuscation,allowshrinking class kotlin.reflect.jvm.internal.** { *; }
-keep class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# AndroidX Media3
-keep class androidx.media3.** { *; }

# Ktor
-keep class io.ktor.** { *; }

# InnerTube/API Models
-keep class com.omnitune.innertube.models.** { *; }
-keep class com.omnitune.kugou.models.** { *; }
-keep class com.omnitune.lrclib.models.** { *; }
-keep class com.omnitune.lastfm.models.** { *; }
-keep class com.omnitune.simpmusic.models.** { *; }

# Ignore missing Java desktop classes for Rhino/Ktor
-dontwarn java.beans.**
-dontwarn java.lang.management.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
