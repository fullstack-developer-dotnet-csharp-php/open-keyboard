const fs = require('fs');
const path = require('path');

const keymapsDir = 'D:/Users/Siva/Desktop/open-keyboard/keymaps';
const files = fs.readdirSync(keymapsDir).filter(f => f.endsWith('.xml'));

const bundle = {};
for (const f of files) {
  bundle[f] = fs.readFileSync(path.join(keymapsDir, f), 'utf8');
}

const outContent = 'const BUNDLED_KEYMAPS = ' + JSON.stringify(bundle, null, 2) + ';\n';
fs.writeFileSync('D:/Users/Siva/Desktop/open-keyboard/web-simulator/bundled_keymaps.js', outContent, 'utf8');
console.log(`Updated web-simulator/bundled_keymaps.js with ${files.length} keymaps.`);
