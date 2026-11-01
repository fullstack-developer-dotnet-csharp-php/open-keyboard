const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const artDir = 'C:/Users/Siva/.gemini/antigravity-cli/brain/a9b6704e-33fd-4e2e-9eb1-4f131b27daa7';

(async () => {
  console.log('--- STARTING SIMULATOR E2E TESTS (Row 1 Sanskrit & Clean ASDF) ---');
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
  console.log('Testing: English Default Layout');
  await page.selectOption('#keymapSelect', 'English.xml');
  await page.waitForTimeout(400);

  // Assert keys
  const firstKeyText = await page.textContent('.gboard-row:nth-child(1) .gboard-key:nth-child(1) .key-main-text');
  console.log('First key text (expected q):', firstKeyText);
  if (firstKeyText !== 'q') throw new Error(`Expected 'q', got '${firstKeyText}'`);

  const thirdTextQ = await page.textContent('.gboard-row:nth-child(1) .gboard-key:nth-child(1) .key-third-text');
  console.log('Q 3rd text hint in English (expected 1):', thirdTextQ);
  if (thirdTextQ !== '1') throw new Error(`Expected '1', got '${thirdTextQ}'`);

  await page.screenshot({ path: path.join(artDir, 'test_1_english_default.png') });
  console.log('Saved test_1_english_default.png');

  // 2. Tamil 26 Layout Test: Sanskrit on Row 1, NONE on ASDF
  console.log('Testing: Tamil 26 Layout with Sanskrit on Row 1');
  await page.selectOption('#keymapSelect', 'Tamil 26.xml');
  await page.waitForTimeout(400);

  // Expected Row 1 keys: main Tamil, badge, Sanskrit 3rd text
  const expectedRow1 = [
    { key: 'q', tamil: 'ங்', sanskrit: 'ஸ்ரீ' },
    { key: 'w', tamil: 'ய்', sanskrit: 'க்ஷ' },
    { key: 'e', tamil: 'ச்', sanskrit: 'ஹ' },
    { key: 'r', tamil: 'ப்', sanskrit: 'ஷ' },
    { key: 't', tamil: 'ன்', sanskrit: 'ஸ' },
    { key: 'y', tamil: 'ஐ', sanskrit: 'ஜ' },
    { key: 'u', tamil: 'ட்', sanskrit: 'ஶ' },
    { key: 'i', tamil: 'ந்', sanskrit: 'ௐ' }
  ];

  for (let i = 0; i < expectedRow1.length; i++) {
    const item = expectedRow1[i];
    const keyLoc = page.locator(`.gboard-row:nth-child(1) .gboard-key:nth-child(${i + 1})`);
    const mainText = await keyLoc.locator('.key-main-text').textContent();
    const badgeText = await keyLoc.locator('.key-hint-badge').textContent();
    const sanskritText = await keyLoc.locator('.key-third-text').textContent();

    console.log(`Row 1 Key ${item.key}: main='${mainText}', badge='${badgeText}', sanskrit='${sanskritText}'`);
    if (mainText !== item.tamil) throw new Error(`Key ${item.key}: expected main '${item.tamil}', got '${mainText}'`);
    if (badgeText !== item.key) throw new Error(`Key ${item.key}: expected badge '${item.key}', got '${badgeText}'`);
    if (sanskritText !== item.sanskrit) throw new Error(`Key ${item.key}: expected Sanskrit '${item.sanskrit}', got '${sanskritText}'`);
  }
  console.log('✓ Row 1 has desktop Sanskrit mappings on q, w, e, r, t, y, u, i');

  // Verify ASDF keys have NO Sanskrit 3rd text hints
  const asdfKeys = ['a', 's', 'd', 'f'];
  for (const k of asdfKeys) {
    const count = await page.locator(`.gboard-key:has(.key-hint-badge:text-is("${k}")) .key-third-text`).count();
    console.log(`Key ${k} 3rd text count (expected 0):`, count);
    if (count !== 0) throw new Error(`Key ${k} should NOT have 3rd text, but found one!`);
  }
  console.log('✓ Keys ASDF successfully have all Sanskrit removed');

  await page.screenshot({ path: path.join(artDir, 'test_4_tamil26_default.png') });
  console.log('Saved test_4_tamil26_default.png');

  // 3. Typing Tests in Tamil 26: 2 times pressing s
  console.log('Testing: 2 times pressing s');
  await page.click('#btnClear');
  const sKeyLoc = page.locator(`.gboard-key:has(.key-hint-badge:text-is("s"))`).first();
  const hKeyLoc = page.locator(`.gboard-key:has(.key-hint-badge:text-is("h"))`).first();
  const yKeyLoc = page.locator(`.gboard-key:has(.key-hint-badge:text-is("y"))`).first();

  // Press s once -> க்
  await sKeyLoc.click();
  await page.waitForTimeout(100);
  let curVal = await page.inputValue('#editor');
  console.log('Pressed s once:', curVal, '(expected க்)');
  if (curVal !== 'க்') throw new Error(`Expected 'க்', got '${curVal}'`);

  // Press s twice -> க்க்
  await sKeyLoc.click();
  await page.waitForTimeout(100);
  curVal = await page.inputValue('#editor');
  console.log('Pressed s twice:', curVal, '(expected க்க்)');
  if (curVal !== 'க்க்') throw new Error(`Expected 'க்க்', got '${curVal}'`);

  // Press h -> becomes க்க
  await hKeyLoc.click();
  await page.waitForTimeout(100);
  curVal = await page.inputValue('#editor');
  console.log('Pressed h after ss:', curVal, '(expected க்க)');
  if (curVal !== 'க்க') throw new Error(`Expected 'க்க', got '${curVal}'`);

  // Press h again -> becomes க்கா
  await hKeyLoc.click();
  await page.waitForTimeout(100);
  curVal = await page.inputValue('#editor');
  console.log('Pressed h again:', curVal, '(expected க்கா)');
  if (curVal !== 'க்கா') throw new Error(`Expected 'க்கா', got '${curVal}'`);

  // Test s + y -> கை
  await page.click('#btnClear');
  await sKeyLoc.click();
  await page.waitForTimeout(100);
  await yKeyLoc.click();
  await page.waitForTimeout(100);
  curVal = await page.inputValue('#editor');
  console.log('Pressed s then y:', curVal, '(expected கை)');
  if (curVal !== 'கை') throw new Error(`Expected 'கை', got '${curVal}'`);
  console.log('✓ Verified 2 times pressing s and s+y combining work perfectly!');

  // 4. Test Long-Press on Row 1 (e.g. q -> ஸ்ரீ, w -> க்ஷ, e -> ஹ)
  console.log('Testing: Long-press on Row 1 (w -> க்ஷ)');
  await page.click('#btnClear');
  const wKeyLoc = page.locator('.gboard-row:nth-child(1) .gboard-key:nth-child(2)');
  // Simulate mousedown and wait 500ms
  const box = await wKeyLoc.boundingBox();
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(500);
  await page.mouse.up();
  await page.waitForTimeout(100);

  const typedLong = await page.inputValue('#editor');
  console.log('Long-pressed w (expected க்ஷ):', typedLong);
  if (typedLong !== 'க்ஷ') throw new Error(`Expected 'க்ஷ', got '${typedLong}'`);
  console.log('✓ Verified Row 1 long-press types Sanskrit directly!');

  await browser.close();
  console.log('--- ALL SIMULATOR TESTS PASSED SUCCESSFULLY! ---');
})();
