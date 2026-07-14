package com.akustom15.mint.dashboard

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

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
                    val rClass = Class.forName("$packageName.R\$drawable")
                    rClass.fields.forEach { field ->
                        if (field.type == Int::class.javaPrimitiveType && field.name.startsWith("icon_")) {
                            cursor.addRow(arrayOf(field.name, field.getInt(null)))
                        }
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
