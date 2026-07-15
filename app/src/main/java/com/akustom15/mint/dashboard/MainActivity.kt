package com.akustom15.mint.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.akustom15.mint.library.billing.MintPremiumProduct
import com.akustom15.mint.library.config.MoreApp
import com.akustom15.mint.library.config.MintConfig
import com.akustom15.mint.library.notifications.MintNotificationHelper
import com.akustom15.mint.library.ui.MintScreen

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Push notifications: create channel + subscribe to FCM topic
        MintNotificationHelper.initialize(this)
        requestNotificationPermissionIfNeeded()

        val config = MintConfig(
            appName = getString(R.string.app_name),
            appSubtitle = "Liquid Glass Design",
            packageName = packageName,
            developerName = "AKustom15",
            iconRequestEmail = "akustom15help@gmail.com",
            iconRequestSubject = "Solicitud de iconos para ${getString(R.string.app_name)}",
            freeRequestLimit = 10,
            premiumEnabled = true,
            premiumProducts = listOf(
                MintPremiumProduct("premium_icon_request_5", 5, "$2.00"),
                MintPremiumProduct("premium_icon_request_10", 10, "$3.50"),
                MintPremiumProduct("premium_icon_request_20", 20, "$6.00"),
                MintPremiumProduct("premium_icon_request_50", 50, "$10.00")
            ),
            firestoreCollection = "icon_requests",
            firestoreUpdateDocument = "glasswave_current", // Cambia esto por el nombre de tu documento en Firebase (ej. "tuapp_current")
            
            // Remote Kill-Switch and Toggles
            appConfigUrl = "", // Sube un JSON a GitHub y pon la URL aquí para controlar las solicitudes
            
            cloudWallpapersUrl = "https://raw.githubusercontent.com/rs1525/wallpaper_glasswave/refs/heads/main/wallpaper_glaswave.json",
            // updateJsonUrl = "https://.../mint_version.json",
            
            // App Update Info / Changelog
            updateInfo = "• Added 25 new glassmorphic icons\n• 3 new clock widgets\n• Support for Nova Launcher 8\n• Fixed padding issue on settings screen",

            // ── More Apps carousel ─────────────────────────────────────────
            // Remote JSON takes priority over the static list below.
            // Point this to your own more_apps.json on GitHub when ready.
            moreAppsJsonUrl = "https://raw.githubusercontent.com/rs1525/carrusel_more_apps/refs/heads/main/more_apps.json",

            // Static fallback list (shown while remote JSON loads, or if URL is empty)
            moreApps = listOf(
                MoreApp(
                    name = "GlassWave Icons Pack",
                    description = "Modern icon pack with Glassmorphic effect",
                    iconUrl = "https://raw.githubusercontent.com/rs1525/lunex_for_kwgt/d8f9e1fdc022eef412ba00fb6191d73a66192e90/icono_glasswave.png",
                    screenshotUrls = listOf(
                        "https://raw.githubusercontent.com/rs1525/lunex_for_kwgt/14a47ac0f11fac6be8579197b8f615420e8aaa54/glasswave.png"
                    ),
                    playStoreUrl = "https://play.google.com/store/apps/details?id=com.akustom15.glasswave"
                ),
                MoreApp(
                    name = "Zyra Icon Pack & Widgets",
                    description = "Dark icons with depth and functional native widgets",
                    iconUrl = "https://raw.githubusercontent.com/rs1525/lunex_for_kwgt/d8f9e1fdc022eef412ba00fb6191d73a66192e90/icono_zyra.png",
                    screenshotUrls = listOf(
                        "https://raw.githubusercontent.com/rs1525/lunex_for_kwgt/14a47ac0f11fac6be8579197b8f615420e8aaa54/zyra_playstore_1.png"
                    ),
                    playStoreUrl = "https://play.google.com/store/apps/details?id=com.akustom15.zyra"
                ),
                MoreApp(
                    name = "Dunkin for KWGT",
                    description = "Beautiful widgets with interactive design and 3D effect",
                    iconUrl = "https://raw.githubusercontent.com/rs1525/lunex_for_kwgt/d8f9e1fdc022eef412ba00fb6191d73a66192e90/icono_dunkin.png",
                    screenshotUrls = listOf(
                        "https://raw.githubusercontent.com/rs1525/lunex_for_kwgt/14a47ac0f11fac6be8579197b8f615420e8aaa54/dunkin.png"
                    ),
                    playStoreUrl = "https://play.google.com/store/apps/details?id=my.app.dunkinforkwgt.pack"
                ),
                MoreApp(
                    name = "Neumorphic 3D Fusion for KLWP",
                    description = "Customize your screen with 3D effects",
                    iconUrl = "https://raw.githubusercontent.com/rs1525/lunex_for_kwgt/d8f9e1fdc022eef412ba00fb6191d73a66192e90/icono_Neumorphic%203D%20Fusion%20for%20Klwp.png",
                    screenshotUrls = listOf(
                        "https://raw.githubusercontent.com/rs1525/lunex_for_kwgt/14a47ac0f11fac6be8579197b8f615420e8aaa54/neumorphic.png"
                    ),
                    playStoreUrl = "https://play.google.com/store/apps/details?id=my.app.neumorphic3dfusionforklwp.pack"
                )
            )
        )

        setContent {
            MintScreen(config = config)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
