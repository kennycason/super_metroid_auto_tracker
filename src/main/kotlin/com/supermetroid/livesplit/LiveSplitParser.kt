package com.supermetroid.livesplit

import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses LiveSplit (.lss) XML files into [LiveSplitDocument].
 *
 * LSS format overview:
 * ```xml
 * <Run version="1.7.0">
 *   <GameName>Super Metroid</GameName>
 *   <CategoryName>100%</CategoryName>
 *   <AttemptCount>221</AttemptCount>
 *   <AttemptHistory>
 *     <Attempt id="1" started="..." ended="...">
 *       <RealTime>01:22:57.1393650</RealTime>
 *     </Attempt>
 *   </AttemptHistory>
 *   <Segments>
 *     <Segment>
 *       <Name>Bomb</Name>
 *       <BestSegmentTime><RealTime>00:04:51.8787600</RealTime></BestSegmentTime>
 *       <SplitTimes>
 *         <SplitTime name="Personal Best"><RealTime>00:04:57.2103800</RealTime></SplitTime>
 *       </SplitTimes>
 *       <SegmentHistory>
 *         <Time id="1"><RealTime>00:05:07.0000000</RealTime></Time>
 *       </SegmentHistory>
 *     </Segment>
 *   </Segments>
 * </Run>
 * ```
 */
class LiveSplitParser : Logging {

    fun parseFile(file: File): LiveSplitDocument {
        logger.info { "Parsing LiveSplit file: ${file.absolutePath}" }
        return parse(file.inputStream())
    }

    fun parseString(xml: String): LiveSplitDocument {
        return parse(xml.byteInputStream())
    }

    fun parse(inputStream: InputStream): LiveSplitDocument {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        val builder = factory.newDocumentBuilder()
        val doc: Document = builder.parse(inputStream)
        doc.documentElement.normalize()

        val root = doc.documentElement
        require(root.tagName == "Run") { "Expected root element <Run>, got <${root.tagName}>" }

        val gameName = root.getChildText("GameName") ?: ""
        val categoryName = root.getChildText("CategoryName") ?: ""
        val attemptCount = root.getChildText("AttemptCount")?.toIntOrNull() ?: 0
        val offset = root.getChildText("Offset") ?: "00:00:00.0000000"

        val segments = parseSegments(root)
        val attemptHistory = parseAttemptHistory(root)
        val autoSplitterSettings = root.getChildText("AutoSplitterSettings")

        val result = LiveSplitDocument(
            gameName = gameName,
            categoryName = categoryName,
            attemptCount = attemptCount,
            offset = offset,
            segments = segments,
            attemptHistory = attemptHistory,
            autoSplitterSettings = autoSplitterSettings
        )

        logger.info { "Parsed ${result.segments.size} segments, ${result.attemptHistory.size} attempts for $gameName - $categoryName" }
        return result
    }

    private fun parseSegments(root: Element): List<LiveSplitSegment> {
        val segmentsElement = root.getChildElement("Segments") ?: return emptyList()
        val segmentNodes = segmentsElement.getElementsByTagName("Segment")
        return (0 until segmentNodes.length).map { i ->
            parseSegment(segmentNodes.item(i) as Element)
        }
    }

    private fun parseSegment(element: Element): LiveSplitSegment {
        val name = element.getChildText("Name") ?: ""
        val icon = element.getChildText("Icon")?.takeIf { it.isNotBlank() }

        val bestSegmentTimeEl = element.getChildElement("BestSegmentTime")
        val bestSegmentTime = bestSegmentTimeEl?.let { parseTimeSpan(it) }

        val splitTimes = parseSplitTimes(element)
        val segmentHistory = parseSegmentHistory(element)

        return LiveSplitSegment(
            name = name,
            icon = icon,
            bestSegmentTime = bestSegmentTime,
            splitTimes = splitTimes,
            segmentHistory = segmentHistory
        )
    }

    private fun parseSplitTimes(segmentElement: Element): List<LiveSplitComparisonSplit> {
        val splitTimesEl = segmentElement.getChildElement("SplitTimes") ?: return emptyList()
        val splitTimeNodes = splitTimesEl.getElementsByTagName("SplitTime")
        return (0 until splitTimeNodes.length).map { i ->
            val el = splitTimeNodes.item(i) as Element
            val comparisonName = el.getAttribute("name") ?: ""
            val realTime = el.getChildText("RealTime")?.let { parseTimeToMs(it) }
            val gameTime = el.getChildText("GameTime")?.let { parseTimeToMs(it) }
            LiveSplitComparisonSplit(
                comparisonName = comparisonName,
                realTime = realTime,
                gameTime = gameTime
            )
        }
    }

    private fun parseSegmentHistory(segmentElement: Element): List<LiveSplitHistoryEntry> {
        val historyEl = segmentElement.getChildElement("SegmentHistory") ?: return emptyList()
        val timeNodes = historyEl.getElementsByTagName("Time")
        return (0 until timeNodes.length).map { i ->
            val el = timeNodes.item(i) as Element
            val id = el.getAttribute("id")?.toIntOrNull() ?: 0
            val realTime = el.getChildText("RealTime")?.let { parseTimeToMs(it) }
            val gameTime = el.getChildText("GameTime")?.let { parseTimeToMs(it) }
            LiveSplitHistoryEntry(id = id, realTime = realTime, gameTime = gameTime)
        }
    }

    private fun parseAttemptHistory(root: Element): List<LiveSplitAttempt> {
        val historyEl = root.getChildElement("AttemptHistory") ?: return emptyList()
        val attemptNodes = historyEl.getElementsByTagName("Attempt")
        return (0 until attemptNodes.length).map { i ->
            val el = attemptNodes.item(i) as Element
            val id = el.getAttribute("id")?.toIntOrNull() ?: 0
            val started = el.getAttribute("started")
            val ended = el.getAttribute("ended")
            val realTime = el.getChildText("RealTime")?.let { parseTimeToMs(it) }
            val gameTime = el.getChildText("GameTime")?.let { parseTimeToMs(it) }
            LiveSplitAttempt(
                id = id,
                started = started,
                ended = ended,
                realTime = realTime,
                gameTime = gameTime
            )
        }
    }

    private fun parseTimeSpan(element: Element): LiveSplitTimeSpan? {
        val realTime = element.getChildText("RealTime")?.let { parseTimeToMs(it) }
        val gameTime = element.getChildText("GameTime")?.let { parseTimeToMs(it) }
        if (realTime == null && gameTime == null) return null
        return LiveSplitTimeSpan(realTime = realTime, gameTime = gameTime)
    }

    companion object {
        /**
         * Parse a LiveSplit time string to milliseconds.
         *
         * Formats:
         * - "HH:MM:SS.TTTTTTT" (100-nanosecond ticks fractional part)
         * - "MM:SS.TTTTTTT"
         * - "-HH:MM:SS.TTTTTTT" (negative times for skipped segments)
         *
         * LiveSplit uses .NET TimeSpan format where the fractional part is
         * in units of 100 nanoseconds (ticks), not decimal seconds.
         */
        fun parseTimeToMs(timeStr: String): Long? {
            if (timeStr.isBlank()) return null

            val trimmed = timeStr.trim()
            val negative = trimmed.startsWith("-")
            val abs = if (negative) trimmed.substring(1) else trimmed

            val parts = abs.split(":")
            if (parts.size < 2) return null

            try {
                val hours: Long
                val minutes: Long
                val secondsWithFraction: String

                when (parts.size) {
                    2 -> {
                        hours = 0
                        minutes = parts[0].toLong()
                        secondsWithFraction = parts[1]
                    }
                    3 -> {
                        hours = parts[0].toLong()
                        minutes = parts[1].toLong()
                        secondsWithFraction = parts[2]
                    }
                    else -> return null
                }

                val secondsParts = secondsWithFraction.split(".")
                val seconds = secondsParts[0].toLong()
                val fractionStr = if (secondsParts.size > 1) secondsParts[1] else "0"

                // LiveSplit uses 7-digit ticks (100ns units). Pad/truncate to 7 digits, then take first 3 for ms
                val paddedFraction = fractionStr.padEnd(7, '0').take(7)
                val ticks = paddedFraction.toLong()
                val fractionalMs = ticks / 10_000 // 10,000 ticks per millisecond

                val totalMs = (hours * 3600 + minutes * 60 + seconds) * 1000 + fractionalMs
                return if (negative) -totalMs else totalMs
            } catch (e: NumberFormatException) {
                return null
            }
        }

        /**
         * Convert milliseconds to LiveSplit time string format (HH:MM:SS.TTTTTTT)
         */
        fun msToTimeString(ms: Long): String {
            val negative = ms < 0
            val abs = if (negative) -ms else ms

            val hours = abs / 3600000
            val minutes = (abs % 3600000) / 60000
            val seconds = (abs % 60000) / 1000
            val remainderMs = abs % 1000
            val ticks = remainderMs * 10_000 // Convert ms to 100ns ticks

            val timeStr = "%02d:%02d:%02d.%07d".format(hours, minutes, seconds, ticks)
            return if (negative) "-$timeStr" else timeStr
        }
    }
}

// XML helper extensions

private fun Element.getChildText(tagName: String): String? {
    val nodes = getElementsByTagName(tagName)
    if (nodes.length == 0) return null
    // Only look at direct children to avoid picking up nested elements
    for (i in 0 until nodes.length) {
        val node = nodes.item(i)
        if (node.parentNode == this) {
            return node.textContent?.trim()
        }
    }
    return nodes.item(0)?.textContent?.trim()
}

private fun Element.getChildElement(tagName: String): Element? {
    val nodes = getElementsByTagName(tagName)
    if (nodes.length == 0) return null
    for (i in 0 until nodes.length) {
        val node = nodes.item(i)
        if (node.parentNode == this && node is Element) {
            return node
        }
    }
    return if (nodes.item(0) is Element) nodes.item(0) as Element else null
}
