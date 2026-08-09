package com.akustom15.mint.library.data

import android.content.Context
import java.util.UUID

/**
 * Identificador de instalación: aleatorio, propio de la app, sin relación con
 * el hardware.
 *
 * Sustituye a `Settings.Secure.ANDROID_ID`, que es un identificador PERSISTENTE
 * DE DISPOSITIVO: dato personal bajo GDPR, declarable en Data Safety de Play, y
 * que complica el derecho de supresión (no se puede "borrar" un ANDROID_ID).
 *
 * Este se borra al desinstalar, que es exactamente el comportamiento deseable.
 *
 * Nota: se adelantó de la fase 3 a la 2 porque el servidor necesita una clave de
 * documento y no tenía sentido estrenar el endpoint nuevo perpetuando el
 * identificador viejo.
 */
object MintInstallId {

    private const val PREFS = "mint_install"
    private const val KEY = "install_id"

    @Volatile
    private var cached: String? = null

    fun get(context: Context): String {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val prefs = context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val existing = prefs.getString(KEY, null)
            val id = existing ?: UUID.randomUUID().toString().also {
                prefs.edit().putString(KEY, it).apply()
            }
            cached = id
            return id
        }
    }
}
