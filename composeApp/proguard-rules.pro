# Project-specific ProGuard rules for composeApp Android release builds.

# Keep useful metadata for crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Kotlin metadata/signatures needed by reflection/generics-heavy libraries.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations

# Ktor client stack (runtime reflective paths in serializers/plugins).
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep @Serializable generated serializers.
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class io.github.dimitrysaf.provenio.core.catalog.CatalogTargetKind { *; }

# Avoid R8 merging/optimizing the stream badge chip used in lazy stream rows.
-keep class io.github.dimitrysaf.provenio.shell.screens.streams.StreamBadgeChipKt { *; }
-keep class io.github.dimitrysaf.provenio.shell.screens.streams.StreamBadgeChipSize { *; }
-keep class io.github.dimitrysaf.provenio.shell.screens.streams.StreamBadgeChipDefaults { *; }

# Avoid R8 producing verifier-invalid bytecode for the large player composable.
-keep class io.github.dimitrysaf.provenio.shell.screens.player.PlayerKt { *; }
-keep class io.github.dimitrysaf.provenio.shell.screens.player.PlayerKt$* { *; }

# QuickJS plugin runtime is dynamic; keep runtime and app plugin classes.
-keep class com.dokar.quickjs.** { *; }
-keep class io.github.dimitrysaf.provenio.shell.screens.plugins.** { *; }
-keep class io.github.dimitrysaf.provenio.core.plugins.** { *; }

# P2P runtime and Nuvio Engine JNI bridge. Native libraries are not processed
# by R8, but their Kotlin/JNI wrapper classes and method names must stay stable.
-keep class io.github.dimitrysaf.provenio.shell.screens.p2p.** { *; }
-keep class io.github.dimitrysaf.provenio.core.p2p.** { *; }
-keep class com.nuvio.engine.** { *; }
-keep interface com.nuvio.engine.** { *; }

-keep class androidx.work.impl.WorkDatabase_Impl { *; }

# Media3 / ExoPlayer classes from local AAR decoders and stock modules.
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-keep interface com.google.android.exoplayer2.** { *; }

-keep class is.xyz.mpv.** { *; }
-keep interface is.xyz.mpv.** { *; }

# Common optional security providers used by okhttp on some devices.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
