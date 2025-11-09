package com.supermetroid.model

enum class MapRandoInfoFontSize(val displayName: String, val labelSize: Int, val valueSize: Int, val panelWidth: Int) {
    VERY_SMALL("Very Small", 9, 10, 100),
    SMALL("Small", 10, 11, 130),
    MEDIUM("Medium", 12, 14, 150),
    LARGE("Large", 12, 16, 175),
    VERY_LARGE("Very Large", 12, 18, 180);

    companion object {
        fun fromDisplayName(name: String): MapRandoInfoFontSize {
            return values().find { it.displayName == name } ?: MEDIUM
        }
    }
}
