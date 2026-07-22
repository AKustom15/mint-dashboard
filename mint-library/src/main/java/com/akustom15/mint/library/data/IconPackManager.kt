package com.akustom15.mint.library.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import org.xmlpull.v1.XmlPullParser

object IconPackManager {
    private var themedAppsCount: Int = 0
    private var totalIconsCount: Int = 0
    private var totalInstalledApps: Int = 0
    private var missingAppsCount: Int = 0
    private var iconResourceNames: List<String> = emptyList()
    private var categoryMap: Map<String, Set<String>> = emptyMap()

    fun getThemedAppsCount(): Int = themedAppsCount
    fun getTotalIconsCount(): Int = totalIconsCount
    fun getTotalInstalledApps(): Int = totalInstalledApps
    fun getMissingAppsCount(): Int = missingAppsCount
    fun getIconResourceNames(): List<String> = iconResourceNames
    fun getCategoryMap(): Map<String, Set<String>> = categoryMap
    fun getNewIconResourceNames(): List<String> = categoryMap["new"]?.toList() ?: emptyList()

    fun loadCategoriesFromXml(context: Context): Map<String, Set<String>> {
        if (categoryMap.isNotEmpty()) return categoryMap
        try {
            val packageName = context.packageName
            val resId = context.resources.getIdentifier("categorias", "xml", packageName)
            if (resId == 0) {
                Log.w("IconPackManager", "categorias.xml not found")
                return emptyMap()
            }
            val parser = context.resources.getXml(resId)
            val result = mutableMapOf<String, MutableSet<String>>()
            var currentCategory: String? = null
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "category" -> {
                                currentCategory = parser.getAttributeValue(null, "name") ?: parser.getAttributeValue(null, "title")
                                if (currentCategory != null) {
                                    result[currentCategory] = mutableSetOf()
                                }
                            }
                            "item" -> {
                                val drawable = parser.getAttributeValue(null, "drawable")
                                if (drawable != null && currentCategory != null) {
                                    result[currentCategory]?.add(drawable)
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "category") currentCategory = null
                    }
                }
                eventType = parser.next()
            }
            categoryMap = result
            Log.d("IconPackManager", "Categories loaded: ${result.keys}, total entries: ${result.values.sumOf { it.size }}")
            return result
        } catch (e: Exception) {
            Log.e("IconPackManager", "Error loading categorias.xml: ${e.message}", e)
            return emptyMap()
        }
    }

    private val EXCLUDED_ICONS = setOf("icon_back", "icon_mask", "icon_upon", "icon_base")

    private fun loadDrawableIcons(context: Context): Pair<Int, List<String>> {
        val packageName = context.packageName
        val resources = context.resources
        val iconNames = mutableSetOf<String>()

        try {
            val resId = resources.getIdentifier("drawable", "xml", packageName)
            if (resId != 0) {
                val parser = resources.getXml(resId)
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (drawable != null && drawable.startsWith("icon_") && !EXCLUDED_ICONS.contains(drawable)) {
                            iconNames.add(drawable)
                        }
                    }
                    eventType = parser.next()
                }
                parser.close()
            }
        } catch (e: Exception) {
            Log.e("IconPackManager", "Error parsing res/xml/drawable.xml: ${e.message}", e)
        }

        if (iconNames.isEmpty()) {
            try {
                AppFilterCache.getIconNames(context)
                    .filter { it.startsWith("icon_") && !EXCLUDED_ICONS.contains(it) }
                    .forEach { iconNames.add(it) }
            } catch (e: Exception) {
                Log.e("IconPackManager", "Error loading from appfilter fallback: ${e.message}", e)
            }
        }

        val validNames = iconNames.filter { name ->
            resources.getIdentifier(name, "drawable", packageName) != 0
        }.shuffled()

        return Pair(validNames.size, validNames)
    }

    private fun countInstalledAndThemed(context: Context) {
        try {
            val pm = context.packageManager
            val themedComponents = AppFilterCache.getThemedComponents(context)
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            var totalLaunchable = 0
            var themed = 0

            for (appInfo in installedApps) {
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val isUserApp = !isSystemApp || isUpdatedSystemApp
                if (!isUserApp) continue

                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName) ?: continue
                val component = launchIntent.component ?: continue
                totalLaunchable++

                val componentKey = "${appInfo.packageName}/${component.className}".lowercase().trim()
                if (themedComponents.contains(componentKey)) {
                    themed++
                }
            }

            totalInstalledApps = totalLaunchable
            themedAppsCount = themed
            missingAppsCount = totalLaunchable - themed
            Log.d("IconPackManager", "Apps: total=$totalLaunchable, themed=$themed, missing=$missingAppsCount")
        } catch (e: Exception) {
            Log.e("IconPackManager", "Error counting installed apps: ${e.message}", e)
        }
    }

    fun updateIconCounts(context: Context) {
        val (count, names) = loadDrawableIcons(context)
        totalIconsCount = count
        iconResourceNames = names
        countInstalledAndThemed(context)
        Log.d("IconPackManager", "Total de iconos actualizados: $totalIconsCount")
    }
}
