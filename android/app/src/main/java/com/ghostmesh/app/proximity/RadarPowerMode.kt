package com.ghostmesh.app.proximity

enum class RadarPowerMode(val displayName: String, val description: String) {
    OFF("Radar Off", "Radio inactive. Zero battery consumption."),
    SMART_DISCOVERY("Smart Discovery", "Balanced low-power scanning and advertising. Recommended default."),
    EXTENDED_RANGE("Extended Range", "Maximizes reach using higher transmit power and LE Coded PHY. Uses more battery.")
}
