/**
 * Cloud Function: verifyAppLicense
 * ---------------------------------
 * Server-side paid-app ownership check using the Google Play Integrity API.
 *
 * The Android client (MintLicenseVerifier) sends a Play Integrity token. Here we
 * decode it WITH GOOGLE (the token is signed for your Google Cloud project, so a
 * patched client cannot forge it) and read `accountDetails.appLicensingVerdict`:
 *   - "LICENSED"    → the Google account owns (bought) the app  ✅
 *   - "UNLICENSED"  → the account did NOT buy the app           ❌
 *   - "UNEVALUATED" → couldn't be evaluated (treated as unknown)
 *
 * Deploy this in the SAME Firebase project as the app.
 */
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { google } = require("googleapis");

// ⚠️ Set this to YOUR app's applicationId (must match the Play listing).
const PACKAGE_NAME = process.env.APP_PACKAGE_NAME || "com.akustom15.glasswave";

exports.verifyAppLicense = onCall(
  {
    // Turn this ON once App Check is registered and confirmed working:
    // enforceAppCheck: true,
    region: "us-central1",
  },
  async (request) => {
    const token = request.data && request.data.integrityToken;
    if (!token) {
      throw new HttpsError("invalid-argument", "Missing integrityToken");
    }

    try {
      const auth = new google.auth.GoogleAuth({
        scopes: ["https://www.googleapis.com/auth/playintegrity"],
      });
      const authClient = await auth.getClient();
      const playintegrity = google.playintegrity({ version: "v1", auth: authClient });

      const res = await playintegrity.v1.decodeIntegrityToken({
        packageName: PACKAGE_NAME,
        requestBody: { integrityToken: token },
      });

      const payload = (res.data && res.data.tokenPayloadExternal) || {};
      const verdict =
        (payload.accountDetails && payload.accountDetails.appLicensingVerdict) ||
        "UNEVALUATED";

      console.log("appLicensingVerdict:", verdict);
      return { licensed: verdict === "LICENSED", verdict };
    } catch (e) {
      // Fail OPEN on server/API errors so a real buyer isn't locked out by an
      // outage. The client treats "UNKNOWN" as non-blocking.
      console.error("decodeIntegrityToken failed:", e && e.message);
      return { licensed: false, verdict: "UNKNOWN", error: String((e && e.message) || e) };
    }
  }
);
