# KWGT Widget Files

Place your `.kwgt` widget preset files in this folder.

## File naming convention
- Use descriptive names: `MyPack_001.kwgt`, `MyPack_002.kwgt`, etc.
- The file name (without extension) is used as the display name in the app.
- Underscores (`_`) are replaced with spaces for display.

## How it works
1. Place `.kwgt` files here (and optionally `.klwp` files for KLWP wallpapers).
2. The app automatically lists them in the Widgets tab.
3. Preview thumbnails are extracted from inside each `.kwgt` ZIP file
   (`preset_thumb_portrait.png` or `preset_thumb_landscape.png`).
4. When the user taps "Apply", KWGT opens with the preset loaded via `kfile://` URI.

## Creating .kwgt files
1. Open KWGT app on your device.
2. Design your widget.
3. Export the preset as a `.kwgt` file.
4. Copy it to this folder.
