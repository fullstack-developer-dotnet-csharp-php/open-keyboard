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
  console.log('Q 3rd text hint (expected 1):', thirdTextQ);
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

  // Check 3rd text on Row 1 (numbers 1..0, NOT Tamil numbers)
  const t26_q_third = await page.textContent('.gboard-row:nth-child(1) .gboard-key:nth-child(1) .key-third-text');
  console.log('Tamil 26 Q 3rd text (expected 1):', t26_q_third);
  if (t26_q_third !== '1') throw new Error(`Expected '1', got '${t26_q_third}'`);

  // Check Sanskrit on 'a' (expected ஷ)
  // 'a' is key index 1 in row 2
  const a_third = await page.textContent('.gboard-row:nth-child(2) .gboard-key:nth-child(1) .key-third-text');
  console.log('Tamil 26 key "a" 3rd text (expected Sanskrit ஷ):', a_third);
  if (a_third !== 'ஷ') throw new Error(`Expected 'ஷ', got '${a_third}'`);

  // Check Sanskrit on 's' (expected ஸ)
  const s_third = await page.textContent('.gboard-row:nth-child(2) .gboard-key:nth-child(2) .key-third-text');
  console.log('Tamil 26 key "s" 3rd text (expected Sanskrit ஸ):', s_third);
  if (s_third !== 'ஸ') throw new Error(`Expected 'ஸ', got '${s_third}'`);

  // Check Sanskrit on 'd' (expected ஹ)
  const d_third = await page.textContent('.gboard-row:nth-child(2) .gboard-key:nth-child(3) .key-third-text');
  console.log('Tamil 26 key "d" 3rd text (expected Sanskrit ஹ):', d_third);
  if (d_third !== 'ஹ') throw new Error(`Expected 'ஹ', got '${d_third}'`);

  // Check Sanskrit on 'f' (expected ஜ)
  const f_third = await page.textContent('.gboard-row:nth-child(2) .gboard-key:nth-child(4) .key-third-text');
  console.log('Tamil 26 key "f" 3rd text (expected Sanskrit ஜ):', f_third);
  if (f_third !== 'ஜ') throw new Error(`Expected 'ஜ', got '${f_third}'`);

  // Check Sanskrit on 'z' (expected ஸ்ரீ)
  const z_third = await page.textContent('.gboard-row:nth-child(3) .gboard-key:nth-child(2) .key-third-text');
  console.log('Tamil 26 key "z" 3rd text (expected Sanskrit ஸ்ரீ):', z_third);
  if (z_third !== 'ஸ்ரீ') throw new Error(`Expected 'ஸ்ரீ', got '${z_third}'`);

  // Check top-right ASCII reference badges exist in Tamil 26
  const t26_badges = await page.locator('.key-hint-badge').count();
  console.log('Tamil 26 reference badges count (expected > 20):', t26_badges);
  if (t26_badges < 20) throw new Error(`Expected >20 badges, got ${t26_badges}`);

  await page.screenshot({ path: path.join(artDir, 'test_4_tamil26_default.png') });
  console.log('Saved test_4_tamil26_default.png');

  // 5. Tamil 26 CAPS Shift Test
  console.log('Testing: Tamil 26 CAPS Shift mode');
  await page.click('.gboard-key.shift');
  await page.waitForTimeout(300);

  const t26_caps_q = await page.textContent('.gboard-row:nth-child(1) .gboard-key:nth-child(1) .key-main-text');
  console.log('Tamil 26 key in CAPS mode (expected Q):', t26_caps_q);
  if (t26_caps_q !== 'Q') throw new Error(`Expected 'Q', got '${t26_caps_q}'`);

  await page.screenshot({ path: path.join(artDir, 'test_5_tamil26_caps.png') });
  console.log('Saved test_5_tamil26_caps.png');

  // 6. Tamil 26 Small English (Star ★) Shift Test
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

  // 7. Test Dropdown Menu (🔄 Sync button next to smiley)
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
