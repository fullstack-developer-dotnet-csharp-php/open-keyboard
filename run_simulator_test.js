const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const artDir = 'C:/Users/Siva/.gemini/antigravity-cli/brain/a9b6704e-33fd-4e2e-9eb1-4f131b27daa7';

async function typeKeySequence(page, keys) {
  for (const k of keys) {
    const keyLoc = page.locator(`.gboard-key:has(.key-hint-badge:text-is("${k}")), .gboard-key:has(.key-main-text:text-is("${k}"))`).first();
    await keyLoc.click();
    await page.waitForTimeout(60);
  }
}

(async () => {
  console.log('--- STARTING SIMULATOR E2E TESTS (Tamil 26 & Tamil26ADN Sanskrit & Shift Alignment) ---');
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe'
  });
  const page = await browser.newPage();
  await page.setViewportSize({ width: 440, height: 950 });

  const htmlUrl = 'file:///' + path.resolve('D:/Users/Siva/Desktop/open-keyboard/web-simulator/index.html').replace(/\\/g, '/');
  console.log('Navigating to simulator:', htmlUrl);
  await page.goto(htmlUrl);
  await page.waitForTimeout(600);

  // 1. English Default Layout Test
  console.log('1. Testing: English Default Layout');
  await page.selectOption('#keymapSelect', 'English.xml');
  await page.waitForTimeout(400);

  const firstKeyText = await page.textContent('.gboard-row:nth-child(1) .gboard-key:nth-child(1) .key-main-text');
  console.log('First key text (expected q):', firstKeyText);
  if (firstKeyText !== 'q') throw new Error(`Expected 'q', got '${firstKeyText}'`);

  const thirdTextQ = await page.textContent('.gboard-row:nth-child(1) .gboard-key:nth-child(1) .key-third-text');
  console.log('Q 3rd text hint in English (expected 1):', thirdTextQ);
  if (thirdTextQ !== '1') throw new Error(`Expected '1', got '${thirdTextQ}'`);

  await page.screenshot({ path: path.join(artDir, 'test_1_english_default.png') });

  // 2. Tamil 26 Layout Test
  console.log('\n2. Testing: Tamil 26 Layout');
  await page.selectOption('#keymapSelect', 'Tamil 26.xml');
  await page.waitForTimeout(400);

  // Check Row 1 keys: main Tamil & badge
  const expectedRow1 = [
    { key: 'q', tamil: 'ங்', third: '1' },
    { key: 'w', tamil: 'ய்', third: '2' },
    { key: 'e', tamil: 'ச்', third: '3' },
    { key: 'r', tamil: 'ப்', third: '4' },
    { key: 't', tamil: 'ன்', third: '5' },
    { key: 'y', tamil: 'ஐ', third: '6' },
    { key: 'u', tamil: 'ட்', third: '7' },
    { key: 'i', tamil: 'ந்', third: '8' }
  ];

  for (let i = 0; i < expectedRow1.length; i++) {
    const item = expectedRow1[i];
    const keyLoc = page.locator(`.gboard-row:nth-child(1) .gboard-key:nth-child(${i + 1})`);
    const mainText = await keyLoc.locator('.key-main-text').textContent();
    const badgeText = await keyLoc.locator('.key-hint-badge').textContent();
    const thirdText = await keyLoc.locator('.key-third-text').textContent();

    console.log(`Tamil 26 Row 1 Key ${item.key}: main='${mainText}', badge='${badgeText}', third='${thirdText}'`);
    if (mainText !== item.tamil) throw new Error(`Key ${item.key}: expected main '${item.tamil}', got '${mainText}'`);
    if (badgeText !== item.key) throw new Error(`Key ${item.key}: expected badge '${item.key}', got '${badgeText}'`);
    if (thirdText !== item.third) throw new Error(`Key ${item.key}: expected third '${item.third}', got '${thirdText}'`);
  }
  console.log('✓ Row 1 Tamil 26 keycaps correct');

  // Verify double 's' typing in Tamil 26
  await page.click('#btnClear');
  await typeKeySequence(page, ['s']);
  let cur = await page.inputValue('#editor');
  console.log('Tamil 26: pressed s once ->', cur);
  if (cur !== 'க்') throw new Error(`Expected 'க்', got '${cur}'`);

  await typeKeySequence(page, ['s']);
  cur = await page.inputValue('#editor');
  console.log('Tamil 26: pressed s twice ->', cur);
  if (cur !== 'க்க்') throw new Error(`Expected 'க்க்', got '${cur}'`);

  await typeKeySequence(page, ['h']);
  cur = await page.inputValue('#editor');
  console.log('Tamil 26: pressed s+s+h ->', cur);
  if (cur !== 'க்க') throw new Error(`Expected 'க்க', got '${cur}'`);

  await typeKeySequence(page, ['h']);
  cur = await page.inputValue('#editor');
  console.log('Tamil 26: pressed s+s+h+h ->', cur);
  if (cur !== 'க்கா') throw new Error(`Expected 'க்கா', got '${cur}'`);

  // Verify Sanskrit modifier (+ g) in Tamil 26:
  // q + g -> ஸ்ரீ
  await page.click('#btnClear');
  await typeKeySequence(page, ['q', 'g']);
  cur = await page.inputValue('#editor');
  console.log('Tamil 26: q + g ->', cur, '(expected ஸ்ரீ)');
  if (cur !== 'ஸ்ரீ') throw new Error(`Expected 'ஸ்ரீ', got '${cur}'`);

  // t + g -> ஸ், + h -> ஸ
  await page.click('#btnClear');
  await typeKeySequence(page, ['t', 'g']);
  cur = await page.inputValue('#editor');
  console.log('Tamil 26: t + g ->', cur, '(expected ஸ்)');
  if (cur !== 'ஸ்') throw new Error(`Expected 'ஸ்', got '${cur}'`);

  await typeKeySequence(page, ['h']);
  cur = await page.inputValue('#editor');
  console.log('Tamil 26: t + g + h ->', cur, '(expected ஸ)');
  if (cur !== 'ஸ') throw new Error(`Expected 'ஸ', got '${cur}'`);

  // Shift + A in Tamil 26 -> English 'A'
  await page.click('#btnClear');
  await page.evaluate(() => {
    shiftMode = 'CAPS_ENGLISH';
    renderGboard();
  });
  await page.waitForTimeout(100);
  const aKeyLoc = page.locator(`.gboard-key:has(.key-hint-badge:text-is("a")), .gboard-key:has(.key-main-text:text-is("A"))`).first();
  await aKeyLoc.click();
  await page.waitForTimeout(100);
  cur = await page.inputValue('#editor');
  console.log('Tamil 26: Shift + a ->', cur, '(expected A)');
  if (cur !== 'A') throw new Error(`Expected 'A', got '${cur}'`);

  await page.evaluate(() => {
    shiftMode = 'BASE';
    renderGboard();
  });

  // 3. Tamil26ADN Layout Test
  console.log('\n3. Testing: Tamil26ADN Layout');
  await page.selectOption('#keymapSelect', 'Tamil26ADN.xml');
  await page.waitForTimeout(400);

  // Verify Sanskrit modifier (+ g) in Tamil26ADN (same as Tamil 26!):
  // q + g -> ஸ்ரீ
  await page.click('#btnClear');
  await typeKeySequence(page, ['q', 'g']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: q + g ->', cur, '(expected ஸ்ரீ)');
  if (cur !== 'ஸ்ரீ') throw new Error(`Expected 'ஸ்ரீ', got '${cur}'`);

  // t + g -> ஸ், + h -> ஸ
  await page.click('#btnClear');
  await typeKeySequence(page, ['t', 'g']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: t + g ->', cur, '(expected ஸ்)');
  if (cur !== 'ஸ்') throw new Error(`Expected 'ஸ்', got '${cur}'`);

  await typeKeySequence(page, ['h']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: t + g + h ->', cur, '(expected ஸ)');
  if (cur !== 'ஸ') throw new Error(`Expected 'ஸ', got '${cur}'`);

  // y + g -> ஜ், + h -> ஜ
  await page.click('#btnClear');
  await typeKeySequence(page, ['y', 'g']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: y + g ->', cur, '(expected ஜ்)');
  if (cur !== 'ஜ்') throw new Error(`Expected 'ஜ்', got '${cur}'`);

  await typeKeySequence(page, ['h']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: y + g + h ->', cur, '(expected ஜ)');
  if (cur !== 'ஜ') throw new Error(`Expected 'ஜ', got '${cur}'`);

  // e + g -> ஹ், + h -> ஹ
  await page.click('#btnClear');
  await typeKeySequence(page, ['e', 'g']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: e + g ->', cur, '(expected ஹ்)');
  if (cur !== 'ஹ்') throw new Error(`Expected 'ஹ்', got '${cur}'`);

  await typeKeySequence(page, ['h']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: e + g + h ->', cur, '(expected ஹ)');
  if (cur !== 'ஹ') throw new Error(`Expected 'ஹ', got '${cur}'`);

  // w + g -> க்ஷ், + h -> க்ஷ
  await page.click('#btnClear');
  await typeKeySequence(page, ['w', 'g']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: w + g ->', cur, '(expected க்ஷ்)');
  if (cur !== 'க்ஷ்') throw new Error(`Expected 'க்ஷ்', got '${cur}'`);

  await typeKeySequence(page, ['h']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: w + g + h ->', cur, '(expected க்ஷ)');
  if (cur !== 'க்ஷ') throw new Error(`Expected 'க்ஷ', got '${cur}'`);

  // r + g -> ஷ், + h -> ஷ
  await page.click('#btnClear');
  await typeKeySequence(page, ['r', 'g']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: r + g ->', cur, '(expected ஷ்)');
  if (cur !== 'ஷ்') throw new Error(`Expected 'ஷ்', got '${cur}'`);

  await typeKeySequence(page, ['h']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: r + g + h ->', cur, '(expected ஷ)');
  if (cur !== 'ஷ') throw new Error(`Expected 'ஷ', got '${cur}'`);

  // m + g -> ஃ
  await page.click('#btnClear');
  await typeKeySequence(page, ['m', 'g']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: m + g ->', cur, '(expected ஃ)');
  if (cur !== 'ஃ') throw new Error(`Expected 'ஃ', got '${cur}'`);

  // Verify Shift + A, S, D, F in Tamil26ADN produces capital English letters, NOT Sanskrit!
  const lettersToTest = ['a', 's', 'd', 'f', 'z', 'x', 'c', 'v'];
  for (const l of lettersToTest) {
    await page.click('#btnClear');
    await page.evaluate(() => {
      shiftMode = 'CAPS_ENGLISH';
      renderGboard();
    });
    await page.waitForTimeout(100);
    const keyLoc = page.locator(`.gboard-key:has(.key-hint-badge:text-is("${l}")), .gboard-key:has(.key-main-text:text-is("${l.toUpperCase()}"))`).first();
    await keyLoc.click();
    await page.waitForTimeout(100);
    cur = await page.inputValue('#editor');
    const expectedLetter = l.toUpperCase();
    console.log(`Tamil26ADN: Shift + ${l} -> ${cur} (expected ${expectedLetter})`);
    if (cur !== expectedLetter) throw new Error(`Expected '${expectedLetter}', got '${cur}'`);
  }
  await page.evaluate(() => {
    shiftMode = 'BASE';
    renderGboard();
  });
  console.log('✓ Verified Shift + A/S/D/F/Z/X/C/V strictly type standard uppercase English in Tamil26ADN!');

  // Verify Tamil26ADN double consonants (e.g. s+s -> க்க, s+s+h -> க்கா, a+a -> த்த, a+a+h -> த்தா)
  await page.click('#btnClear');
  await typeKeySequence(page, ['s', 's']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: s + s ->', cur, '(expected க்க)');
  if (cur !== 'க்க') throw new Error(`Expected 'க்க', got '${cur}'`);

  await page.click('#btnClear');
  await typeKeySequence(page, ['s', 's', 'h']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: s + s + h ->', cur, '(expected க்கா)');
  if (cur !== 'க்கா') throw new Error(`Expected 'க்கா', got '${cur}'`);

  await page.click('#btnClear');
  await typeKeySequence(page, ['a', 'a']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: a + a ->', cur, '(expected த்த)');
  if (cur !== 'த்த') throw new Error(`Expected 'த்த', got '${cur}'`);

  await page.click('#btnClear');
  await typeKeySequence(page, ['a', 'a', 'h']);
  cur = await page.inputValue('#editor');
  console.log('Tamil26ADN: a + a + h ->', cur, '(expected த்தா)');
  if (cur !== 'த்தா') throw new Error(`Expected 'த்தா', got '${cur}'`);

  await page.screenshot({ path: path.join(artDir, 'test_tamil26adn_verified.png') });
  console.log('Saved test_tamil26adn_verified.png');

  await browser.close();
  console.log('\n--- ALL SIMULATOR TESTS PASSED 100% SUCCESSFULLY! ---');
})();
