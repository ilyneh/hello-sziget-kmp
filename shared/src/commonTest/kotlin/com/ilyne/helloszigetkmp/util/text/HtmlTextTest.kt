package com.ilyne.helloszigetkmp.util.text

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlTextTest {
    @Test
    fun plainText_isUnchanged() {
        val result = "Just plain text".htmlToAnnotatedString()

        assertEquals("Just plain text", result.text)
    }

    @Test
    fun paragraphs_areSeparatedByBlankLine() {
        val result = "<p>First</p><p>Second</p>".htmlToAnnotatedString()

        assertEquals("First\n\nSecond", result.text)
    }

    @Test
    fun br_insertsSingleNewline() {
        val result = "Line one<br>Line two".htmlToAnnotatedString()

        assertEquals("Line one\nLine two", result.text)
    }

    @Test
    fun bold_appliesBoldSpanStyle() {
        val result = "<b>Bold</b> text".htmlToAnnotatedString()

        assertEquals("Bold text", result.text)
        assertTrue(result.spanStyles.any { it.item == SpanStyle(fontWeight = FontWeight.Bold) && it.start == 0 && it.end == 4 })
    }

    @Test
    fun italic_appliesItalicSpanStyle() {
        val result = "<i>Italic</i>".htmlToAnnotatedString()

        assertEquals("Italic", result.text)
        assertTrue(result.spanStyles.any { it.item == SpanStyle(fontStyle = FontStyle.Italic) })
    }

    @Test
    fun underline_appliesUnderlineSpanStyle() {
        val result = "<u>Underlined</u>".htmlToAnnotatedString()

        assertEquals("Underlined", result.text)
        assertTrue(result.spanStyles.any { it.item == SpanStyle(textDecoration = TextDecoration.Underline) })
    }

    @Test
    fun link_addsUrlAnnotationAndUnderlineStyle() {
        val result = """<a href="https://example.com">link</a>""".htmlToAnnotatedString()

        assertEquals("link", result.text)
        val annotation = result.getStringAnnotations(tag = "URL", start = 0, end = result.length).single()
        assertEquals("https://example.com", annotation.item)
        assertTrue(result.spanStyles.any { it.item == SpanStyle(textDecoration = TextDecoration.Underline) })
    }

    @Test
    fun link_withoutHref_producesEmptyAnnotation() {
        val result = "<a>link</a>".htmlToAnnotatedString()

        val annotation = result.getStringAnnotations(tag = "URL", start = 0, end = result.length).single()
        assertEquals("", annotation.item)
    }

    @Test
    fun unorderedList_prefixesItemsWithBullet() {
        val result = "<ul><li>One</li><li>Two</li></ul>".htmlToAnnotatedString()

        assertEquals("• One\n• Two\n", result.text)
    }

    @Test
    fun nestedTags_composeStyles() {
        val result = "<b><i>Both</i></b>".htmlToAnnotatedString()

        assertEquals("Both", result.text)
        assertTrue(result.spanStyles.any { it.item == SpanStyle(fontWeight = FontWeight.Bold) })
        assertTrue(result.spanStyles.any { it.item == SpanStyle(fontStyle = FontStyle.Italic) })
    }

    @Test
    fun entities_areDecoded() {
        val result = "Tom &amp; Jerry &lt;3 &quot;fun&quot; &#39;times&#39; &nbsp;end&gt;".htmlToAnnotatedString()

        assertEquals("Tom & Jerry <3 \"fun\" 'times'  end>", result.text)
    }

    @Test
    fun unclosedAngleBracket_isAppendedLiterally() {
        val result = "5 < 10".htmlToAnnotatedString()

        assertEquals("5 < 10", result.text)
    }

    @Test
    fun unknownTag_isStrippedButContentKept() {
        val result = "<span>content</span>".htmlToAnnotatedString()

        assertEquals("content", result.text)
    }
}
