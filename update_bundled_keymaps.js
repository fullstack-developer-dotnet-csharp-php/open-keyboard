const fs = require("fs");
const path = require("path");

const keymapsDir = "D:/Users/Siva/Desktop/open-keyboard/keymaps";
const files = fs.readdirSync(keymapsDir).filter(f => f.endsWith(".xml"));

const bundle = {};

for (const f of files) {
  const content = fs.readFileSync(path.join(keymapsDir, f), "utf8");
  const lang = content.match(/<Language>(.*?)<\/Language>/)?.[1] || "Custom";
  const enc = content.match(/<Encoding>(.*?)<\/Encoding>/)?.[1] || "Unicode";
  const kbd = content.match(/<Keyboard>(.*?)<\/Keyboard>/)?.[1] || f.replace(".xml", "");
  const desc = content.match(/<Description>(.*?)<\/Description>/)?.[1] || "";
  const author = content.match(/<Author>(.*?)<\/Author>/)?.[1] || "Open Keyboard";
  
  bundle[f] = {
    filename: f,
    language: lang,
    encoding: enc,
    keyboard: kbd,
    author: author,
    description: desc,
    xml: content
  };
}

fs.writeFileSync(
  "D:/Users/Siva/Desktop/open-keyboard/web-simulator/bundled_keymaps.js",
  "window.BUNDLED_KEYMAPS = " + JSON.stringify(bundle) + ";",
  "utf8"
);

console.log("Successfully bundled", Object.keys(bundle).length, "keymaps into bundled_keymaps.js");