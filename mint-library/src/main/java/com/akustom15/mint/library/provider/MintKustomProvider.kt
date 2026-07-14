package com.akustom15.mint.library.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

/**
 * Content Provider that exposes Kustom preset files (.kwgt / .klwp) from
 * assets/widgets/ to the KWGT and KLWP apps via the kfile:// URI scheme.
 *
 * Automatically registered in the library AndroidManifest with authority
 * `${applicationId}`, so consumer apps do NOT need to add it manually —
 * manifest merger handles it.
 *
 * URI format: kfile://{applicationId}/widgets/{filename.kwgt}
 */
class MintKustomProvider : ContentProvider() {

    companion object {
        private const val TAG = "MintKustomProvider"
    }

    override fun onCreate(): Boolean {
        Log.d(TAG, "MintKustomProvider initialized")
        return true
    }

    /**
     * Called by KWGT/KLWP to read a preset file.
     * Always copies the asset to cache first because compressed APK assets
     * cannot provide a raw file descriptor.
     */
    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val pfd = openFileInternal(uri)
        return AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }

    /**
     * Some Kustom versions call openFile instead of openAssetFile.
     */
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return openFileInternal(uri)
    }

    /**
     * Shared logic: resolve URI → asset path → cache file → PFD.
     */
    private fun openFileInternal(uri: Uri): ParcelFileDescriptor {
        val ctx = context ?: throw FileNotFoundException("Context unavailable")

        Log.d(TAG, "openFileInternal: URI=$uri")

        val pathSegments = uri.pathSegments
        if (pathSegments.size < 2) {
            throw FileNotFoundException("Invalid URI (need folder/filename): $uri")
        }

        val folder = pathSegments[0]   // "widgets" or "wallpapers"
        val fileName = pathSegments[1] // e.g. "MyPack_001.kwgt"
        val assetPath = "$folder/$fileName"

        // Verify asset exists
        val listing = ctx.assets.list(folder) ?: emptyArray()
        if (!listing.contains(fileName)) {
            throw FileNotFoundException("Asset not found: $assetPath")
        }

        // Copy to cache (handles compressed APK assets that can't be opened as FD)
        val cacheDir = File(ctx.cacheDir, "kustom_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val cachedFile = File(cacheDir, fileName)
        if (!cachedFile.exists() || cachedFile.length() == 0L) {
            ctx.assets.open(assetPath).use { input ->
                FileOutputStream(cachedFile).use { output -> input.copyTo(output) }
            }
            Log.d(TAG, "Cached asset: $assetPath → ${cachedFile.absolutePath}")
        }

        return ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String {
        return when {
            uri.path?.endsWith(".kwgt") == true -> "application/x-kustom-widget"
            uri.path?.endsWith(".klwp") == true -> "application/x-kustom-wallpaper"
            else -> "application/octet-stream"
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
