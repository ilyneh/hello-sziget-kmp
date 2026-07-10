package com.ilyne.helloszigetkmp.util.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Minimal HTML -> [AnnotatedString] converter for simple rich-text content such as artist bios
 * (paragraphs, bold/italic/underline, links, and lists). Compose's `AnnotatedString.fromHtml` is
 * Android-only, so this covers the common subset of tags needed in commonMain without pulling in
 * a full HTML parsing dependency.
 */
fun String.htmlToAnnotatedString(): AnnotatedString {
    val html = this
    return buildAnnotatedString {
        var i = 0
        var pendingBlockBreak = false
        var listDepth = 0

        fun appendPendingBreak() {
            if (pendingBlockBreak) {
                if (length > 0) append("\n\n")
                pendingBlockBreak = false
            }
        }

        while (i < html.length) {
            val char = html[i]
            if (char == '<') {
                val end = html.indexOf('>', i)
                if (end == -1) {
                    append(char)
                    i++
                    continue
                }
                val tag = html.substring(i + 1, end).trim()
                val tagName = tag.trimStart('/').substringBefore(' ').lowercase()
                val isClosing = tag.startsWith("/")

                when (tagName) {
                    "p", "div" -> {
                        if (isClosing) pendingBlockBreak = true
                        else appendPendingBreak()
                    }

                    "br" -> {
                        append("\n")
                    }

                    "b", "strong" -> {
                        if (!isClosing) {
                            appendPendingBreak()
                            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        } else {
                            pop()
                        }
                    }

                    "i", "em" -> {
                        if (!isClosing) {
                            appendPendingBreak()
                            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        } else {
                            pop()
                        }
                    }

                    "u" -> {
                        if (!isClosing) {
                            appendPendingBreak()
                            pushStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                        } else {
                            pop()
                        }
                    }

                    "a" -> {
                        if (!isClosing) {
                            appendPendingBreak()
                            val href = Regex("""href\s*=\s*["']([^"']*)["']""")
                                .find(tag)
                                ?.groupValues
                                ?.getOrNull(1)
                            pushStringAnnotation(tag = "URL", annotation = href.orEmpty())
                            pushStyle(
                                SpanStyle(
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                )
                            )
                        } else {
                            pop()
                            pop()
                        }
                    }

                    "ul", "ol" -> {
                        if (!isClosing) {
                            appendPendingBreak()
                            listDepth++
                        } else {
                            listDepth--
                            pendingBlockBreak = true
                        }
                    }

                    "li" -> {
                        if (!isClosing) {
                            appendPendingBreak()
                            append("• ")
                        } else {
                            append("\n")
                        }
                    }
                }

                i = end + 1
            } else {
                appendPendingBreak()
                when {
                    html.startsWith("&amp;", i) -> {
                        append("&"); i += 5
                    }
                    html.startsWith("&lt;", i) -> {
                        append("<"); i += 4
                    }
                    html.startsWith("&gt;", i) -> {
                        append(">"); i += 4
                    }
                    html.startsWith("&quot;", i) -> {
                        append("\""); i += 6
                    }
                    html.startsWith("&#39;", i) || html.startsWith("&apos;", i) -> {
                        append("'"); i += if (html.startsWith("&#39;", i)) 5 else 6
                    }
                    html.startsWith("&nbsp;", i) -> {
                        append(" "); i += 6
                    }
                    else -> {
                        append(char)
                        i++
                    }
                }
            }
        }
    }
}
