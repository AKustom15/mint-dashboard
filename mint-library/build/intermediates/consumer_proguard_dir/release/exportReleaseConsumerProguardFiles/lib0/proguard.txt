# ── Mint library consumer R8 rules (applied in the consumer app's release build) ──

# Keep every library package EXCEPT `security`, which is intentionally left out so
# R8 obfuscates the anti-piracy / license / integrity classes (they become
# unreadable in the decompiled APK). Everything else stays kept to guarantee that
# Gson/Firestore reflection, Compose, Parcelize and resource lookups keep working.
-keep class com.akustom15.mint.library.config.** { *; }
-keep class com.akustom15.mint.library.billing.** { *; }
-keep class com.akustom15.mint.library.data.** { *; }
-keep class com.akustom15.mint.library.model.** { *; }
-keep class com.akustom15.mint.library.navigation.** { *; }
-keep class com.akustom15.mint.library.notifications.** { *; }
-keep class com.akustom15.mint.library.provider.** { *; }
-keep class com.akustom15.mint.library.ui.** { *; }
-keep class com.akustom15.mint.library.utils.** { *; }
# NOTE: com.akustom15.mint.library.security.** is deliberately NOT kept → obfuscated.
# (MintAppCheck/SecurityManager stay functional; R8 keeps reachable code, just renamed.)
-dontwarn com.akustom15.mint.library.**

# Keep app R$drawable/R$xml/R$raw fields so resources.getIdentifier() works
# in release builds (R8 inlines & removes R class fields in AGP 8+).
-keepclassmembers class **.R$drawable { public static <fields>; }
-keepclassmembers class **.R$xml { public static <fields>; }
-keepclassmembers class **.R$raw { public static <fields>; }

# Strip verbose/debug/info logs in release so the security flow (license checks,
# purchase verification, integrity) doesn't leave readable breadcrumbs in logcat
# or the decompiled bytecode. Error/warn logs are kept.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Allow R8 to rename across access boundaries and flatten the package tree for
# harder-to-follow decompiled output.
-allowaccessmodification
-repackageclasses ''
