package com.supermetroid.livesplit

import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Writes [LiveSplitDocument] back to LiveSplit (.lss) XML format.
 *
 * Produces valid XML that LiveSplit can read, preserving the same structure:
 * - Game/category metadata
 * - Segment definitions with best times and split comparisons
 * - Full attempt history
 * - Per-segment history
 */
class LiveSplitWriter : Logging {

    fun writeToFile(document: LiveSplitDocument, file: File) {
        logger.info { "Writing LiveSplit file: ${file.absolutePath}" }
        val xml = writeToString(document)
        file.writeText(xml)
    }

    fun writeToString(document: LiveSplitDocument): String {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc: Document = builder.newDocument()

        val run = doc.createElement("Run").also { doc.appendChild(it) }
        run.setAttribute("version", "1.7.0")

        run.appendTextElement(doc, "GameIcon", "")
        run.appendTextElement(doc, "GameName", document.gameName)
        run.appendTextElement(doc, "CategoryName", document.categoryName)

        val metadata = doc.createElement("Metadata")
        val runEl = doc.createElement("Run")
        runEl.setAttribute("id", "")
        metadata.appendChild(runEl)
        val platform = doc.createElement("Platform")
        platform.setAttribute("usesEmulator", "False")
        metadata.appendChild(platform)
        metadata.appendTextElement(doc, "Region", "")
        metadata.appendTextElement(doc, "Variables", "")
        run.appendChild(metadata)

        run.appendTextElement(doc, "Offset", document.offset)
        run.appendTextElement(doc, "AttemptCount", document.attemptCount.toString())

        val attemptHistory = doc.createElement("AttemptHistory")
        for (attempt in document.attemptHistory) {
            val attemptEl = doc.createElement("Attempt")
            attemptEl.setAttribute("id", attempt.id.toString())
            attempt.started?.let { attemptEl.setAttribute("started", it) }
            attempt.ended?.let { attemptEl.setAttribute("ended", it) }

            if (attempt.realTime != null) {
                attemptEl.appendTextElement(doc, "RealTime", LiveSplitParser.msToTimeString(attempt.realTime))
            }
            if (attempt.gameTime != null) {
                attemptEl.appendTextElement(doc, "GameTime", LiveSplitParser.msToTimeString(attempt.gameTime))
            }
            attemptHistory.appendChild(attemptEl)
        }
        run.appendChild(attemptHistory)

        val segments = doc.createElement("Segments")
        for (segment in document.segments) {
            segments.appendChild(buildSegmentElement(doc, segment))
        }
        run.appendChild(segments)

        val autoSplitter = doc.createElement("AutoSplitterSettings")
        if (!document.autoSplitterSettings.isNullOrBlank()) {
            autoSplitter.textContent = document.autoSplitterSettings
        }
        run.appendChild(autoSplitter)

        return serializeDocument(doc)
    }

    private fun buildSegmentElement(doc: Document, segment: LiveSplitSegment): Element {
        val segEl = doc.createElement("Segment")
        segEl.appendTextElement(doc, "Name", segment.name)
        segEl.appendTextElement(doc, "Icon", segment.icon ?: "")

        val bestTime = doc.createElement("BestSegmentTime")
        if (segment.bestSegmentTime != null) {
            segment.bestSegmentTime.realTime?.let {
                bestTime.appendTextElement(doc, "RealTime", LiveSplitParser.msToTimeString(it))
            }
            segment.bestSegmentTime.gameTime?.let {
                bestTime.appendTextElement(doc, "GameTime", LiveSplitParser.msToTimeString(it))
            }
        }
        segEl.appendChild(bestTime)

        val splitTimes = doc.createElement("SplitTimes")
        for (st in segment.splitTimes) {
            val stEl = doc.createElement("SplitTime")
            stEl.setAttribute("name", st.comparisonName)
            st.realTime?.let {
                stEl.appendTextElement(doc, "RealTime", LiveSplitParser.msToTimeString(it))
            }
            st.gameTime?.let {
                stEl.appendTextElement(doc, "GameTime", LiveSplitParser.msToTimeString(it))
            }
            splitTimes.appendChild(stEl)
        }
        segEl.appendChild(splitTimes)

        val history = doc.createElement("SegmentHistory")
        for (entry in segment.segmentHistory) {
            val timeEl = doc.createElement("Time")
            timeEl.setAttribute("id", entry.id.toString())
            entry.realTime?.let {
                timeEl.appendTextElement(doc, "RealTime", LiveSplitParser.msToTimeString(it))
            }
            entry.gameTime?.let {
                timeEl.appendTextElement(doc, "GameTime", LiveSplitParser.msToTimeString(it))
            }
            history.appendChild(timeEl)
        }
        segEl.appendChild(history)

        return segEl
    }

    private fun serializeDocument(doc: Document): String {
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")

        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))
        return writer.toString()
    }
}

private fun Element.appendTextElement(doc: Document, tagName: String, text: String) {
    val el = doc.createElement(tagName)
    el.textContent = text
    this.appendChild(el)
}
