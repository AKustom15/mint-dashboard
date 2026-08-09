package com.akustom15.mint.library.ui.screens.request

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akustom15.mint.library.billing.MintPremiumPreferences
import com.akustom15.mint.library.data.AppFilterCache
import com.akustom15.mint.library.data.MintInstallId
import com.akustom15.mint.library.security.MintIconRequestGate
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.min

data class MissingIconApp(
    val appName: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable?,
    val isSelected: Boolean = false,
    val alreadyRequested: Boolean = false
)

data class RemoteIconConfig(
    val allowFreeRequests: Boolean = true,
    val allowPremiumRequests: Boolean = true,
    val pauseMessage: String = ""
)

data class IconRequestUiState(
    val missingApps: List<MissingIconApp> = emptyList(),
    val isLoading: Boolean = true,
    val selectedCount: Int = 0,
    val freeLimit: Int = 10,
    val isSending: Boolean = false,
    val requestSent: Boolean = false,
    val error: String? = null,
    val requestedIcons: Set<String> = emptySet(),
    val totalAvailable: Int = 10,
    val remoteConfig: RemoteIconConfig = RemoteIconConfig()
)

class IconRequestViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(IconRequestUiState())
    val uiState: StateFlow<IconRequestUiState> = _uiState.asStateFlow()

    private var appName: String = "Mint Icons"
    private var emailAddress: String = "akustom15help@gmail.com"
    private var firestoreCollection: String = "icon_requests"
    private var nonceUrl: String = ""
    private var iconRequestUrl: String = ""
    private var iconStatusUrl: String = ""
    private var appVersionName: String = ""

    /** Identificador firmado por el servidor de la última solicitud aceptada. */
    private var lastRequestId: String? = null

    fun configure(
        appName: String,
        email: String,
        collection: String,
        nonceUrl: String = "",
        iconRequestUrl: String = "",
        iconStatusUrl: String = "",
        appVersionName: String = ""
    ) {
        this.appName = appName
        this.emailAddress = email
        this.firestoreCollection = collection
        this.nonceUrl = nonceUrl
        this.iconRequestUrl = iconRequestUrl
        this.iconStatusUrl = iconStatusUrl
        this.appVersionName = appVersionName
    }

    fun loadMissingIcons(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // La cuenta la lleva el SERVIDOR, así que se le pregunta a él.
            // Antes esto se leía directamente de Firestore desde el cliente y,
            // si la lectura fallaba, el catch devolvía una lista vacía: la app
            // mostraba la cuota entera disponible aunque estuviera agotada.
            val status = MintIconRequestGate(
                context, nonceUrl, iconRequestUrl, iconStatusUrl
            ).status()

            val requestedIcons = status?.requested ?: emptySet()

            val missing = withContext(Dispatchers.IO) {
                findMissingIcons(context, requestedIcons)
            }

            if (status == null) {
                // Fail-closed también aquí: si no sabemos cuántas lleva, NO se
                // le ofrece la cuota completa. Mostrar 10 disponibles sin
                // haberlo comprobado es lo que causaba el fallo anterior.
                _uiState.value = _uiState.value.copy(
                    missingApps = missing,
                    isLoading = false,
                    requestedIcons = emptySet(),
                    totalAvailable = 0,
                    error = "No se pudieron comprobar tus solicitudes disponibles. " +
                            "Revisa tu conexión e inténtalo de nuevo."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                missingApps = missing,
                isLoading = false,
                requestedIcons = requestedIcons,
                totalAvailable = status.freeRemaining + premiumAvailableCount
            )
        }
    }

    fun loadRemoteConfig(urlStr: String) {
        if (urlStr.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL(urlStr)
                val connection = url.openConnection() as java.net.HttpURLConnection
                
                // Parse ?token= parameter if it exists (for private GitHub repos)
                val query = url.query
                if (query != null && query.contains("token=")) {
                    val token = query.split("&").firstOrNull { it.startsWith("token=") }?.substringAfter("token=")
                    if (token != null) {
                        connection.setRequestProperty("Authorization", "token $token")
                    }
                }
                
                connection.requestMethod = "GET"
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                
                val jsonObject = JSONObject(jsonString)
                val config = RemoteIconConfig(
                    allowFreeRequests = jsonObject.optBoolean("allow_free_requests", true),
                    allowPremiumRequests = jsonObject.optBoolean("allow_premium_requests", true),
                    pauseMessage = jsonObject.optString("pause_message", "")
                )
                _uiState.value = _uiState.value.copy(remoteConfig = config)
            } catch (e: Exception) {
                Log.e("IconRequest", "Error loading remote config: ${e.message}")
            }
        }
    }

    fun toggleSelection(index: Int) {
        val current = _uiState.value.missingApps.toMutableList()
        if (index in current.indices) {
            val app = current[index]
            if (app.alreadyRequested) return

            val newSelected = !app.isSelected
            val currentSelected = _uiState.value.selectedCount

            if (newSelected && currentSelected >= _uiState.value.totalAvailable) {
                return
            }

            current[index] = app.copy(isSelected = newSelected)
            _uiState.value = _uiState.value.copy(
                missingApps = current,
                selectedCount = if (newSelected) currentSelected + 1 else currentSelected - 1
            )
        }
    }

    fun selectAll() {
        val limit = _uiState.value.totalAvailable
        val current = _uiState.value.missingApps
        var count = 0
        val newList = current.map { app ->
            if (!app.alreadyRequested && count < limit) {
                count++
                app.copy(isSelected = true)
            } else {
                app.copy(isSelected = false)
            }
        }
        _uiState.value = _uiState.value.copy(
            missingApps = newList,
            selectedCount = count
        )
    }

    fun deselectAll() {
        val current = _uiState.value.missingApps.map { it.copy(isSelected = false) }
        _uiState.value = _uiState.value.copy(
            missingApps = current,
            selectedCount = 0
        )
    }

    private var premiumAvailableCount: Int = 0

    fun setFreeLimit(limit: Int) {
        _uiState.value = _uiState.value.copy(freeLimit = limit)
    }

    fun setPremiumAvailable(count: Int) {
        premiumAvailableCount = count
        recalculateAvailable()
    }

    private fun recalculateAvailable() {
        val freeUsed = kotlin.math.min(
            _uiState.value.requestedIcons.size,
            _uiState.value.freeLimit
        )
        val freeRemaining = kotlin.math.max(0, _uiState.value.freeLimit - freeUsed)
        _uiState.value = _uiState.value.copy(
            totalAvailable = freeRemaining + premiumAvailableCount
        )
    }

    /**
     * Sends icon request email with ZIP attachment (appfilter.xml, appmap.xml, icons PNG)
     * and saves to Firestore. Replicates GlassWave behavior exactly.
     */
    fun sendIconRequest(context: Context, antiPiracyStatus: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)

            try {
                val selectedApps = _uiState.value.missingApps.filter { it.isSelected }
                if (selectedApps.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isSending = false)
                    return@launch
                }

                // PRIMERO: el servidor valida y registra. Si dice que no, no hay
                // email. Fail-closed: sin confirmación no se entrega el servicio.
                val ok = submitToServer(context, selectedApps.map { it.packageName }.toSet())
                if (!ok) {
                    _uiState.value = _uiState.value.copy(isSending = false)
                    return@launch
                }

                // DESPUÉS: si aceptó, se genera el ZIP y se abre el email
                withContext(Dispatchers.IO) {
                    shareIconRequests(context, selectedApps, antiPiracyStatus)
                }

                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    requestSent = true
                )
            } catch (e: Exception) {
                Log.e("IconRequest", "Error sending request")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "No se pudo completar la solicitud. Revisa tu conexión e inténtalo de nuevo.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                _uiState.value = _uiState.value.copy(isSending = false)
            }
        }
    }

    /**
     * Pide permiso al servidor y deja que sea él quien escriba en Firestore.
     *
     * Antes esto lo hacía el cliente: leía su propio documento, contaba su
     * cuota y la escribía. Cualquiera con el APK podía resetearla o pedir
     * iconos ilimitados sin haber comprado la app.
     *
     * @return true si el servidor aceptó la solicitud.
     */
    private suspend fun submitToServer(context: Context, newPackages: Set<String>): Boolean {
        val gate = MintIconRequestGate(context, nonceUrl, iconRequestUrl, iconStatusUrl)

        val result = gate.request(
            packages = newPackages,
            premiumCredits = premiumAvailableCount,
            appVersion = appVersionName
        )

        return when (result) {
            is MintIconRequestGate.Result.Accepted -> {
                lastRequestId = result.requestId

                // Los créditos premium que el servidor dice haber consumido.
                if (result.premiumConsumed > 0) {
                    MintPremiumPreferences.consumePremiumRequests(context, result.premiumConsumed)
                    premiumAvailableCount = MintPremiumPreferences.getPremiumRequestCount(context)
                }

                val allRequested = _uiState.value.requestedIcons + newPackages
                _uiState.value = _uiState.value.copy(
                    requestedIcons = allRequested,
                    totalAvailable = result.freeRemaining + premiumAvailableCount,
                    selectedCount = 0,
                    missingApps = _uiState.value.missingApps.map { app ->
                        if (newPackages.contains(app.packageName)) {
                            app.copy(isSelected = false, alreadyRequested = true)
                        } else {
                            app.copy(isSelected = false)
                        }
                    }
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Solicitud verificada: ${newPackages.size} iconos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true
            }

            is MintIconRequestGate.Result.QuotaExceeded -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "No te quedan solicitudes disponibles.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                false
            }

            is MintIconRequestGate.Result.Unavailable -> {
                // Mensaje NEUTRO a propósito. No se acusa a nadie de piratería:
                // a un comprador con mala conexión sería un falso positivo caro,
                // y a un atacante no se le dice qué comprobación falló.
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "No se pudo verificar la solicitud en este momento. Inténtalo más tarde.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                false
            }
        }
    }

    private fun shareIconRequests(context: Context, selectedApps: List<MissingIconApp>, antiPiracyStatus: String?) {
        // 1) Prepare temp payload directory
        //
        // Todo vive bajo cacheDir/icon_request/, que es la ÚNICA ruta que el
        // FileProvider expone (file_paths.xml). Antes el provider podía generar
        // URIs para cualquier archivo del almacenamiento externo.
        val shareDir = File(context.cacheDir, "icon_request").apply { mkdirs() }
        val payloadDir = File(shareDir, "payload").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        // 2) Generate appfilter.xml and appmap.xml
        val appfilterXml = generateAppfilterXml(selectedApps)
        File(payloadDir, "appfilter.xml").writeText(appfilterXml)

        val appmapXml = generateAppmapXml(selectedApps)
        File(payloadDir, "appmap.xml").writeText(appmapXml)

        // 3) Export app icons as PNG
        val iconsDir = File(payloadDir, "icons").apply { mkdirs() }
        exportAppIcons(context, selectedApps, iconsDir)

        // 4) Compress to ZIP
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val zipFile = File(shareDir, "${appName.replace(" ", "_")}_icon_request_${timestamp}.zip")
        createZipFromDirectory(payloadDir, zipFile)

        if (!zipFile.exists() || zipFile.length() == 0L) {
            throw Exception("Error creando el ZIP de solicitud")
        }

        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )
        
        val installId = MintInstallId.get(context)
        val model = android.os.Build.MODEL
        val androidVersion = "Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})"
        val orderId = MintPremiumPreferences.getLastProcessedOrderId(context)

        // Calculate if this batch uses premium requests
        val freeRequestsAlreadyUsed = kotlin.math.min(_uiState.value.requestedIcons.size, _uiState.value.freeLimit)
        val freeRequestsAvailableForBatch = kotlin.math.max(0, _uiState.value.freeLimit - freeRequestsAlreadyUsed)
        val isPremium = selectedApps.size > freeRequestsAvailableForBatch

        // Build email subject and body
        val subject = if (isPremium) {
            "🌟 Solicitud Premium - $appName (${selectedApps.size} apps)"
        } else {
            "Solicitud Gratuita - $appName (${selectedApps.size} apps)"
        }

        val emailBody = buildString {
            append("Icon request for $appName Icon Pack:\n\n")
            selectedApps.forEach { app ->
                val playStoreLink = "https://play.google.com/store/apps/details?id=${app.packageName}"
                append("• ${app.appName} (${app.packageName})\n")
                append("  Descargar/Ver: $playStoreLink\n\n")
            }
            append("(Los detalles completos de los componentes están en el archivo XML adjunto.)\n\n")
            append("--- Información Adicional ---\n")
            append("Modelo: $model\n")
            append("Versión de Android: $androidVersion\n")
            if (orderId != null) {
                append("Order ID: $orderId\n")
            }
            append("Install ID: $installId\n")
            append("License Status: ${antiPiracyStatus ?: "Unknown"}\n")
            // Identificador firmado por el servidor. Si falta o está vacío, la
            // solicitud NO pasó por la verificación y se puede ignorar.
            append("Request ID: ${lastRequestId ?: "SIN VERIFICAR"}\n")
        }

        // Create share intent with ZIP attachment
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, emailBody +
                "\nSe adjunta ZIP con: appfilter.xml, appmap.xml e iconos PNG de las apps seleccionadas.")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(
            Intent.createChooser(shareIntent, "Compartir solicitud de iconos")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun generateAppfilterXml(apps: List<MissingIconApp>): String {
        return buildString {
            append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            append("<resources>\n")
            append("<!-- Icon requests: appfilter.xml generado automáticamente -->\n")
            apps.forEach { app ->
                val drawableName = "icon_${normalizeName(app.appName)}"
                val componentInfo = "ComponentInfo{${app.packageName}/${app.activityName}}"
                val escapedName = escapeXml(app.appName)

                append("\n    <!-- $escapedName -->\n")
                append("    <item\n")
                append("        component=\"$componentInfo\"\n")
                append("        drawable=\"$drawableName\" />\n")
                append("    <item name=\"$escapedName\" drawable=\"$drawableName\" ")
                append("package=\"${app.packageName}\" activity=\"${app.activityName}\" />\n")
            }
            append("</resources>\n")
        }
    }

    private fun generateAppmapXml(apps: List<MissingIconApp>): String {
        return buildString {
            append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            append("<appmap>\n")
            append("    <!-- Icon requests: appmap.xml generado automáticamente -->\n")
            apps.forEach { app ->
                val drawableName = "icon_${normalizeName(app.appName)}"
                val escapedName = escapeXml(app.appName)
                append("    <item class=\"$escapedName\" name=\"$escapedName\" drawable=\"$drawableName\" />\n")
            }
            append("</appmap>\n")
        }
    }

    private fun exportAppIcons(context: Context, apps: List<MissingIconApp>, outputDir: File) {
        apps.forEach { app ->
            try {
                val drawable = app.icon ?: return@forEach
                val baseBitmap = drawableToBitmap(drawable)
                val targetSize = 512
                val scaledBitmap = if (baseBitmap.width != targetSize || baseBitmap.height != targetSize) {
                    Bitmap.createScaledBitmap(baseBitmap, targetSize, targetSize, true)
                } else baseBitmap

                val fileName = "icon_${normalizeName(app.appName)}.png"
                val outFile = File(outputDir, fileName)
                FileOutputStream(outFile).use { fos ->
                    BufferedOutputStream(fos).use { bos ->
                        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
                        bos.flush()
                    }
                }
            } catch (e: Exception) {
                Log.w("IconRequest", "Cannot export icon for ${app.packageName}", e)
            }
        }
    }

    private fun createZipFromDirectory(sourceDir: File, zipFile: File) {
        fun addFileToZip(baseDir: File, file: File, zos: ZipOutputStream) {
            val entryName = file.relativeTo(baseDir).path.replace('\\', '/')
            if (file.isDirectory) {
                val children = file.listFiles()
                if (children.isNullOrEmpty()) {
                    val dirEntry = ZipEntry(if (entryName.endsWith("/")) entryName else "$entryName/")
                    zos.putNextEntry(dirEntry)
                    zos.closeEntry()
                } else {
                    children.forEach { child -> addFileToZip(baseDir, child, zos) }
                }
            } else {
                FileInputStream(file).use { fis ->
                    val entry = ZipEntry(entryName)
                    zos.putNextEntry(entry)
                    fis.copyTo(zos, bufferSize = 8 * 1024)
                    zos.closeEntry()
                }
            }
        }

        if (zipFile.exists()) zipFile.delete()
        FileOutputStream(zipFile).use { fos ->
            ZipOutputStream(BufferedOutputStream(fos)).use { zos ->
                addFileToZip(sourceDir, sourceDir, zos)
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun normalizeName(name: String): String {
        return name.lowercase()
            .replace("\\s+".toRegex(), "_")
            .replace("[^a-z0-9_]".toRegex(), "")
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    private fun findMissingIcons(context: Context, requestedIcons: Set<String>): List<MissingIconApp> {
        val pm = context.packageManager
        val themedComponents = AppFilterCache.getThemedComponents(context)

        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val missing = mutableListOf<MissingIconApp>()

        for (appInfo in installedApps) {
            try {
                val packageName = appInfo.packageName
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val isUserApp = !isSystemApp || isUpdatedSystemApp

                if (!isUserApp) continue

                val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: continue
                val component = launchIntent.component ?: continue
                val activityName = component.className
                val componentNameKey = "$packageName/$activityName"

                // Skip if already themed
                if (themedComponents.contains(componentNameKey.lowercase())) continue

                val appLabel = appInfo.loadLabel(pm).toString()
                val icon = try { appInfo.loadIcon(pm) } catch (e: Exception) { null }
                val alreadyRequested = requestedIcons.contains(packageName)

                missing.add(
                    MissingIconApp(
                        appName = appLabel,
                        packageName = packageName,
                        activityName = activityName,
                        icon = icon,
                        alreadyRequested = alreadyRequested
                    )
                )
            } catch (e: Exception) {
                Log.w("IconRequest", "Error processing app ${appInfo.packageName}", e)
            }
        }

        return missing.sortedBy { it.appName.lowercase() }
    }
}
