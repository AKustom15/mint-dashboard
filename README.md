# Mint Dashboard Library

Mint Dashboard is a highly customizable, modern Android Library designed for creating premium Icon Packs.

## Features
- **100% Anti-Piracy Security**: Integrated Google Play Integrity API, LVL License Verification, Installer Source checking, and Signature validation.
- **Dynamic Theming**: Support for dark/light mode, and custom colors injected via `MintConfig`.
- **Glassmorphism UI**: Beautiful, premium blur effects across the app using Haze.
- **JitPack Ready**: Can be easily imported into any Android project using JitPack.

## How to build your own Icon Pack
We have provided a fully prepared "Base App" in the `app` module. 
For detailed instructions on how to use it, please read the [WIKI_ICON_PACK.md](app/WIKI_ICON_PACK.md).

## Publishing to JitPack
1. Push this repository to your GitHub account/organization.
2. Go to [JitPack](https://jitpack.io/) and paste your repository URL.
3. JitPack will automatically build the `mint-library` module.
4. Add the generated dependency to your new projects!

```kotlin
// In your other projects
implementation("com.github.AKustom15:mint-dashboard:Tag")
```
