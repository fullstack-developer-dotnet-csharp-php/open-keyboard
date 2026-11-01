const fs = require('fs');

function testKeymap(filePath) {
  console.log('Testing keymap:', filePath);
  const xml = fs.readFileSync(filePath, 'utf8');
  
  function hexToStr(hex) {
    let str = '';
    for (let i = 0; i < hex.length; i += 4) {
      str += String.fromCharCode(parseInt(hex.substr(i, 4), 16));
    }
    return str;
  }

  const defaultMap = {};
  const regex = /<Map(\s+Layer="([^"]*)")?[^>]*><Display>(.*?)<\/Display><Character>(.*?)<\/Character><Keys>(.*?)<\/Keys><\/Map>/gi;
  let m;
  while ((m = regex.exec(xml)) !== null) {
    const layer = m[2] || '';
    if (!layer || layer === 'Tamil') {
      const keys = Buffer.from(m[5], 'hex').toString('utf8');
      defaultMap[keys] = hexToStr(m[4]);
    }
  }

  console.log('Key y maps to:', defaultMap['y']);
  console.log('Key i maps to:', defaultMap['i']);
  console.log('Key j maps to:', defaultMap['j']);
  
  // Find all combinations with y
  const withY = Object.keys(defaultMap).filter(k => k.includes('y'));
  console.log(`Total rules containing y: ${withY.length}`);
  console.log('Sample rules with y:', withY.slice(0, 15).map(k => `${k} -> ${defaultMap[k]}`).join(', '));

  // Find all combinations with i
  const withI = Object.keys(defaultMap).filter(k => k.includes('i'));
  console.log(`Total rules containing i: ${withI.length}`);
  console.log('Sample rules with i:', withI.slice(0, 15).map(k => `${k} -> ${defaultMap[k]}`).join(', '));
}

testKeymap('D:/Users/Siva/Desktop/open-keyboard/keymaps/Tamil 26.xml');
console.log('----------------------------------------------------');
testKeymap('D:/Users/Siva/Desktop/open-keyboard/keymaps/Tamil26ADN.xml');
