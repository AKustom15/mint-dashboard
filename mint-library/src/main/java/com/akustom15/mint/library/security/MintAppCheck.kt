package com.akustom15.mint.library.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.tasks.await

/**
 * Inicializa Firebase App Check.
 *
 * App Check hace que los backends de Firebase (Firestore, FCM…) y nuestras edge
 * functions rechacen peticiones que no vengan de la app genuina, firmada por
 * Play. Combinado con reglas que exigen `request.app != null`, impide que un
 * script con la API key del APK toque la base de datos.
 *
 * ── Builds de release ──
 * Proveedor Play Integrity. Funciona solo si la app se instaló desde Play.
 *
 * ── Builds de debug ──
 * Play Integrity SIEMPRE falla en debug (firma distinta, instalación por ADB),
 * así que Firestore denegaría todo y no podrías desarrollar. Por eso, si la app
 * es depurable, se instala el proveedor de depuración, que imprime en Logcat un
 * secreto que hay que registrar en la consola:
 *
 *     Firebase Console → App Check → tu app → ⋮ → Administrar tokens de depuración
 *
 * El proveedor de depuración se carga por reflexión, así que la librería no
 * necesita depender de él. Para activarlo, la app consumidora añade:
 *
 *     debugImplementation("com.google.firebase:firebase-appcheck-debug")
 *
 * Si esa dependencia no está, se cae con elegancia a Play Integrity.
 */
object MintAppCheck {

    private const val TAG = "MintAppCheck"

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        try {
            val isDebuggable =
                (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

            val factory: AppCheckProviderFactory =
                if (isDebuggable) {
                    debugFactory() ?: PlayIntegrityAppCheckProviderFactory.getInstance()
                } else {
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
            initialized = true
        } catch (e: Exception) {
            // Nunca tumbar la app si Firebase no está disponible.
            Log.e(TAG, "No se pudo inicializar App Check", e)
        }
    }

    /**
     * Carga DebugAppCheckProviderFactory por reflexión.
     * Devuelve null si el artefacto firebase-appcheck-debug no está presente.
     */
    private fun debugFactory(): AppCheckProviderFactory? = try {
        val cls = Class.forName(
            "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory"
        )
        cls.getMethod("getInstance").invoke(null) as AppCheckProviderFactory
    } catch (e: Throwable) {
        Log.w(TAG, "Proveedor de depuración no disponible; se usa Play Integrity. " +
                "Añade debugImplementation(\"com.google.firebase:firebase-appcheck-debug\") " +
                "para poder desarrollar contra Firestore.")
        null
    }

    /** Token actual de App Check, o null. Lo usan las llamadas al backend. */
    suspend fun token(): String? = try {
        FirebaseAppCheck.getInstance()
            .getAppCheckToken(false)
            .await()
            .token
            .takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }
}
