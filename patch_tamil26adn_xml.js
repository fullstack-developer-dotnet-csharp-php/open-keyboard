const fs = require('fs');

function patchTamil26ADN(filePath) {
  let xml = fs.readFileSync(filePath, 'utf8');

  if (!xml.includes('Layer="LongPress"')) {
    const additions = `\n<!-- Sanskrit / Grantha Long-Press Mappings & Space -->
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
    console.log('Successfully patched Tamil26ADN:', filePath);
  } else {
    console.log('Already contains LongPress layer:', filePath);
  }
}

patchTamil26ADN('D:/Users/Siva/Desktop/open-keyboard/keymaps/Tamil26ADN.xml');
patchTamil26ADN('D:/Users/Siva/Desktop/open-keyboard/android/app/src/main/assets/keymaps/Tamil26ADN.xml');
patchTamil26ADN('D:/Users/Siva/Desktop/open-keyboard/ios/Shared/Keymaps/Tamil26ADN.xml');
