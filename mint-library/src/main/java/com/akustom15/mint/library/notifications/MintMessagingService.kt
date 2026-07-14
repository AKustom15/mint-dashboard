package com.akustom15.mint.library.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging service that receives push notifications,
 * stores them in history, and shows a local notification with custom sound.
 *
 * Consumer apps should register this in their AndroidManifest.xml:
 * <service android:name="com.akustom15.mint.library.notifications.MintMessagingService"
 *     android:exported="false">
 *     <intent-filter>
 *         <action android:name="com.google.firebase.MESSAGING_EVENT" />
 *     </intent-filter>
 * </service>
 */
class MintMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MintMessagingService"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: ""
        val body = message.notification?.body ?: message.data["body"] ?: ""

        Log.d(TAG, "Push received: title=$title, body=$body")

        if (title.isBlank() && body.isBlank()) return

        val prefs = MintNotificationPreferences.getInstance(this)

        // Always save to history
        prefs.saveNotification(title, body)

        // Only show notification if user has them enabled
        if (prefs.areNotificationsEnabled()) {
            MintNotificationHelper.showNotification(this, title, body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed: $token")
    }
}
