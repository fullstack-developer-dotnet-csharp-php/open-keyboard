const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const artDir = 'C:/Users/Siva/.gemini/antigravity-cli/brain/a9b6704e-33fd-4e2e-9eb1-4f131b27daa7';

(async () => {
  console.log('--- STARTING SIMULATOR E2E TESTS ---');
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

  const hintBadgeCount = await page.locator('.key-hint-badge').count();
  console.log('Hint badges in English (expected 0):', hintBadgeCount);
  if (hintBadgeCount !== 0) throw new Error(`Expected 0 hint badges in English, got ${hintBadgeCount}`);

  await page.screenshot({ path: path.join(artDir, 'test_1_english_default.png') });
  console.log('Saved test_1_english_default.png');

  // 2. English CAPS Shift Test
  console.log('Testing: English CAPS Shift');
  await page.click('.gboard-key.shift');
  await page.waitForTimeout(300);

  const firstKeyCaps = await page.textContent('.gboard-row:nth-child(1) .gboard-key:nth-child(1) .key-main-text');
  console.log('First key in CAPS (expected Q):', firstKeyCaps);
  if (firstKeyCaps !== 'Q') throw new Error(`Expected 'Q', got '${firstKeyCaps}'`);

  await page.screenshot({ path: path.join(artDir, 'test_2_english_caps.png') });
  console.log('Saved test_2_english_caps.png');

  // Return to lowercase
  await page.click('.gboard-key.shift');
  await page.waitForTimeout(300);

  // 3. Type text into Notepad
  console.log('Testing: Typing in English');
  await page.click('#btnClear');
  // Type 'open'
  const keysToClick = ['o', 'p', 'e', 'n'];
  for (const k of keysToClick) {
    const keyLoc = page.locator(`.gboard-key:has(.key-main-text:text-is("${k}"))`).first();
    await keyLoc.click();
    await page.waitForTimeout(100);
  }
  const editorVal = await page.inputValue('#editor');
  console.log('Editor text (expected open):', editorVal);
  if (editorVal !== 'open') throw new Error(`Expected 'open', got '${editorVal}'`);

  await page.screenshot({ path: path.join(artDir, 'test_3_english_typed.png') });
  console.log('Saved test_3_english_typed.png');

  // 4. Tamil 26 Layout Test
  console.log('Testing: Tamil 26 Layout');
  await page.selectOption('#keymapSelect', 'Tamil 26.xml');
  await page.waitForTimeout(400);

  // Verify qwertyu keycaps
  const expectedRow1 = [
    { key: 'q', tamil: 'ங்' },
    { key: 'w', tamil: 'ய்' },
    { key: 'e', tamil: 'ச்' },
    { key: 'r', tamil: 'ப்' },
    { key: 't', tamil: 'ன்' },
    { key: 'y', tamil: 'ஐ' },
    { key: 'u', tamil: 'ட்' }
  ];

  for (let i = 0; i < expectedRow1.length; i++) {
    const item = expectedRow1[i];
    const keyLoc = page.locator(`.gboard-row:nth-child(1) .gboard-key:nth-child(${i + 1})`);
    const mainText = await keyLoc.locator('.key-main-text').textContent();
    const badgeText = await keyLoc.locator('.key-hint-badge').textContent();
    console.log(`Row 1 Key ${item.key}: mainText='${mainText}', badge='${badgeText}' (expected '${item.tamil}', '${item.key}')`);
    if (mainText !== item.tamil) throw new Error(`Key ${item.key}: expected main text '${item.tamil}', got '${mainText}'`);
    if (badgeText !== item.key) throw new Error(`Key ${item.key}: expected badge '${item.key}', got '${badgeText}'`);

    // Verify row 1 has NO numbers 1..0 on Tamil 26
    const thirdLoc = keyLoc.locator('.key-third-text');
    const hasThird = await thirdLoc.count();
    if (hasThird > 0) {
      const thirdVal = await thirdLoc.textContent();
      throw new Error(`Row 1 key ${item.key} should not have 3rd text, got '${thirdVal}'`);
    }
  }
  console.log('✓ Verified qwertyu keycaps match desktop XML and have no numbers on row 1');

  // Check Sanskrit hints on 'a', 's', 'd', 'f', 'z'
  const a_third = await page.locator('.gboard-key:has(.key-hint-badge:text-is("a")) .key-third-text').textContent();
  console.log('Tamil 26 key "a" 3rd text (expected Sanskrit ஷ):', a_third);
  if (a_third !== 'ஷ') throw new Error(`Expected 'ஷ', got '${a_third}'`);

  const s_third = await page.locator('.gboard-key:has(.key-hint-badge:text-is("s")) .key-third-text').textContent();
  console.log('Tamil 26 key "s" 3rd text (expected Sanskrit ஸ):', s_third);
  if (s_third !== 'ஸ') throw new Error(`Expected 'ஸ', got '${s_third}'`);

  const d_third = await page.locator('.gboard-key:has(.key-hint-badge:text-is("d")) .key-third-text').textContent();
  console.log('Tamil 26 key "d" 3rd text (expected Sanskrit ஹ):', d_third);
  if (d_third !== 'ஹ') throw new Error(`Expected 'ஹ', got '${d_third}'`);

  const f_third = await page.locator('.gboard-key:has(.key-hint-badge:text-is("f")) .key-third-text').textContent();
  console.log('Tamil 26 key "f" 3rd text (expected Sanskrit ஜ):', f_third);
  if (f_third !== 'ஜ') throw new Error(`Expected 'ஜ', got '${f_third}'`);

  await page.screenshot({ path: path.join(artDir, 'test_4_tamil26_default.png') });
  console.log('Saved test_4_tamil26_default.png');

  // 5. Typing Tests in Tamil 26: 2 times pressing s
  console.log('Testing: 2 times pressing s');
  await page.click('#btnClear');
  const sKeyLoc = page.locator('.gboard-key:has(.key-hint-badge:text-is("s"))').first();
  const hKeyLoc = page.locator('.gboard-key:has(.key-hint-badge:text-is("h"))').first();
  const yKeyLoc = page.locator('.gboard-key:has(.key-hint-badge:text-is("y"))').first();

  // Press s once
  await sKeyLoc.click();
  await page.waitForTimeout(100);
  let curVal = await page.inputValue('#editor');
  console.log('Pressed s once:', curVal, '(expected க்)');
  if (curVal !== 'க்') throw new Error(`Expected 'க்', got '${curVal}'`);

  // Press s twice
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
  console.log('✓ Verified 2 times pressing s and s+y combining work perfectly according to original XML!');

  // 6. Tamil 26 CAPS Shift Test
  console.log('Testing: Tamil 26 CAPS Shift mode');
  await page.click('.gboard-key.shift');
  await page.waitForTimeout(300);

  const t26_caps_q = await page.textContent('.gboard-row:nth-child(1) .gboard-key:nth-child(1) .key-main-text');
  console.log('Tamil 26 key in CAPS mode (expected Q):', t26_caps_q);
  if (t26_caps_q !== 'Q') throw new Error(`Expected 'Q', got '${t26_caps_q}'`);

  await page.screenshot({ path: path.join(artDir, 'test_5_tamil26_caps.png') });
  console.log('Saved test_5_tamil26_caps.png');

  // 7. Tamil 26 Small English (Star ★) Shift Test
  console.log('Testing: Tamil 26 Small English mode (star ★)');
  await page.click('.gboard-key.shift');
  await page.waitForTimeout(300);

  const t26_small_q = await page.textContent('.gboard-row:nth-child(1) .gboard-key:nth-child(1) .key-main-text');
  console.log('Tamil 26 key in Small English mode (expected q):', t26_small_q);
  if (t26_small_q !== 'q') throw new Error(`Expected 'q', got '${t26_small_q}'`);

  await page.screenshot({ path: path.join(artDir, 'test_6_tamil26_small_english.png') });
  console.log('Saved test_6_tamil26_small_english.png');

  // Return to Tamil mode
  await page.click('.gboard-key.shift');
  await page.waitForTimeout(300);

  // 8. Test Dropdown Menu (🔄 Sync button next to smiley)
  console.log('Testing: Dropdown Menu (🔄 button)');
  await page.click('.gboard-row:nth-child(4) .gboard-key:nth-child(3)');
  await page.waitForTimeout(300);

  const isDropdownVisible = await page.isVisible('#layoutDropdown');
  console.log('Dropdown menu visible (expected true):', isDropdownVisible);
  if (!isDropdownVisible) throw new Error('Expected dropdown to be visible');

  await page.screenshot({ path: path.join(artDir, 'test_7_dropdown_open.png') });
  console.log('Saved test_7_dropdown_open.png');

  await browser.close();
  console.log('--- ALL SIMULATOR TESTS PASSED SUCCESSFULLY! ---');
})();
