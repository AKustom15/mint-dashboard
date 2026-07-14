# Mint Dashboard Library - Base App Configuration

Welcome to the Mint Dashboard Base App template! This project (`com.akustom15.mint.dashboard`) is ready to be configured as your own Icon Pack Dashboard.

## 1. Icon Pack XML Structure

Mint relies on the standard icon pack XML files to map your drawables to installed apps. 
You must place your XML files in `app/src/main/res/xml/`.

- **appfilter.xml**: Maps ComponentNames to drawables.
- **appmap.xml**: Maps Package Names to drawables.
- **drawable.xml**: Defines categories for the icons shown in the dashboard.
- **alternative/**: A sub-directory where you can place `nova_config.xml`, `apex_config.xml`, etc., for specific third-party launcher compatibility.

*Example:* Look at the dummy files already created in `app/src/main/res/xml/` for guidance.

## 2. Configuring MintConfig

In your `app/src/main/java/com/akustom15/mint/MainActivity.kt`, you provide a `MintConfig` instance to `MintScreen`.

```kotlin
val config = MintConfig(
    // URLs
    playStoreUrl = "https://play.store/my_app",
    moreAppsJsonUrl = "https://example.com/more_apps.json",
    
    // Feature Toggles
    showWidgets = true,
    showWallpapers = true,
    
    // Security (100% Anti-Piracy)
    enableAntiPiracy = true,
    base64LicenseKey = "YOUR_BASE64_KEY_FROM_GOOGLE_PLAY_CONSOLE",
    gcpProjectNumber = 123456789L, // From Firebase / Google Cloud

    // Colors
    colorConfig = MintColorConfig(
        lightStatusBarColor = 0xFFFFFFFF,
        darkStatusBarColor = 0xFF000000
        // ... Customize your theme colors here
    )
)
```

## 3. Anti-Piracy Security

Mint comes with an extremely robust, GlassWave-inspired security system:
1. **Play Integrity API**: Validates the device and app installation source.
2. **License Verification (LVL)**: Checks purchase status via Google Play.
3. **Installer Verification**: Blocks alternative pirate stores (Aptoide, Blackmart, etc.).
4. **Signature Checks**: Prevents APK modifications and Lucky Patcher injections.

To enable it, simply set `enableAntiPiracy = true` and provide your `base64LicenseKey` in the `MintConfig`. The Dashboard will automatically show a non-dismissible warning screen if the checks fail.

## 4. Drawables

Place all your icon images (`.png` or `.webp`) in `app/src/main/res/drawable-nodpi/`. Make sure the names match the ones defined in your `appfilter.xml`.

## 5. Deployment

Once your icons are ready and your config is set, you can build the signed APK from Android Studio:
`Build > Generate Signed Bundle / APK...`
