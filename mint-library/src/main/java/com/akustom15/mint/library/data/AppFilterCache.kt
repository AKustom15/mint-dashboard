package com.akustom15.mint.library.data

import android.content.Context
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

object AppFilterCache {
    private var cachedIcons: List<String>? = null
    private var cachedThemedComponents: Set<String>? = null
    
    fun getIconNames(context: Context): List<String> {
        if (cachedIcons != null) {
            return cachedIcons!!
        }
        
        val iconNames = mutableListOf<String>()
        val packageName = context.packageName
        
        try {
            val appfilterResId = context.resources.getIdentifier("appfilter_new", "xml", packageName)
            if (appfilterResId == 0) {
                val fallbackResId = context.resources.getIdentifier("appfilter", "xml", packageName)
                if (fallbackResId == 0) return emptyList()
                
                parseXml(context, fallbackResId, iconNames)
            } else {
                parseXml(context, appfilterResId, iconNames)
            }
            
            cachedIcons = iconNames
        } catch (e: Exception) {
            Log.e("AppFilterCache", "Error loading icons from appfilter", e)
        }
        
        return iconNames
    }

    /**
     * Returns the set of themed component strings (packageName/activityName) from appfilter.xml.
     * Used for detecting which apps already have icons.
     */
    fun getThemedComponents(context: Context): Set<String> {
        if (cachedThemedComponents != null) {
            return cachedThemedComponents!!
        }

        val result = mutableSetOf<String>()
        val packageName = context.packageName

        try {
            val resId = context.resources.getIdentifier("appfilter", "xml", packageName)
            if (resId == 0) return emptySet()

            val parser = context.resources.getXml(resId)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val componentString = parser.getAttributeValue(null, "component")
                    if (componentString != null &&
                        componentString.startsWith("ComponentInfo{") &&
                        componentString.endsWith("}")
                    ) {
                        val fullComponent = componentString.substring(
                            "ComponentInfo{".length,
                            componentString.length - 1
                        )
                        if (fullComponent.isNotBlank() && fullComponent.contains("/") && !fullComponent.contains(" ")) {
                            result.add(fullComponent.lowercase().trim())
                        }
                    }
                }
                eventType = parser.next()
            }
            parser.close()
        } catch (e: Exception) {
            Log.e("AppFilterCache", "Error parsing appfilter for themed components", e)
        }

        cachedThemedComponents = result
        return result
    }
    
    private fun parseXml(context: Context, resId: Int, iconNames: MutableList<String>) {
        var parser = context.resources.getXml(resId)
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    for (i in 0 until parser.attributeCount) {
                        if (parser.getAttributeName(i) == "drawable") {
                            parser.getAttributeValue(i)?.let { iconNames.add(it) }
                        }
                    }
                }
                eventType = parser.next()
            }
        } finally {
            parser.close()
        }
    }
    
    fun clearCache() {
        cachedIcons = null
        cachedThemedComponents = null
    }
}
