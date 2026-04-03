package com.supermetroid.model

enum class StatsFontSize(
    val displayName: String,
    val label: Int,      // stat labels, axis labels
    val value: Int,      // stat values, bar counts
    val heading: Int,    // section headings
    val chart: Int       // chart labels, axis text
) {
    SMALL("Small", 9, 9, 10, 7),
    MEDIUM("Medium", 11, 11, 12, 8),
    LARGE("Large", 13, 13, 14, 10);

    companion object {
        fun fromDisplayName(name: String): StatsFontSize {
            return values().find { it.displayName == name } ?: MEDIUM
        }
    }
}
