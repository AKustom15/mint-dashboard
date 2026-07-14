package com.akustom15.mint.library.model

/**
 * Represents a Kustom Widget (KWGT) or Wallpaper (KLWP) item loaded from assets/widgets/
 *
 * @param id Unique identifier
 * @param name Display name (e.g., "Widget 001")
 * @param description Brief description extracted from preset.json
 * @param fileName Name of the .kwgt/.klwp file in assets
 * @param previewPath Absolute path to the extracted preview image
 * @param isKlwp Whether this is a KLWP wallpaper file (vs KWGT widget)
 */
data class MintWidgetItem(
    val id: String,
    val name: String,
    val description: String,
    val fileName: String,
    val previewPath: String? = null,
    val isKlwp: Boolean = false
)
