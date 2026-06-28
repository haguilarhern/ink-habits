# Onyx SDK — reaches hidden firmware APIs by reflection/JNI; keep everything.
-keep class com.onyx.** { *; }
-dontwarn com.onyx.**

# HiddenApiBypass — reflects into hidden android.* APIs for the pen SDK.
-keep class org.lsposed.hiddenapibypass.** { *; }

# ML Kit digital-ink recognition — loads models via reflection/native. The AAR
# bundles consumer rules, but keep explicitly as belt-and-suspenders.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**

# Room entities are referenced from generated DAO/database code; keep them and
# their fields so column mapping isn't broken by renaming/stripping.
-keep class com.inkhabits.data.entity.** { *; }

# Optional transitive deps referenced but never present at runtime — safe to ignore.
-dontwarn org.slf4j.**
-dontwarn org.joda.convert.**
