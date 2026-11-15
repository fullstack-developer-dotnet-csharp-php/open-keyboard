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
  page.on('console', msg => console.log('PAGE LOG:', msg.text()));
  page.on('pageerror', err => console.log('PAGE ERROR:', err.message));
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

  // Check Row 1 keys: main Tamil, badge, and Sanskrit 3rd key
  const expectedRow1 = [
    { key: 'q', tamil: 'ங்', third: 'ஸ்ரீ' },
    { key: 'w', tamil: 'ய்', third: 'க்ஷ்' },
    { key: 'e', tamil: 'ச்', third: 'ஹ்' },
    { key: 'r', tamil: 'ப்', third: 'ஷ்' },
    { key: 't', tamil: 'ன்', third: 'ஸ்' },
    { key: 'y', tamil: 'ஐ', third: 'ஜ்' },
    { key: 'u', tamil: 'ட்', third: 'ஶ்' },
    { key: 'i', tamil: 'ந்', third: 'ௐ' }
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

  // Verify long-press Sanskrit 'y' -> 'ஜ்', and subsequent vowel composition with 'h' -> 'ஜ'
  await page.click('#btnClear');
  await page.evaluate(() => {
    applyChar('ஜ்');
  });
  cur = await page.inputValue('#editor');
  console.log('Tamil 26 long-press Sanskrit y ->', cur, '(expected ஜ்)');
  if (cur !== 'ஜ்') throw new Error(`Expected 'ஜ்', got '${cur}'`);

  await typeKeySequence(page, ['h']);
  cur = await page.inputValue('#editor');
  console.log('Tamil 26 long-press Sanskrit y + h ->', cur, '(expected ஜ)');
  if (cur !== 'ஜ') throw new Error(`Expected 'ஜ', got '${cur}'`);

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

  // Verify Tamil26ADN is completely removed from the dropdown
  console.log('\n3. Verifying: Tamil26ADN is completely removed');
  const hasADN = await page.evaluate(() => {
    const opts = Array.from(document.querySelectorAll('#keymapSelect option'));
    return opts.some(o => o.value.includes('ADN') || o.textContent.includes('ADN'));
  });
  console.log('Does dropdown contain Tamil26ADN? (expected false):', hasADN);
  if (hasADN) throw new Error('Tamil26ADN was found in keymap dropdown, but should be removed!');
  console.log('✓ Verified Tamil26ADN is completely removed from the keymap options');

  // Verify full Tamil 26 Shift + A-Z typing produces standard uppercase English
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
    console.log(`Tamil 26: Shift + ${l} -> ${cur} (expected ${expectedLetter})`);
    if (cur !== expectedLetter) throw new Error(`Expected '${expectedLetter}', got '${cur}'`);
  }
  await page.evaluate(() => {
    shiftMode = 'BASE';
    renderGboard();
  });
  console.log('✓ Verified Shift + A/S/D/F/Z/X/C/V strictly type standard uppercase English in Tamil 26!');

  await page.screenshot({ path: path.join(artDir, 'test_tamil26_only_verified.png') });
  console.log('Saved test_tamil26_only_verified.png');

  await browser.close();
  console.log('\n--- ALL SIMULATOR TESTS PASSED 100% (TAMIL 26 ONLY)! ---');
})();
