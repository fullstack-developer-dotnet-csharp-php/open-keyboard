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
                <Keyboard>Tamil26ADN</Keyboard>
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
        val parsed = OpenXmlParser.parse(sampleXml, "Tamil26ADN.xml")
        assertEquals("Tamil", parsed.info.language)
        assertEquals("Unicode", parsed.info.encoding)
        assertEquals("Tamil26ADN", parsed.info.keyboard)
        assertEquals("SIVA", parsed.info.author)
        assertEquals(6, parsed.info.ruleCount)
    }

    @Test
    fun testTamil26ADNComposition() {
        val parsed = OpenXmlParser.parse(sampleXml, "Tamil26ADN.xml")
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
    fun testTamil26ADNPulliAndVowel() {
        val parsed = OpenXmlParser.parse(sampleXml, "Tamil26ADN.xml")
        val engine = OpenKeyboardEngine(parsed)

        // Type 'a' (61) -> "த"
        engine.handleKeyPress('a')

        // Type 'g' (67) -> "ag" (6167) -> "த்"
        val res = engine.handleKeyPress('g')
        assertEquals(1, res.deleteCount)
        assertEquals("த்", res.insertText)
    }
}
