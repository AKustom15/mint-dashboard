package com.akustom15.mint.library.security

import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Initializes Firebase App Check with the Play Integrity provider.
 *
 * App Check makes Firebase backends (Firestore, FCM, Storage…) reject requests
 * that don't come from your genuine, Play-signed app. Combined with Firestore
 * Security Rules that require `request.app != null`, this stops attackers from
 * hitting your database/FCM directly with scripts or repackaged builds.
 *
 * Call once, early (e.g. Application.onCreate or MainActivity.onCreate), AFTER
 * Firebase has initialized (the google-services plugin auto-initializes it).
 *
 * IMPORTANT (console steps, see instructions):
 *  - Register the app in Firebase Console → App Check with the Play Integrity provider.
 *  - For DEBUG builds, add a debug provider / debug token or App Check will fail.
 *  - Enable enforcement only after you confirm legit traffic passes.
 */
object MintAppCheck {

    private const val TAG = "MintAppCheck"

    fun initialize() {
        try {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "Firebase App Check initialized (Play Integrity provider)")
        } catch (e: Exception) {
            // Never crash the app if App Check/Firebase isn't available.
            Log.e(TAG, "Could not initialize App Check", e)
        }
    }
}
