package com.akustom15.mint.library.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log

data class AppInfo(
    val name: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable? = null
) {
    val componentName: String
        get() = "$packageName/$activityName"
}

data class IconItem(
    val name: String,
    val resourceId: Int
)

object IconApplicator {
    private const val PREFS_NAME = "icon_applicator_prefs"
    private const val APPLIED_ICONS_KEY = "applied_icons"
    
    fun saveAppliedIcon(
        context: Context, 
        packageName: String, 
        iconName: String, 
        variant: String
    ) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val appliedIcons = sharedPrefs.getStringSet(APPLIED_ICONS_KEY, mutableSetOf()) ?: mutableSetOf()
        
        val iconInfo = "$packageName:$iconName:$variant"
        val updatedIcons = appliedIcons.toMutableSet()
        
        updatedIcons.removeAll { it.startsWith("$packageName:") }
        updatedIcons.add(iconInfo)
        
        sharedPrefs.edit().putStringSet(APPLIED_ICONS_KEY, updatedIcons).apply()
    }
    
    fun applyIconToApp(
        context: Context, 
        iconItem: IconItem, 
        variant: String,
        targetPackage: String,
        targetActivity: String
    ): Boolean {
        try {
            val iconNameBase = iconItem.name.substringAfter("icon_")
            val variantResourceName = when (variant) {
                "Original" -> iconItem.name
                "Minimalista" -> "icon_${iconNameBase}_minimal"
                "Redondeado" -> "icon_${iconNameBase}_circle"
                "Flat" -> "icon_${iconNameBase}_flat"
                else -> {
                    if (variant.startsWith("Variante ")) {
                        val number = variant.substringAfter("Variante ").trim()
                        "icon_${iconNameBase}_$number"
                    } else if (variant == "Alternativo") {
                        "icon_${iconNameBase}_2"
                    } else {
                        iconItem.name
                    }
                }
            }
            
            saveAppliedIcon(context, targetPackage, variantResourceName, variant)
            return true
        } catch (e: Exception) {
            Log.e("IconApplicator", "Error al aplicar icono", e)
            return false
        }
    }
    
    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val resolveInfoList = pm.queryIntentActivities(intent, 0)
        return resolveInfoList.map { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            val appName = resolveInfo.loadLabel(pm).toString()
            val icon = resolveInfo.loadIcon(pm)
            AppInfo(
                name = appName,
                packageName = activityInfo.packageName,
                activityName = activityInfo.name,
                icon = icon
            )
        }
    }
    
    fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
    
    fun getAllLaunchableApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val launchableApps = allApps.filter { appInfo ->
            val intent = pm.getLaunchIntentForPackage(appInfo.packageName)
            intent != null
        }
        return launchableApps.map { appInfo ->
            val appName = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
            val activityName = launchIntent?.component?.className ?: ""
            AppInfo(
                name = appName,
                packageName = appInfo.packageName,
                activityName = activityName,
                icon = icon
            )
        }
    }
}
