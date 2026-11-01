# Open Keyboard XML Keymap Specification

**Open Keyboard** is a universal, open-source keyboard platform for **Android**, **iOS**, and the **Web**. It allows anyone to design, upload, and use custom XML keyboard layouts for **any Indian or European language**.

---

## 1. High-Level XML Schema

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Table>
    <Header>
        <Language>Tamil</Language>
        <Encoding>Unicode</Encoding>
        <Keyboard>Tamil26ADN</Keyboard>
        <LCID>0449</LCID>
        <Author>SIVA</Author>
        <Description>Universal 26-Key Layout for Tamil</Description>
        <!-- 3-Stage Shift Cycle: Tamil -> CAPS English -> small English -> Tamil -->
        <ShiftCycle>Tamil,CapsEnglish,SmallEnglish</ShiftCycle>
    </Header>
    <Data>
        <!-- Default Native Mode (Tamil) -->
        <Map>
            <Display>0BA4</Display>
            <Character>0BA4</Character>
            <Keys>61</Keys>
        </Map>
        <Map>
            <Display>0BA40BCD0BA4</Display>
            <Character>0BA40BCD0BA4</Character>
            <Keys>6161</Keys>
        </Map>
        <Map>
            <Display>0BA40BCD</Display>
            <Character>0BA40BCD</Character>
            <Keys>6167</Keys>
        </Map>
        <Map>
            <Display>0BA40BBE</Display>
            <Character>0BA40BBE</Character>
            <Keys>6168</Keys>
        </Map>

        <!-- Layer: CapsEnglish (1st Shift press) -->
        <Map Layer="CapsEnglish">
            <Display>A</Display>
            <Character>0041</Character>
            <Keys>41</Keys>
        </Map>
        <Map Layer="CapsEnglish">
            <Display>B</Display>
            <Character>0042</Character>
            <Keys>42</Keys>
        </Map>

        <!-- Layer: SmallEnglish (2nd Shift press) -->
        <Map Layer="SmallEnglish">
            <Display>a</Display>
            <Character>0061</Character>
            <Keys>61</Keys>
        </Map>
        <Map Layer="SmallEnglish">
            <Display>b</Display>
            <Character>0062</Character>
            <Keys>62</Keys>
        </Map>
    </Data>
</Table>
```

---

## 2. Element Definitions

### `<Header>` Block
* `<Language>`: Language name:
  - **Indian Languages:** `Tamil`, `Hindi`, `Telugu`, `Malayalam`, `Kannada`, `Bengali`, `Gujarati`, `Punjabi`, `Odia`, `Marathi`, `Sanskrit`, etc.
  - **European Languages:** `English`, `French`, `German`, `Spanish`, `Portuguese`, `Russian`, `Greek`, etc.
* `<Encoding>`: Target font encoding (e.g. `Unicode`, `Bamini`, `Shrilipi`, `TSCII`, `TAB`, `TAM`, `Vanavil`).
* `<Keyboard>`: Unique layout name (e.g. `Tamil26ADN`, `Phonetic`, `Tamil99`, `Bamini`, `Inscript`, `Accents`, `Translit`).
* `<Author>`: Layout creator/author name (e.g. `SIVA`, `Open Keyboard Community`).
* `<Description>`: Purpose and notes regarding the layout.
* `<ShiftCycle>` *(Optional)*: Comma-separated list of shift modes (e.g. `Tamil,CapsEnglish,SmallEnglish`). Defaults to `<Language>,CapsEnglish,SmallEnglish`.

### `<Data>` Block
Contains one or more `<Map>` entries:
* `<Keys>`: Hexadecimal representation of ASCII keystrokes typed by the user (2 hex digits per character).
  - Examples:
    - `61` = `'a'`
    - `6161` = `'aa'`
    - `6167` = `'ag'`
    - `41` = `'A'`
    - `6527` = `'e\''` (for French `é`)
    - `6E7E` = `'n~'` (for Spanish `ñ`)
* `<Character>`: Hexadecimal representation of target Unicode/font character codepoints (4 hex digits per Unicode character).
  - Examples:
    - `0BA4` = `\u0BA4` ('த')
    - `0BA40BCD0BA4` = `\u0BA4\u0BCD\u0BA4` ('த்த')
    - `0041` = `A`
    - `0061` = `a`
    - `00E9` = `\u00E9` ('é')
    - `00F1` = `\u00F1` ('ñ')
    - `0430` = `\u0430` ('а' in Russian Cyrillic)
* `<Display>` *(Optional)*: Character to display on virtual key badges or UI hints. If omitted, `<Character>` is used.
* `Layer` attribute or `<Layer>` element *(Optional)*: Target layer for multi-stage shift cycles:
  - `Tamil` (or native language): Active by default.
  - `CapsEnglish`: Active on 1st Shift press (`A-Z`).
  - `SmallEnglish`: Active on 2nd Shift press (`a-z`).

---

## 3. 3-Stage Shift Cycle Mechanics

On Indian and phonetic XML keyboards where all 26 keys are assigned to native characters, users frequently need to enter English without switching keyboards entirely. The Shift key operates as a 3-stage cycle:

```
[Tamil Mode] ---> 1st Tap ---> [CAPS English] ---> 2nd Tap ---> [small English] ---> 3rd Tap ---> [Tamil Mode]
 (Default)                       (Q W E R T Y)                    (q w e r t y)                    (Default)
```

1. **Stage 1 (Tamil Mode):** Default state. Typing consumes the XML syllable rules (e.g. `a` $\rightarrow$ `த`, `ag` $\rightarrow$ `த்`). Secondary Tamil badges are rendered on keycaps. Spacebar displays layout name (e.g. `Tamil26ADN`).
2. **Stage 2 (CAPS English Mode):** Shift key lights up in Google Blue (`▲`). Center keycaps show uppercase letters (`A-Z`). Syllable badges are hidden. Typing outputs capital English letters. Spacebar displays `English (CAPS)`.
3. **Stage 3 (small English Mode):** Shift key displays `abc`. Center keycaps show lowercase letters (`a-z`). Syllable badges are hidden. Typing outputs lowercase English letters. Spacebar displays `English (abc)`.
4. **Stage 4:** Tapping Shift returns immediately to **Tamil Mode**.

---

## 4. How the Engine Works Across Languages

The Open Keyboard engine uses a **universal Trie-based longest prefix matching state machine**:
1. Keystrokes are buffered dynamically per active layer.
2. If typed characters form a prefix of an existing rule, the syllable is displayed and updated in place.
3. If the next key extends the rule, previous output is updated automatically (e.g. `a` $\rightarrow$ `த`, `aa` $\rightarrow$ `த்த`, `ag` $\rightarrow$ `த்`).
4. If a key doesn't extend the prefix, the previous sequence is committed, and a fresh sequence starts.
5. Backspace accurately rolls back to the previous syllable state or deletes characters cleanly.

---

## 5. How to Import a Keymap

1. Open the **Open Keyboard** app on your phone (or the Web Simulator in your browser).
2. Tap **"Upload Custom XML"** (Android) or **"Import XML"** (iOS).
3. Select your `.xml` file.
4. Open Keyboard automatically validates, loads, and sets the keymap as active system-wide!
