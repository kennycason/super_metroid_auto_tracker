package com.supermetroid.model

enum class SplitsFontSize(
    val displayName: String,
    val header: Int,    // column header labels
    val text: Int,      // split name text
    val time: Int,      // time value (main line, compact; +1 when no segment delta second line)
    val detail: Int,    // segment delta second line
    val summary: Int,   // PB / Best Possible summary rows
    val timeColumnWidth: Int,  // cumulative time columns, sized for H:MM:SS.ss
    val deltaColumnWidth: Int  // signed segment/cumulative delta columns
) {
    SMALL(
        "Small", header = 9, text = 10, time = 9, detail = 8, summary = 13,
        timeColumnWidth = 85, deltaColumnWidth = 60
    ),
    MEDIUM(
        "Medium", header = 10, text = 12, time = 10, detail = 9, summary = 15,
        timeColumnWidth = 85, deltaColumnWidth = 65
    ),
    LARGE(
        "Large", header = 11, text = 14, time = 12, detail = 10, summary = 18,
        timeColumnWidth = 96, deltaColumnWidth = 72
    ),
    VERY_LARGE(
        "Very Large", header = 13, text = 18, time = 15, detail = 12, summary = 22,
        timeColumnWidth = 116, deltaColumnWidth = 86
    );

    companion object {
        fun fromDisplayName(name: String): SplitsFontSize {
            return values().find { it.displayName == name } ?: MEDIUM
        }
    }
}
