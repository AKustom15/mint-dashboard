package com.akustom15.mint.library.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Utility to extract preview images and metadata from Kustom widget (.kwgt) files.
 * The .kwgt files are ZIP archives containing embedded thumbnails and preset.json.
 */
object MintPreviewExtractor {

    private const val TAG = "MintPreviewExtractor"

    private val PORTRAIT_THUMBS = listOf("preset_thumb_portrait.png", "preset_thumb_portrait.jpg")
    private val LANDSCAPE_THUMBS = listOf("preset_thumb_landscape.png", "preset_thumb_landscape.jpg")

    /**
     * Extract preview image from a .kwgt widget file.
     * Returns the absolute path to the extracted preview image, or null if extraction fails.
     *
     * @param context Android context
     * @param widgetFileName Name of the .kwgt file in assets/widgets
     * @param usePortrait Whether to extract portrait (true) or landscape (false) thumbnail
     * @return Absolute path to extracted preview image, or null
     */
    fun extractWidgetPreview(
        context: Context,
        widgetFileName: String,
        usePortrait: Boolean = true
    ): String? {
        return extractPreview(
            context = context,
            assetPath = "widgets/$widgetFileName",
            fileName = widgetFileName,
            usePortrait = usePortrait
        )
    }

    /**
     * Extract description from preset.json inside a .kwgt widget file.
     */
    fun extractWidgetDescription(context: Context, widgetFileName: String): String? {
        return extractDescription(context, "widgets/$widgetFileName")
    }

    private fun extractPreview(
        context: Context,
        assetPath: String,
        fileName: String,
        usePortrait: Boolean
    ): String? {
        try {
            // Use filesDir so previews survive cache clearing
            val previewCacheDir = File(context.filesDir, "mint_previews")
            if (!previewCacheDir.exists()) {
                previewCacheDir.mkdirs()
            }

            val orientation = if (usePortrait) "portrait" else "landscape"
            val outputFileName = "${fileName}_${orientation}.png"
            val outputFile = File(previewCacheDir, outputFileName)

            // If already extracted, return cached path
            if (outputFile.exists()) {
                return outputFile.absolutePath
            }

            // Open the .kwgt file as ZIP from assets
            val inputStream = context.assets.open(assetPath)
            val zipInputStream = ZipInputStream(inputStream)

            val thumbNames = if (usePortrait) PORTRAIT_THUMBS else LANDSCAPE_THUMBS

            // Search for the thumbnail file inside the ZIP
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name in thumbNames) {
                    val bitmap = BitmapFactory.decodeStream(zipInputStream)

                    if (bitmap != null) {
                        FileOutputStream(outputFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        bitmap.recycle()

                        Log.d(TAG, "Extracted preview for $fileName: ${outputFile.absolutePath}")
                        zipInputStream.close()
                        return outputFile.absolutePath
                    }
                }
                entry = zipInputStream.nextEntry
            }

            zipInputStream.close()
            Log.w(TAG, "Preview not found in $fileName")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting preview from $fileName", e)
            return null
        }
    }

    private fun extractDescription(context: Context, assetPath: String): String? {
        try {
            val inputStream = context.assets.open(assetPath)
            val zipInputStream = ZipInputStream(inputStream)

            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name == "preset.json") {
                    val jsonString = zipInputStream.bufferedReader().readText()
                    zipInputStream.close()
                    val json = JSONObject(jsonString)
                    if (json.has("preset_info")) {
                        val info = json.getJSONObject("preset_info")
                        if (info.has("description")) {
                            val desc = info.getString("description")
                            if (desc.isNotBlank()) return desc
                        }
                    }
                    if (json.has("description")) {
                        val desc = json.getString("description")
                        if (desc.isNotBlank()) return desc
                    }
                    return null
                }
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting description from $assetPath", e)
            return null
        }
    }

    /** Clear all cached preview images */
    fun clearPreviewCache(context: Context) {
        try {
            val previewDir = File(context.filesDir, "mint_previews")
            if (previewDir.exists()) {
                previewDir.deleteRecursively()
                Log.d(TAG, "Preview cache cleared")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing preview cache", e)
        }
    }
}
