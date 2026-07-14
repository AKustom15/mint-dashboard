package com.akustom15.mint.library.security

import android.content.Context
import android.util.Log

/**
 * Gestor de verificación del origen de instalación de la aplicación. Utilizado para restringir
 * funcionalidades premium en instalaciones no oficiales.
 */
object InstallSourceManager {
    private const val TAG = "InstallSourceManager"

    // Instaladores oficiales permitidos
    private val OFFICIAL_INSTALLERS =
            listOf(
                    "com.android.vending", // Google Play Store
                    "com.google.android.feedback" // Google Play Internal Testing
            )

    // Cache del resultado para evitar verificaciones repetidas
    private var cachedResult: Boolean? = null
    private var installerPackage: String? = null

    /**
     * Verifica si la aplicación fue instalada desde una fuente oficial. Fuentes oficiales
     * reconocidas:
     * - com.android.vending (Google Play Store)
     * - com.google.android.feedback (Google Play Internal Testing)
     *
     * NOTA: En builds DEBUG, siempre retorna true para permitir testing desde Android Studio. En
     * builds RELEASE (producción), verifica el instalador real.
     *
     * @return true si la instalación es oficial, false en caso contrario
     */
    fun isOfficialInstall(context: Context): Boolean {
        // 🔧 En builds DEBUG (desarrollo), deshabilitar verificación
        if (com.akustom15.mint.library.BuildConfig.DEBUG) {
            Log.d(
                    TAG,
                    "⚙️ DEBUG build detectado - verificación de instalador DESHABILITADA para testing"
            )
            return true // Permitir siempre en desarrollo
        }

        // Retornar resultado cacheado si existe
        cachedResult?.let {
            Log.d(TAG, "Usando resultado cacheado: $it")
            return it
        }

        return try {
            val packageName = context.packageName
            val installer =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        // API 30+
                        context.packageManager.getInstallSourceInfo(packageName)
                                .installingPackageName
                    } else {
                        // API < 30
                        @Suppress("DEPRECATION")
                        context.packageManager.getInstallerPackageName(packageName)
                    }

            installerPackage = installer
            val isOfficial = installer in OFFICIAL_INSTALLERS

            Log.i(TAG, "📦 Instalador detectado: ${installer ?: "desconocido"}")
            Log.i(TAG, "✅ Es instalación oficial: $isOfficial")

            // Cachear resultado
            cachedResult = isOfficial
            isOfficial
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verificando instalador", e)
            // En caso de error, asumimos instalación no oficial por seguridad
            cachedResult = false
            false
        }
    }

    /** Obtiene el nombre del paquete instalador. */
    fun getInstallerPackage(context: Context): String? {
        if (installerPackage == null) {
            isOfficialInstall(context) // Esto cacheará el instalador
        }
        return installerPackage
    }

    /** Limpia el cache. Útil para testing. */
    fun clearCache() {
        cachedResult = null
        installerPackage = null
        Log.d(TAG, "🔄 Cache limpiado")
    }

    /** Mensaje informativo para mostrar cuando las funciones están restringidas. */
    fun getRestrictionMessage(): String {
        return "Esta funcionalidad solo está disponible en la versión oficial de Play Store"
    }
}
