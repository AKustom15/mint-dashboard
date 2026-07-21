import { GoogleAuth } from "npm:google-auth-library@9.0.0";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const bodyText = await req.text();
    let integrityToken = "";
    if (bodyText) {
        const bodyJson = JSON.parse(bodyText);
        integrityToken = bodyJson.integrityToken;
    }

    if (!integrityToken) {
      return new Response(JSON.stringify({ error: "Missing integrityToken" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const packageName = Deno.env.get("APP_PACKAGE_NAME") || "com.akustom15.glasswave";
    const serviceAccountJsonStr = Deno.env.get("GOOGLE_SERVICE_ACCOUNT_JSON");
    
    if (!serviceAccountJsonStr) {
       console.error("Missing GOOGLE_SERVICE_ACCOUNT_JSON in environment variables");
       return new Response(JSON.stringify({ 
           licensed: false, 
           verdict: "UNKNOWN", 
           error: "Server configuration error" 
       }), { headers: { ...corsHeaders, "Content-Type": "application/json" } });
    }

    const credentials = JSON.parse(serviceAccountJsonStr);
    
    const auth = new GoogleAuth({
      credentials,
      scopes: ["https://www.googleapis.com/auth/playintegrity"],
    });

    const authClient = await auth.getClient();
    const token = await authClient.getAccessToken();

    const url = `https://playintegrity.googleapis.com/v1/${packageName}:decodeIntegrityToken`;
    
    const res = await fetch(url, {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${token.token}`,
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ integrityToken: integrityToken })
    });

    const resJson = await res.json();
    const payload = resJson.tokenPayloadExternal || {};
    const verdict = (payload.accountDetails && payload.accountDetails.appLicensingVerdict) || "UNEVALUATED";

    console.log("appLicensingVerdict:", verdict);
    
    return new Response(
      JSON.stringify({ licensed: verdict === "LICENSED", verdict: verdict }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );

  } catch (error) {
    console.error("verify failed:", error.message || error);
    return new Response(
      JSON.stringify({ licensed: false, verdict: "UNKNOWN", error: String(error) }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
