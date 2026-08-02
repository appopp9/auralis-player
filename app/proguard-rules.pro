# ---------------------------------------------------------------------------
# Media3 / ExoPlayer
# ---------------------------------------------------------------------------
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ---------------------------------------------------------------------------
# Room: entities are read reflectively by generated code, DAOs are implemented
# by generated *_Impl classes. Without these the release APK can crash where
# debug works.
# ---------------------------------------------------------------------------
-keep class com.auralis.player.data.db.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Domain models: used as Room projections, MediaItem extras and backup payload
# ---------------------------------------------------------------------------
-keep class com.auralis.player.domain.model.** { *; }

# ---------------------------------------------------------------------------
# Hilt / Dagger generated components
# ---------------------------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep class **_HiltModules* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-dontwarn dagger.hilt.**

# ViewModels are instantiated reflectively by the ViewModel factory
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# ---------------------------------------------------------------------------
# Compose
# ---------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable <methods>;
}

# ---------------------------------------------------------------------------
# Coroutines
# ---------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# Kotlin metadata and enum values() used by name lookups (SmartField etc.)
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------------------------------------------------------------------------
# App entry points: service, widgets, application
# ---------------------------------------------------------------------------
-keep class com.auralis.player.playback.PlaybackService { *; }
-keep class com.auralis.player.widget.** { *; }
-keep class com.auralis.player.AuralisApp { *; }
