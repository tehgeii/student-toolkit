package com.tehgeii.studenttoolkit.domain.tools

import java.net.URI
import java.net.URLEncoder

enum class QrType { TEXT, URL, WIFI, EMAIL, PHONE }

enum class WifiSecurity(val code: String) {
    WPA("WPA"),
    WEP("WEP"),
    NONE("nopass"),
}

enum class QrError { URL_SPACES, URL_INVALID, WIFI_PASSWORD_MISSING, WIFI_PASSWORD_SHORT, EMAIL_INVALID, PHONE_INVALID, TOO_LONG }

sealed interface QrPayloadResult {
    data class Ok(val payload: String) : QrPayloadResult
    data class Error(val error: QrError) : QrPayloadResult
    data object Empty : QrPayloadResult
}

/** Builds QR payload strings using the formats phone cameras understand. Everything stays on-device. */
object QrPayload {
    /** Byte-mode capacity of the largest QR code at low error correction. */
    const val MAX_BYTES = 2953

    fun text(text: String): QrPayloadResult = when {
        text.isBlank() -> QrPayloadResult.Empty
        else -> checkLength(text)
    }

    fun url(input: String): QrPayloadResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return QrPayloadResult.Empty
        if (trimmed.any { it.isWhitespace() }) return QrPayloadResult.Error(QrError.URL_SPACES)
        val withScheme = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        val host = runCatching { URI(withScheme).host }.getOrNull()
        if (host.isNullOrBlank() || (!host.contains('.') && host != "localhost")) {
            return QrPayloadResult.Error(QrError.URL_INVALID)
        }
        return checkLength(withScheme)
    }

    fun wifi(ssid: String, password: String, security: WifiSecurity, hidden: Boolean): QrPayloadResult {
        if (ssid.isEmpty()) return QrPayloadResult.Empty
        if (security != WifiSecurity.NONE && password.isEmpty()) return QrPayloadResult.Error(QrError.WIFI_PASSWORD_MISSING)
        if (security == WifiSecurity.WPA && password.length < 8) {
            return QrPayloadResult.Error(QrError.WIFI_PASSWORD_SHORT)
        }
        val builder = StringBuilder("WIFI:T:").append(security.code).append(';')
        builder.append("S:").append(escapeWifi(ssid)).append(';')
        if (security != WifiSecurity.NONE) builder.append("P:").append(escapeWifi(password)).append(';')
        if (hidden) builder.append("H:true;")
        builder.append(';')
        return checkLength(builder.toString())
    }

    fun email(to: String, subject: String, body: String): QrPayloadResult {
        val address = to.trim()
        if (address.isEmpty()) return QrPayloadResult.Empty
        if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(address)) return QrPayloadResult.Error(QrError.EMAIL_INVALID)
        val params = buildList {
            if (subject.isNotBlank()) add("subject=" + encode(subject))
            if (body.isNotBlank()) add("body=" + encode(body))
        }
        val payload = "mailto:$address" + if (params.isEmpty()) "" else "?" + params.joinToString("&")
        return checkLength(payload)
    }

    fun phone(number: String): QrPayloadResult {
        val cleaned = number.filterNot { it == ' ' || it == '-' || it == '(' || it == ')' || it == '.' }
        if (cleaned.isEmpty()) return QrPayloadResult.Empty
        if (!Regex("^\\+?[0-9]{3,15}$").matches(cleaned)) return QrPayloadResult.Error(QrError.PHONE_INVALID)
        return QrPayloadResult.Ok("tel:$cleaned")
    }

    /** Escapes the characters that have a meaning in the Wi-Fi QR format. */
    fun escapeWifi(value: String): String = buildString {
        for (c in value) {
            if (c == '\\' || c == ';' || c == ',' || c == ':' || c == '"') append('\\')
            append(c)
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun checkLength(payload: String): QrPayloadResult =
        if (payload.toByteArray(Charsets.UTF_8).size > MAX_BYTES) {
            QrPayloadResult.Error(QrError.TOO_LONG)
        } else {
            QrPayloadResult.Ok(payload)
        }
}
