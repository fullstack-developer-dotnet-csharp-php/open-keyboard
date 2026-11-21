package org.openkeyboard

import org.junit.Assert.assertEquals
import org.junit.Test
import org.openkeyboard.engine.OpenKeyboardEngine
import org.openkeyboard.engine.OpenXmlParser

class OpenKeyboardEngineTest {

    private val sampleXml = """
        <?xml version="1.0" ?>
        <Table>
            <Header>
                <Language>Tamil</Language>
                <Encoding>Unicode</Encoding>
                <Keyboard>Tamil26</Keyboard>
                <Author>SIVA</Author>
                <Description>SIVA 26-Key layout test</Description>
            </Header>
            <Data>
                <Map><Display>0BA4</Display><Character>0BA4</Character><Keys>61</Keys></Map>
                <Map><Display>0BA40BCD0BA4</Display><Character>0BA40BCD0BA4</Character><Keys>6161</Keys></Map>
                <Map><Display>0BA40BCD</Display><Character>0BA40BCD</Character><Keys>6167</Keys></Map>
                <Map><Display>0BA40BBE</Display><Character>0BA40BBE</Character><Keys>6168</Keys></Map>
                <Map><Display>0B85</Display><Character>0B85</Character><Keys>68</Keys></Map>
                <Map><Display>0B86</Display><Character>0B86</Character><Keys>6868</Keys></Map>
            </Data>
        </Table>
    """.trimIndent()

    @Test
    fun testXmlParsing() {
        val parsed = OpenXmlParser.parse(sampleXml, "Tamil 26.xml")
        assertEquals("Tamil", parsed.info.language)
        assertEquals("Unicode", parsed.info.encoding)
        assertEquals("Tamil26", parsed.info.keyboard)
        assertEquals("SIVA", parsed.info.author)
        assertEquals(6, parsed.info.ruleCount)
    }

    @Test
    fun testTamil26Composition() {
        val parsed = OpenXmlParser.parse(sampleXml, "Tamil 26.xml")
        val engine = OpenKeyboardEngine(parsed)

        // Type 'a' (61) -> "த"
        val res1 = engine.handleKeyPress('a')
        assertEquals("த", res1.insertText)

        // Type 'a' again -> "aa" (6161) -> "த்த"
        val res2 = engine.handleKeyPress('a')
        assertEquals(1, res2.deleteCount)
        assertEquals("த்த", res2.insertText)
    }

    @Test
    fun testTamil26PulliAndVowel() {
        val parsed = OpenXmlParser.parse(sampleXml, "Tamil 26.xml")
        val engine = OpenKeyboardEngine(parsed)

        // Type 'a' (61) -> "த"
        engine.handleKeyPress('a')

        // Type 'g' (67) -> "ag" (6167) -> "த்"
        val res = engine.handleKeyPress('g')
        assertEquals(1, res.deleteCount)
        assertEquals("த்", res.insertText)
    }

    @Test
    fun testTamil26SanskritThirdKeys() {
        val stream = java.io.File("D:/Users/Siva/Desktop/open-keyboard/keymaps/Tamil 26.xml").inputStream()
        val parsed = OpenXmlParser.parse(stream, "Tamil 26.xml")
        val engine = OpenKeyboardEngine(parsed)

        assertEquals("ஸ்ரீ", engine.getLongPressChar('q'))
        assertEquals("க்ஷ்", engine.getLongPressChar('w'))
        assertEquals("ஹ்", engine.getLongPressChar('e'))
        assertEquals("ஷ்", engine.getLongPressChar('r'))
        assertEquals("ஸ்", engine.getLongPressChar('t'))
        assertEquals("ஜ்", engine.getLongPressChar('y'))
        assertEquals("ஶ்", engine.getLongPressChar('u'))
        assertEquals("ௐ", engine.getLongPressChar('i'))
        assertEquals("ஃ", engine.getLongPressChar('m'))
        assertEquals(",", engine.getLongPressChar('.'))
        assertEquals(",", engine.getLongPressBadge('.'))

        // Verify Sanskrit composition after long press:
        // Sequence for "ஜ்" is "yg"
        val seq = engine.findKeySequenceForOutput("ஜ்")
        assertEquals("yg", seq)

        engine.setBufferAndOutput(seq!!, "ஜ்")
        // Type 'h' -> candidate "ygh" -> maps to "ஜ" (replaces 2-char "ஜ்" with 1-char "ஜ")
        val res = engine.handleKeyPress('h')
        assertEquals(2, res.deleteCount)
        assertEquals("ஜ", res.insertText)

        // Type 'h' again -> candidate "yghh" -> maps to "ஜா"
        val res2 = engine.handleKeyPress('h')
        assertEquals(1, res2.deleteCount)
        assertEquals("ஜா", res2.insertText)
    }

    @Test
    fun testEnglishLongPressPeriodToComma() {
        val stream = java.io.File("D:/Users/Siva/Desktop/open-keyboard/keymaps/English.xml").inputStream()
        val parsed = OpenXmlParser.parse(stream, "English.xml")
        val engine = OpenKeyboardEngine(parsed)

        assertEquals(",", engine.getLongPressChar('.'))
        assertEquals(",", engine.getLongPressBadge('.'))
    }
}
