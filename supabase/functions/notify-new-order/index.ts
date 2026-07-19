import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const PROJECT_ID = "el-motamayz-library";

function toBase64Url(bytes: Uint8Array): string {
  let binary = "";
  bytes.forEach((b) => (binary += String.fromCharCode(b)));
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
}

async function getAccessToken(serviceAccount: Record<string, string>): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const payload = {
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now,
  };

  const enc = new TextEncoder();
  const headerB64 = toBase64Url(enc.encode(JSON.stringify(header)));
  const payloadB64 = toBase64Url(enc.encode(JSON.stringify(payload)));
  const sigInput = `${headerB64}.${payloadB64}`;

  // Parse PEM — handle both literal \n and real newlines
  const pem = serviceAccount.private_key.replace(/\\n/g, "\n");
  const pemBody = pem
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");

  const der = Uint8Array.from(atob(pemBody), (c) => c.charCodeAt(0));

  const key = await crypto.subtle.importKey(
    "pkcs8",
    der,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, enc.encode(sigInput));
  const jwt = `${sigInput}.${toBase64Url(new Uint8Array(sig))}`;

  const tokenRes = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });

  const tokenData = (await tokenRes.json()) as { access_token: string };
  return tokenData.access_token;
}

async function sendFcm(
  accessToken: string,
  token: string,
  title: string,
  body: string
): Promise<void> {
  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token,
          notification: { title, body },
          data: { navigate_to: "orders" },
          android: { priority: "high", notification: { channel_id: "orders_channel" } },
          webpush: {
            notification: { title, body, dir: "rtl", lang: "ar" },
            fcm_options: { link: "/" },
          },
        },
      }),
    }
  );
  if (!res.ok) {
    const err = await res.text();
    // Stale/invalid tokens are expected — log but don't throw
    console.warn(`FCM send failed [${token.substring(0, 20)}...]: ${err}`);
  }
}

Deno.serve(async (req: Request) => {
  try {
    const webhookBody = await req.json();
    const record = webhookBody.record ?? {};

    const customerName: string = record.customer_name ?? "عميل";
    const total: number | null = record.total ?? null;
    const notifTitle = "طلب جديد 🛒";
    const notifBody = total != null
      ? `${customerName} — ${total} ج`
      : customerName;

    const serviceAccountJson = Deno.env.get("FIREBASE_SERVICE_ACCOUNT");
    if (!serviceAccountJson) {
      return new Response("FIREBASE_SERVICE_ACCOUNT secret not set", { status: 500 });
    }
    const serviceAccount = JSON.parse(serviceAccountJson) as Record<string, string>;
    const accessToken = await getAccessToken(serviceAccount);

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    );

    const { data: tokens, error } = await supabase
      .from("push_tokens")
      .select("token");

    if (error) {
      console.error("DB error:", error);
      return new Response(String(error.message), { status: 500 });
    }
    if (!tokens || tokens.length === 0) {
      return new Response("No registered tokens", { status: 200 });
    }

    await Promise.allSettled(
      (tokens as { token: string }[]).map((t) =>
        sendFcm(accessToken, t.token, notifTitle, notifBody)
      )
    );

    return new Response(`Notified ${tokens.length} device(s)`, { status: 200 });
  } catch (e) {
    console.error("notify-new-order error:", e);
    return new Response(String(e), { status: 500 });
  }
});
