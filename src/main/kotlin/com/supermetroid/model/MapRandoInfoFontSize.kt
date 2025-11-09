package com.supermetroid.model

enum class MapRandoInfoFontSize(val displayName: String, val labelSize: Int, val valueSize: Int, val panelWidth: Int) {
    VERY_SMALL("Very Small", 8, 11, 100),
    SMALL("Small", 9, 13, 130),
    MEDIUM("Medium", 10, 15, 150),
    LARGE("Large", 11, 17, 175),
    VERY_LARGE("Very Large", 12, 19, 180);

    companion object {
        fun fromDisplayName(name: String): MapRandoInfoFontSize {
            return values().find { it.displayName == name } ?: MEDIUM
        }
    }
}
