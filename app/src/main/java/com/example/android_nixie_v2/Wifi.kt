package com.example.android_nixie_v2

enum class AuthMode{
    WIFI_AUTH_OPEN,             /**< authenticate mode : open */
    WIFI_AUTH_WEP,              /**< authenticate mode : WEP */
    WIFI_AUTH_WPA_PSK,          /**< authenticate mode : WPA_PSK */
    WIFI_AUTH_WPA2_PSK,         /**< authenticate mode : WPA2_PSK */
    WIFI_AUTH_WPA_WPA2_PSK,     /**< authenticate mode : WPA_WPA2_PSK */
    WIFI_AUTH_WPA2_ENTERPRISE,  /**< authenticate mode : WPA2_ENTERPRISE */
    WIFI_AUTH_WPA3_PSK,         /**< authenticate mode : WPA3_PSK */
    WIFI_AUTH_WPA2_WPA3_PSK,    /**< authenticate mode : WPA2_WPA3_PSK */
    WIFI_AUTH_WAPI_PSK,         /**< authenticate mode : WAPI_PSK */
    WIFI_AUTH_OWE,              /**< authenticate mode : OWE */
}

fun authModeOrdinaltoEnum (value : Int) : AuthMode {
    return when (value) {
        0 -> AuthMode.WIFI_AUTH_OPEN
        1 -> AuthMode.WIFI_AUTH_WEP
        2 -> AuthMode.WIFI_AUTH_WPA_PSK
        3 -> AuthMode.WIFI_AUTH_WPA2_PSK
        4 -> AuthMode.WIFI_AUTH_WPA_WPA2_PSK
        5 -> AuthMode.WIFI_AUTH_WPA2_ENTERPRISE
        6 -> AuthMode.WIFI_AUTH_WPA3_PSK
        7 -> AuthMode.WIFI_AUTH_WPA2_WPA3_PSK
        8 -> AuthMode.WIFI_AUTH_WAPI_PSK
        9 -> AuthMode.WIFI_AUTH_OWE
        else -> AuthMode.WIFI_AUTH_OPEN
    }
}

fun authModetoString (value : AuthMode) : String {
    return when (value) {
        AuthMode.WIFI_AUTH_OPEN -> "Open"
        AuthMode.WIFI_AUTH_WEP -> "WEP"
        AuthMode.WIFI_AUTH_WPA_PSK -> "WAP PSK"
        AuthMode.WIFI_AUTH_WPA2_PSK -> "WPA2 PSK"
        AuthMode.WIFI_AUTH_WPA_WPA2_PSK -> "WPA WPA2 PSK"
        AuthMode.WIFI_AUTH_WPA2_ENTERPRISE -> "WPA2 Enterprise"
        AuthMode.WIFI_AUTH_WPA3_PSK -> "WPA3 PSK"
        AuthMode.WIFI_AUTH_WPA2_WPA3_PSK -> "WPA2 WPA3 PSK"
        AuthMode.WIFI_AUTH_WAPI_PSK -> "WAPI PSK"
        AuthMode.WIFI_AUTH_OWE -> "OWE"
        else -> "Open"
    }
}

data class Wifi(val ssid : String, val rssi : Int, val authMode : AuthMode)
