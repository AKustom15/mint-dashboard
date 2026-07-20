# ── Mint library consumer R8 rules (applied in the consumer app's release build) ──

# Keep the public API and any model/config classes that Gson/Firestore read via
# reflection. NOTE: this currently keeps the WHOLE library unobfuscated, which
# also leaves the security classes readable. For stronger hardening, narrow this
# to only the packages that truly need reflection (config, model, notification
# data) and let `security`/`billing`/`ui` be obfuscated — then test a release
# build end-to-end before shipping (Gson/Firestore reflection can break).
-keep class com.akustom15.mint.library.** { *; }
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
