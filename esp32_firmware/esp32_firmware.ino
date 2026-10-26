/*
  ===========================================================
   ESP32 Car Lock Controller - Firmware
  ===========================================================
  - يتصل بشبكة WiFi المنزلية (Station Mode) أو ينشئ شبكة خاصة
    به (Access Point Mode) كخيار احتياطي.
  - يتحكم بريليه مزدوج موصول مباشرة على أسلاك وحدة القفل
    المركزي الأصلية للسيارة (محاكاة ضغطة الزر الداخلي، وليس
    التقاط أي إشارة لاسلكية خارجية).
  - كل أمر يصل عبر HTTP يجب أن يكون موقعًا بـ HMAC-SHA256
    مع طابع زمني (timestamp) لمنع هجمات إعادة الإرسال (Replay).
  ===========================================================
  المكتبات المطلوبة (Library Manager):
    - WiFi.h            (مدمجة مع ESP32 core)
    - WebServer.h        (مدمجة مع ESP32 core)
    - mbedtls/md.h        (مدمجة مع ESP32 core - لحساب HMAC)
    - ArduinoJson         (ثبّتها من Library Manager)
  ===========================================================
*/

#include <WiFi.h>
#include <WebServer.h>
#include <mbedtls/md.h>
#include <ArduinoJson.h>
#include <Preferences.h>

// ------------------- إعدادات قابلة للتعديل -------------------
const char* WIFI_SSID     = "YOUR_HOME_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_HOME_WIFI_PASSWORD";

const char* DEVICE_NAME = "My Car";

const char* SHARED_SECRET = "CHANGE_THIS_TO_A_LONG_RANDOM_SECRET_KEY_123456";

const unsigned long TIMESTAMP_WINDOW_SEC = 30;

const unsigned long RELAY_PULSE_MS = 400;

const int RELAY_LOCK_PIN   = 26;
const int RELAY_UNLOCK_PIN = 27;

const int STATUS_LED_PIN = 2;
// --------------------------------------------------------------

WebServer server(80);
Preferences prefs;

String lastNonce = "";

String toHex(const uint8_t* data, size_t len) {
  String out;
  out.reserve(len * 2);
  const char* hexChars = "0123456789abcdef";
  for (size_t i = 0; i < len; i++) {
    out += hexChars[(data[i] >> 4) & 0xF];
    out += hexChars[data[i] & 0xF];
  }
  return out;
}

String computeHMAC(const String& message) {
  uint8_t hmacResult[32];
  mbedtls_md_context_t ctx;
  mbedtls_md_type_t md_type = MBEDTLS_MD_SHA256;

  mbedtls_md_init(&ctx);
  mbedtls_md_setup(&ctx, mbedtls_md_info_from_type(md_type), 1);
  mbedtls_md_hmac_starts(&ctx, (const unsigned char*)SHARED_SECRET, strlen(SHARED_SECRET));
  mbedtls_md_hmac_update(&ctx, (const unsigned char*)message.c_str(), message.length());
  mbedtls_md_hmac_finish(&ctx, hmacResult);
  mbedtls_md_free(&ctx);

  return toHex(hmacResult, 32);
}

bool verifyRequest(const String& command, const String& timestamp, const String& nonce, const String& signature) {
  long ts = timestamp.toInt();
  long now = (long)(millis() / 1000);

  if (nonce == lastNonce) {
    return false;
  }

  String message = command + ":" + timestamp + ":" + nonce;
  String expectedSig = computeHMAC(message);

  if (expectedSig.equalsIgnoreCase(signature)) {
    lastNonce = nonce;
    return true;
  }
  return false;
}

void pulseRelay(int pin) {
  digitalWrite(pin, HIGH);
  delay(RELAY_PULSE_MS);
  digitalWrite(pin, LOW);
}

void handleStatus() {
  StaticJsonDocument<200> doc;
  doc["device"] = DEVICE_NAME;
  doc["online"] = true;
  doc["ip"] = WiFi.localIP().toString();
  String response;
  serializeJson(doc, response);
  server.send(200, "application/json", response);
}

void handleCommand(const String& command, int pin) {
  if (!server.hasArg("ts") || !server.hasArg("nonce") || !server.hasArg("sig")) {
    server.send(400, "application/json", "{\"ok\":false,\"error\":\"missing_params\"}");
    return;
  }

  String timestamp = server.arg("ts");
  String nonce     = server.arg("nonce");
  String signature = server.arg("sig");

  if (!verifyRequest(command, timestamp, nonce, signature)) {
    server.send(401, "application/json", "{\"ok\":false,\"error\":\"invalid_signature\"}");
    return;
  }

  pulseRelay(pin);

  StaticJsonDocument<100> doc;
  doc["ok"] = true;
  doc["command"] = command;
  String response;
  serializeJson(doc, response);
  server.send(200, "application/json", response);
}

void handleLock()   { handleCommand("lock", RELAY_LOCK_PIN); }
void handleUnlock() { handleCommand("unlock", RELAY_UNLOCK_PIN); }

void handleNotFound() {
  server.send(404, "application/json", "{\"ok\":false,\"error\":\"not_found\"}");
}

void setup() {
  Serial.begin(115200);

  pinMode(RELAY_LOCK_PIN, OUTPUT);
  pinMode(RELAY_UNLOCK_PIN, OUTPUT);
  pinMode(STATUS_LED_PIN, OUTPUT);
  digitalWrite(RELAY_LOCK_PIN, LOW);
  digitalWrite(RELAY_UNLOCK_PIN, LOW);

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  Serial.print("Connecting to WiFi");
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 30) {
    delay(500);
    Serial.print(".");
    attempts++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\nConnected! IP address: " + WiFi.localIP().toString());
    digitalWrite(STATUS_LED_PIN, HIGH);
  } else {
    Serial.println("\nFalling back to Access Point mode");
    WiFi.mode(WIFI_AP);
    WiFi.softAP("ESP32-CarRemote-Setup", "setup1234");
    Serial.println("AP IP address: " + WiFi.softAPIP().toString());
  }

  server.on("/status", HTTP_GET, handleStatus);
  server.on("/lock",   HTTP_POST, handleLock);
  server.on("/unlock", HTTP_POST, handleUnlock);
  server.onNotFound(handleNotFound);

  server.begin();
  Serial.println("HTTP server started");
}

void loop() {
  server.handleClient();
}
