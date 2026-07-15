package com.akustom15.mint.library.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import android.widget.Toast
import com.akustom15.mint.library.data.LauncherInfo
import com.akustom15.mint.library.data.LaunchersRepository

object LauncherUtils {

    private const val TAG = "LauncherUtils"

    fun getInstalledCompatibleLaunchers(context: Context): List<LauncherInfo> {
        val packageManager = context.packageManager
        val mainHomeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val homeResolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(mainHomeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val installedLauncherPackages = homeResolveInfoList.map { it.activityInfo.packageName }.toMutableSet()

        LaunchersRepository.supportedLaunchers.forEach { launcher ->
            if (!installedLauncherPackages.contains(launcher.packageName)) {
                try {
                    packageManager.getPackageInfo(launcher.packageName, 0)
                    installedLauncherPackages.add(launcher.packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                }
            }
        }
        
        return LaunchersRepository.supportedLaunchers.filter { launcher ->
            installedLauncherPackages.contains(launcher.packageName)
        }
    }

    fun applyIconPackToLauncher(context: Context, launcher: LauncherInfo): Boolean {
        return when (launcher.packageName) {
            "com.teslacoilsw.launcher" -> applyToNovaLauncher(context, launcher)
            "com.microsoft.launcher" -> applyToMicrosoftLauncher(context, launcher)
            "com.anddoes.launcher" -> applyToApexLauncher(context, launcher)
            "ch.deletescape.lawnchair.plah",
            "ch.deletescape.lawnchair",
            "app.lawnchair" -> applyToLawnchairLauncher(context, launcher)
            "ginlemon.smartlauncher",
            "ginlemon.flowerfree" -> applyToSmartLauncher(context, launcher)
            "ginlemon.flower.launcher" -> applyToSmartLauncher(context, launcher)
            "com.actionlauncher.playstore" -> applyToActionLauncher(context, launcher)
            "com.sec.android.app.launcher" -> applyToSamsungLauncher(context)
            else -> applyGeneric(context, launcher)
        }
    }

    private fun applyToSamsungLauncher(context: Context): Boolean {
        val iconPackPackageName = context.packageName
        // Samsung One UI doesn't have a direct icon pack intent.
        // Try opening the Theme Store icon section, or fall back to opening the launcher.
        val attempts = listOf(
            {
                // Try Samsung Galaxy Themes icon pack intent
                Intent("com.samsung.android.themestore.ICON_PACK").apply {
                    putExtra("package", iconPackPackageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            },
            {
                // Try opening Samsung Theme settings
                Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.themestore",
                        "com.samsung.android.themestore.activity.MainActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            },
            {
                // Fallback: open home launcher settings
                context.packageManager.getLaunchIntentForPackage("com.sec.android.app.launcher")?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )

        for (attemptGenerator in attempts) {
            try {
                val intent = attemptGenerator()
                if (intent != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                Log.d(TAG, "Samsung launcher attempt failed: ${e.message}")
            }
        }
        Toast.makeText(
            context,
            "Samsung One UI: Ve a Ajustes > Pantalla de inicio > aplicar el pack de iconos manualmente.",
            Toast.LENGTH_LONG
        ).show()
        return false
    }

    private fun applyToNovaLauncher(context: Context, launcher: LauncherInfo): Boolean {
        val iconPackPackageName = context.packageName
        val attempts = listOf(
            {
                Intent("com.teslacoilsw.launcher.APPLY_ICON_THEME").apply {
                    putExtra("com.teslacoilsw.launcher.extra.ICON_THEME_TYPE", "GO")
                    putExtra("com.teslacoilsw.launcher.extra.ICON_THEME_PACKAGE", iconPackPackageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            },
            {
                Intent("com.teslacoilsw.launcher.APPLY_ICON_THEME").apply {
                     putExtra("com.teslacoilsw.launcher.extra.ICON_PACKAGE", iconPackPackageName)
                     addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            },
            {
                Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(launcher.packageName, "com.teslacoilsw.launcher.NovaLauncher")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )

        for (attemptGenerator in attempts) {
            try {
                val intent = attemptGenerator()
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
            }
        }
        Toast.makeText(context, "No se pudo aplicar a Nova Launcher.", Toast.LENGTH_LONG).show()
        return false
    }
    
    private fun applyToMicrosoftLauncher(context: Context, launcher: LauncherInfo): Boolean {
        val iconPackPackageName = context.packageName
        val intent = Intent("com.microsoft.launcher.action.APPLY_ICON_THEME").apply {
            setPackage(launcher.packageName)
            putExtra("com.microsoft.launcher.iconpack.ICON_THEME_PACKAGE", iconPackPackageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
             try {
                val settingsIntent = Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(launcher.packageName, "com.microsoft.launcher.launcher")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    private fun applyToApexLauncher(context: Context, launcher: LauncherInfo): Boolean {
        val iconPackPackageName = context.packageName
        val intent = Intent("com.anddoes.launcher.SET_THEME").apply {
            putExtra("com.anddoes.launcher.THEME_PACKAGE_NAME", iconPackPackageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun applyToLawnchairLauncher(context: Context, launcher: LauncherInfo): Boolean {
        val iconPackPackageName = context.packageName
        val action = if (launcher.packageName == "app.lawnchair") "app.lawnchair.APPLY_ICONS" else "ch.deletescape.lawnchair.APPLY_ICONS"
        val intent = Intent(action).apply {
            setPackage(launcher.packageName)
            putExtra("packageName", iconPackPackageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun applyToSmartLauncher(context: Context, launcher: LauncherInfo): Boolean {
        val iconPackPackageName = context.packageName
        val intent = Intent("ginlemon.smartlauncher.SET_THEME").apply {
            setPackage(launcher.packageName)
            putExtra("package", iconPackPackageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            val openThemesIntent = Intent("ginlemon.smartlauncher.THEMES").apply {
                setPackage(launcher.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(openThemesIntent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    private fun applyToActionLauncher(context: Context, launcher: LauncherInfo): Boolean {
        val iconPackPackageName = context.packageName
        val intent = Intent("com.actionlauncher.THEME").apply {
            setPackage(launcher.packageName)
            putExtra("iconpack", iconPackPackageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun applyGeneric(context: Context, launcher: LauncherInfo): Boolean {
        val iconPackPackageName = context.packageName
        if (launcher.intentAction.isNotBlank() && launcher.extraKey.isNotBlank()) {
            try {
                val intent = Intent(launcher.intentAction).apply {
                    setPackage(launcher.packageName)
                    putExtra(launcher.extraKey, iconPackPackageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
            }
        }

        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(launcher.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return true
            } else {
                throw PackageManager.NameNotFoundException("No se encontró Launch Intent para ${launcher.packageName}")
            }
        } catch (e: Exception) {
            return false
        }
    }
}
