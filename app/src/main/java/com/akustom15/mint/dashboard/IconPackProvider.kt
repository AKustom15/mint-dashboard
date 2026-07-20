package com.akustom15.mint.dashboard

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import org.xmlpull.v1.XmlPullParser

class IconPackProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY_SUFFIX = ".iconpack"
        private const val CODE_ICONS = 1
    }

    private lateinit var uriMatcher: UriMatcher

    override fun onCreate(): Boolean {
        val authority = "${context?.packageName}$AUTHORITY_SUFFIX"
        uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(authority, "icons", CODE_ICONS)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(arrayOf("icon_name", "icon_resource"))
        val ctx = context ?: return cursor

        when (uriMatcher.match(uri)) {
            CODE_ICONS -> {
                try {
                    val packageName = ctx.packageName
                    val resources = ctx.resources

                    // Parse res/xml/drawable.xml to enumerate icons (R8-safe)
                    val resId = resources.getIdentifier("drawable", "xml", packageName)
                    if (resId != 0) {
                        val parser = resources.getXml(resId)
                        var eventType = parser.eventType
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                                val drawable = parser.getAttributeValue(null, "drawable")
                                if (drawable != null && drawable.startsWith("icon_")) {
                                    val iconResId = resources.getIdentifier(drawable, "drawable", packageName)
                                    if (iconResId != 0) {
                                        cursor.addRow(arrayOf(drawable, iconResId))
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                        parser.close()
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        return cursor
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/icon"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
