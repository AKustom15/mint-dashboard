package com.akustom15.mint.library.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * Utility class for integrating with Kustom apps (KWGT and KLWP).
 * Follows the official Kustom documentation for applying widgets and wallpapers.
 */
object MintKustomIntegration {

    private const val TAG = "MintKustomIntegration"
    private const val KWGT_PACKAGE = "org.kustom.widget"
    private const val KWGT_PRO_PACKAGE = "org.kustom.widget.pro"
    private const val KLWP_PACKAGE = "org.kustom.wallpaper"
    private const val KLWP_PRO_PACKAGE = "org.kustom.wallpaper.pro"

    /**
     * Apply a widget preset to KWGT.
     * Uses kfile:// URI scheme: kfile://{packageName}/widgets/{fileName}
     *
     * KWGT Pro is just an unlock key — all activities live in the base
     * org.kustom.widget package. We use getLaunchIntentForPackage to get
     * the real exported activity and attach the kfile:// data URI to it.
     */
    fun applyWidget(context: Context, widgetFileName: String, packageName: String) {
        Log.d(TAG, "applyWidget: file=$widgetFileName, package=$packageName")

        if (!isKwgtInstalled(context)) {
            Toast.makeText(
                context,
                "KWGT is not installed. Please install it first.",
                Toast.LENGTH_LONG
            ).show()
            openPlayStore(context, KWGT_PACKAGE)
            return
        }

        val kfileUri = Uri.parse("kfile://$packageName/widgets/$widgetFileName")
        Log.d(TAG, "URI: $kfileUri")

        launchKustomApp(context, KWGT_PACKAGE, kfileUri)
    }

    /**
     * Apply a wallpaper preset to KLWP.
     * Uses kfile:// URI scheme: kfile://{packageName}/wallpapers/{fileName}
     */
    fun applyWallpaper(context: Context, wallpaperFileName: String, packageName: String) {
        Log.d(TAG, "applyWallpaper: file=$wallpaperFileName, package=$packageName")

        if (!isKlwpInstalled(context)) {
            Toast.makeText(
                context,
                "KLWP is not installed. Please install it first.",
                Toast.LENGTH_LONG
            ).show()
            openPlayStore(context, KLWP_PACKAGE)
            return
        }

        val kfileUri = Uri.parse("kfile://$packageName/wallpapers/$wallpaperFileName")
        Log.d(TAG, "URI: $kfileUri")

        launchKustomApp(context, KLWP_PACKAGE, kfileUri)
    }

    /**
     * Launch a Kustom app (KWGT or KLWP) with a preset kfile:// URI.
     * Gets the real launch intent from PackageManager so we always target
     * the correct exported activity regardless of KWGT/KLWP version.
     */
    private fun launchKustomApp(context: Context, basePackage: String, kfileUri: Uri) {
        try {
            // Get the real launch activity from the base package
            val launchIntent = context.packageManager.getLaunchIntentForPackage(basePackage)

            if (launchIntent != null) {
                launchIntent.data = kfileUri
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                Log.d(TAG, "Kustom app started: $basePackage with $kfileUri")
            } else {
                Log.e(TAG, "No launch intent found for $basePackage")
                Toast.makeText(context, "Cannot open Kustom app", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Kustom app ($basePackage)", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** Check if KWGT is installed on the device */
    fun isKwgtInstalled(context: Context): Boolean {
        return isPackageInstalled(context, KWGT_PACKAGE) ||
                isPackageInstalled(context, KWGT_PRO_PACKAGE)
    }

    /** Check if KLWP is installed on the device */
    fun isKlwpInstalled(context: Context): Boolean {
        return isPackageInstalled(context, KLWP_PACKAGE) ||
                isPackageInstalled(context, KLWP_PRO_PACKAGE)
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun openPlayStore(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
