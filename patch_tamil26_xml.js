const fs = require('fs');

function patchTamil26(filePath) {
  let xml = fs.readFileSync(filePath, 'utf8');

  // Consonants in Tamil 26:
  // Key hex, base uyirmei hex, ai uyirmei hex
  // 73 (s) -> க (0B95) -> கை (0B950BC8)
  // 71 (q) -> ங (0B99) -> ஙை (0B990BC8)
  // 65 (e) -> ச (0B9A) -> சை (0B9A0BC8)
  // 70 (p) -> ஞ (0B9E) -> ஞை (0B9E0BC8)
  // 75 (u) -> ட (0B9F) -> டை (0B9F0BC8)
  // 78 (x) -> ண (0BA3) -> ணை (0BA30BC8)
  // 61 (a) -> த (0BA4) -> தை (0BA40BC8)
  // 69 (i) -> ந (0BA8) -> நை (0BA80BC8)
  // 72 (r) -> ப (0BAA) -> பை (0BAA0BC8)
  // 66 (f) -> ம (0BAE) -> மை (0BAE0BC8)
  // 77 (w) -> ய (0BAF) -> யை (0BAF0BC8)
  // 64 (d) -> ர (0BB0) -> ரை (0BB00BC8)
  // 62 (b) -> ல (0BB2) -> லை (0BB20BC8)
  // 76 (v) -> வ (0BB5) -> வை (0BB50BC8)
  // 7A (z) -> ழ (0BB4) -> ழை (0BB40BC8)
  // 63 (c) -> ள் (0BB3) -> ளை (0BB30BC8)
  // 6F (o) -> ற (0BB1) -> றை (0BB10BC8)
  // 74 (t) -> ன (0BA9) -> னை (0BA90BC8)

  const consonants = [
    { key: "73", char: "0B95", double: "0B950BCD0B95", ai: "0B950BC8", doubleAi: "0B950BCD0B950BC8" },
    { key: "71", char: "0B99", double: "0B990BCD0B99", ai: "0B990BC8", doubleAi: "0B990BCD0B990BC8" },
    { key: "65", char: "0B9A", double: "0B9A0BCD0B9A", ai: "0B9A0BC8", doubleAi: "0B9A0BCD0B9A0BC8" },
    { key: "70", char: "0B9E", double: "0B9E0BCD0B9E", ai: "0B9E0BC8", doubleAi: "0B9E0BCD0B9E0BC8" },
    { key: "75", char: "0B9F", double: "0B9F0BCD0B9F", ai: "0B9F0BC8", doubleAi: "0B9F0BCD0B9F0BC8" },
    { key: "78", char: "0BA3", double: "0BA30BCD0BA3", ai: "0BA30BC8", doubleAi: "0BA30BCD0BA30BC8" },
    { key: "61", char: "0BA4", double: "0BA40BCD0BA4", ai: "0BA40BC8", doubleAi: "0BA40BCD0BA40BC8" },
    { key: "69", char: "0BA8", double: "0BA80BCD0BA8", ai: "0BA80BC8", doubleAi: "0BA80BCD0BA80BC8" },
    { key: "72", char: "0BAA", double: "0BAA0BCD0BAA", ai: "0BAA0BC8", doubleAi: "0BAA0BCD0BAA0BC8" },
    { key: "66", char: "0BAE", double: "0BAE0BCD0BAE", ai: "0BAE0BC8", doubleAi: "0BAE0BCD0BAE0BC8" },
    { key: "77", char: "0BAF", double: "0BAF0BCD0BAF", ai: "0BAF0BC8", doubleAi: "0BAF0BCD0BAF0BC8" },
    { key: "64", char: "0BB0", double: "0BB00BCD0BB0", ai: "0BB00BC8", doubleAi: "0BB00BCD0BB00BC8" },
    { key: "62", char: "0BB2", double: "0BB20BCD0BB2", ai: "0BB20BC8", doubleAi: "0BB20BCD0BB20BC8" },
    { key: "76", char: "0BB5", double: "0BB50BCD0BB5", ai: "0BB50BC8", doubleAi: "0BB50BCD0BB50BC8" },
    { key: "7A", char: "0BB4", double: "0BB40BCD0BB4", ai: "0BB40BC8", doubleAi: "0BB40BCD0BB40BC8" },
    { key: "63", char: "0BB3", double: "0BB30BCD0BB3", ai: "0BB30BC8", doubleAi: "0BB30BCD0BB30BC8" },
    { key: "6F", char: "0BB1", double: "0BB10BCD0BB1", ai: "0BB10BC8", doubleAi: "0BB10BCD0BB10BC8" },
    { key: "74", char: "0BA9", double: "0BA90BCD0BA9", ai: "0BA90BC8", doubleAi: "0BA90BCD0BA90BC8" }
  ];

  let additions = '\n<!-- Additional Y-Combination (Ai) Rules for complete combining support -->\n';
  
  consonants.forEach(c => {
    // 1. Key + h + y (e.g. sh + y -> கை)
    const shy = `${c.key}6879`;
    if (!xml.includes(`<Keys>${shy}</Keys>`)) {
      additions += `<Map><Display>${c.ai}</Display><Character>${c.ai}</Character><Keys>${shy}</Keys></Map>\n`;
    }
    // 2. Double key + y (e.g. ss + y -> க்கை)
    const ssy = `${c.key}${c.key}79`;
    if (!xml.includes(`<Keys>${ssy}</Keys>`)) {
      additions += `<Map><Display>${c.doubleAi}</Display><Character>${c.doubleAi}</Character><Keys>${ssy}</Keys></Map>\n`;
    }
    // 3. Double key + h + y (e.g. ssh + y -> க்கை)
    const sshy = `${c.key}${c.key}6879`;
    if (!xml.includes(`<Keys>${sshy}</Keys>`)) {
      additions += `<Map><Display>${c.doubleAi}</Display><Character>${c.doubleAi}</Character><Keys>${sshy}</Keys></Map>\n`;
    }
  });

  // Also add Sanskrit LongPress support mappings:
  additions += `\n<!-- Sanskrit / Grantha Long-Press Mappings & y combinations -->
<Map Layer="LongPress"><Display>0BB8</Display><Character>0BB8</Character><Keys>73</Keys></Map>
<Map Layer="LongPress"><Display>0BB7</Display><Character>0BB7</Character><Keys>61</Keys></Map>
<Map Layer="LongPress"><Display>0B9C</Display><Character>0B9C</Character><Keys>6A</Keys></Map>
<Map Layer="LongPress"><Display>0BB9</Display><Character>0BB9</Character><Keys>68</Keys></Map>
<Map Layer="LongPress"><Display>0B950BCD0BB7</Display><Character>0B950BCD0BB7</Character><Keys>6B</Keys></Map>
<Map Layer="LongPress"><Display>0BB80BCD0BB00BC0</Display><Character>0BB80BCD0BB00BC0</Character><Keys>72</Keys></Map>
<Map Layer="LongPress"><Display>0BB6</Display><Character>0BB6</Character><Keys>67</Keys></Map>
`;

  xml = xml.replace('</Data>', additions + '</Data>');
  fs.writeFileSync(filePath, xml, 'utf8');
  console.log('Successfully patched:', filePath);
}

patchTamil26('D:/Users/Siva/Desktop/open-keyboard/keymaps/Tamil 26.xml');
patchTamil26('D:/Users/Siva/Desktop/open-keyboard/android/app/src/main/assets/keymaps/Tamil 26.xml');
patchTamil26('D:/Users/Siva/Desktop/open-keyboard/ios/Shared/Keymaps/Tamil 26.xml');
