package com.akustom15.mint.library.utils

import android.content.Context
import android.util.Log
import com.akustom15.mint.library.model.MintWidgetItem

/**
 * Utility class for reading widget files from assets/widgets/ folder.
 */
object MintAssetsReader {

    private const val TAG = "MintAssetsReader"
    private const val WIDGETS_FOLDER = "widgets"
    private const val WALLPAPERS_FOLDER = "wallpapers"
    private const val KWGT_EXTENSION = ".kwgt"
    private const val KLWP_EXTENSION = ".klwp"

    /**
     * Read all KWGT widget files from assets/widgets/ folder.
     * Extracts preview thumbnails and descriptions from within the .kwgt ZIP files.
     */
    fun getWidgetsFromAssets(context: Context): List<MintWidgetItem> {
        return try {
            val assetManager = context.assets
            val widgetFiles = assetManager.list(WIDGETS_FOLDER) ?: emptyArray()

            widgetFiles
                .filter {
                    it.endsWith(KWGT_EXTENSION, ignoreCase = true) ||
                    it.endsWith(KLWP_EXTENSION, ignoreCase = true)
                }
                .sorted()
                .mapIndexed { index, fileName ->
                    val nameWithoutExtension = fileName
                        .removeSuffix(KWGT_EXTENSION)
                        .removeSuffix(KLWP_EXTENSION)

                    // Extract preview image from widget file
                    val previewPath = MintPreviewExtractor.extractWidgetPreview(
                        context = context,
                        widgetFileName = fileName,
                        usePortrait = true
                    )

                    // Extract description from preset.json inside the file
                    val description = MintPreviewExtractor.extractWidgetDescription(context, fileName)
                        ?: formatName(nameWithoutExtension)

                    val isKlwp = fileName.endsWith(KLWP_EXTENSION, ignoreCase = true)

                    MintWidgetItem(
                        id = "widget_$index",
                        name = formatName(nameWithoutExtension),
                        description = description,
                        fileName = fileName,
                        previewPath = previewPath,
                        isKlwp = isKlwp
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing widgets from assets: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Format file name to display name.
     * Example: "LNX_001" -> "Lnx 001"
     */
    private fun formatName(fileName: String): String {
        return fileName.replace("_", " ").split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}
