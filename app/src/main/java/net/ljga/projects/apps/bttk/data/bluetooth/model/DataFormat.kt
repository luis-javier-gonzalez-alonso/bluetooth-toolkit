package net.ljga.projects.apps.bttk.data.bluetooth.model

enum class DataFormat {
    HEX_ASCII, // For raw binary streams
    STRUCTURED,  // For human-readable strings like service info
    GATT_STRUCTURE
}