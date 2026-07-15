package com.akustom15.mint.library.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LauncherInfo(
    val name: String,
    val packageName: String,
    val intentAction: String,
    val extraKey: String = "package"
) : Parcelable

object LaunchersRepository {
    val supportedLaunchers = listOf(
        LauncherInfo("Nova Launcher", "com.teslacoilsw.launcher", "com.teslacoilsw.launcher.APPLY_ICON_THEME"),
        LauncherInfo("Action Launcher", "com.actionlauncher.playstore", "com.actionlauncher.applytheme"),
        LauncherInfo("Apex Launcher", "com.anddoes.launcher", "com.anddoes.launcher.APPLY_ICON_THEME", "com.anddoes.launcher.ICON_THEME_PACKAGE_NAME"),
        LauncherInfo("ADW Launcher", "org.adw.launcher", "org.adw.launcher.SET_THEME"),
        LauncherInfo("Smart Launcher", "ginlemon.flowerfree", "ginlemon.smartlauncher.setGSLTHEME", "package"),
        LauncherInfo("Smart Launcher 6", "ginlemon.smartlauncher", "ginlemon.smartlauncher.setGSLTHEME", "package"),
        LauncherInfo("Lawnchair", "ch.deletescape.lawnchair.plah", "ch.deletescape.lawnchair.APPLY_ICONS"),
        LauncherInfo("Lawnchair 2", "ch.deletescape.lawnchair", "ch.deletescape.lawnchair.APPLY_ICONS"),
        LauncherInfo("Lawnchair 14", "app.lawnchair", "app.lawnchair.APPLY_ICONS"),
        LauncherInfo("Poco Launcher", "com.mi.android.globallauncher", "com.mi.android.globallauncher.APPLY_ICONS"),
        LauncherInfo("Microsoft Launcher", "com.microsoft.launcher", "com.microsoft.launcher.APPLY_ICONS"),
        LauncherInfo("Niagara Launcher", "bitpit.launcher", "bitpit.launcher.APPLY_ICONS"),
        LauncherInfo("Total Launcher", "com.ss.launcher2", "com.ss.launcher2.APPLY_THEME"),
        LauncherInfo("Samsung One UI Home", "com.sec.android.app.launcher", "com.sec.android.app.launcher.APPLY_ICONS"),
        LauncherInfo("Pixel Launcher", "com.google.android.apps.nexuslauncher", ""),
        LauncherInfo("OnePlus Launcher", "net.oneplus.launcher", "net.oneplus.launcher.APPLY_ICONS"),
        LauncherInfo("Hyperion Launcher", "projekt.launcher", "projekt.launcher.APPLY_ICONS"),
        LauncherInfo("Kiss Launcher", "fr.neamar.kiss", "fr.neamar.kiss.APPLY_ICONS")
    )
}
