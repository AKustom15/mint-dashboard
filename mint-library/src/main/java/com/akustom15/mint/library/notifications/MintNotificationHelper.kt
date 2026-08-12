package com.akustom15.mint.library.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.akustom15.mint.library.R
import com.google.firebase.messaging.FirebaseMessaging
import java.net.URL

/**
 * Handles notification channel creation, FCM topic subscription,
 * and displaying push notifications with custom sound.
 */
object MintNotificationHelper {

    private const val TAG = "MintNotificationHelper"
    const val CHANNEL_ID = "mint_push_channel"
    private const val CHANNEL_NAME = "Updates"
    private const val CHANNEL_DESC = "App updates and news"

    /** Intent extra: if true, the host Activity should navigate to the notification history screen. */
    const val EXTRA_OPEN_NOTIFICATIONS = "mint_open_notifications"

    /**
     * Initialize push notifications: create channel + subscribe to FCM topic.
     * Call this from Activity.onCreate().
     */
    fun initialize(context: Context) {
        createNotificationChannel(context)
        syncSubscription(context)
    }

    /**
     * Create the notification channel with custom sound (Android 8+).
     */
    private fun createNotificationChannel(context: Context) {
        val soundUri = Uri.parse(
            "android.resource://${context.packageName}/${R.raw.new_notification_011}"
        )

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC
            setSound(soundUri, audioAttributes)
            enableVibration(true)
            enableLights(true)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel created: $CHANNEL_ID")
    }

    /**
     * Subscribe/unsubscribe to FCM topic based on user preference.
     */
    fun syncSubscription(context: Context) {
        val prefs = MintNotificationPreferences.getInstance(context)
        val enabled = prefs.areNotificationsEnabled()
        
        // Topic uniquely identifies the app package, replacing dots with underscores for FCM format
        val packageTopic = context.packageName.replace(".", "_")

        // Unsubscribe from legacy "all" topic to prevent duplicates
        FirebaseMessaging.getInstance().unsubscribeFromTopic("all")

        if (enabled) {
            FirebaseMessaging.getInstance().subscribeToTopic(packageTopic)
                .addOnSuccessListener { Log.d(TAG, "Subscribed to topic: $packageTopic") }
                .addOnFailureListener { Log.e(TAG, "Failed to subscribe", it) }
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(packageTopic)
                .addOnSuccessListener { Log.d(TAG, "Unsubscribed from topic: $packageTopic") }
                .addOnFailureListener { Log.e(TAG, "Failed to unsubscribe", it) }
        }
    }

    /**
     * Show a local notification (called by MintMessagingService when FCM arrives).
     */
    fun showNotification(context: Context, title: String, body: String, imageUrl: String? = null) {
        val soundUri = Uri.parse(
            "android.resource://${context.packageName}/${R.raw.new_notification_011}"
        )

        // Open the app and navigate directly to the notification history screen
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, System.currentTimeMillis().toInt(),
            launchIntent ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bitmap: Bitmap? =
            if (!imageUrl.isNullOrBlank()) downloadScaledBitmap(imageUrl) else null

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.mint_ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?) // Hide large icon when expanded
            )
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    /**
     * Ancho/alto máximo del bitmap de la notificación, en píxeles.
     *
     * Una notificación con imagen grande se muestra a unos 2:1 y como mucho al
     * ancho de la pantalla. 1024 px cubre de sobra incluso pantallas grandes.
     */
    private const val MAX_NOTIFICATION_IMAGE_PX = 1024

    /**
     * Descarga la imagen de la notificación y la decodifica REDUCIDA.
     *
     * Antes se hacía `BitmapFactory.decodeStream(input)` a secas: la imagen se
     * decodificaba a resolución completa. Una foto de 4000x3000 ocupa ~48 MB en
     * memoria (ARGB_8888, 4 bytes por píxel), lo bastante para tumbar la app en
     * gama baja. Google Play lo señalaba en "Calidad técnica".
     *
     * Ahora se decodifica en dos pasadas:
     *   1. `inJustDecodeBounds` mide la imagen SIN reservar memoria de píxeles.
     *   2. Se calcula `inSampleSize` y se decodifica ya reducida.
     *
     * Esa foto de 4000x3000 pasa a decodificarse a 1000x750 → ~3 MB. Y como la
     * notificación no la muestra más grande, no se pierde nada visible.
     */
    private fun downloadScaledBitmap(imageUrl: String): Bitmap? {
        var connection: java.net.HttpURLConnection? = null
        return try {
            connection = (URL(imageUrl).openConnection() as java.net.HttpURLConnection).apply {
                doInput = true
                connectTimeout = 10_000
                readTimeout = 15_000
            }
            // Se lee a memoria porque hacen falta DOS pasadas y un flujo de red
            // no se puede rebobinar. Son los bytes comprimidos: unos cientos de KB.
            val bytes = connection.inputStream.use { it.readBytes() }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download notification image", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    /** Potencia de 2 más pequeña que deja la imagen por debajo del máximo. */
    private fun calculateInSampleSize(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sampleSize = 1
        while (width / sampleSize > MAX_NOTIFICATION_IMAGE_PX ||
               height / sampleSize > MAX_NOTIFICATION_IMAGE_PX
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
