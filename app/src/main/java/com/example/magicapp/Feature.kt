package com.example.magicapp

enum class Feature(val label: String, val icon: String) {
    GPS("GPS", "gps_fixed"),
    WIFI_SCANNER("WiFi", "wifi"),
    BT_SCANNER("Bluetooth", "bluetooth"),
    MICROPHONE("Mic", "mic"),
    SYSTEM_INFO("System", "info"),
    COMPASS("Compass", "explore"),
    CLOCK("Clock", "schedule"),
    CAR_BUTTONS("Car Ctrl", "directions_car"),
    BROWSER("Browser", "language"),
    YOUTUBE("YouTube", "play_circle"),
    NEWS("News", "newspaper"),
    VOICE_ASSISTANT("Voice", "record_voice_over"),
    LLM_SETTINGS("LLM", "psychology"),
    AIRPLAY("AirPlay", "cast"),
    ;

    companion object {
        /** Features shown as quick tiles in GlanceScreen */
        val glanceTiles = listOf(NEWS, YOUTUBE, BROWSER, VOICE_ASSISTANT)
    }
}
