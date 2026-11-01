const fs = require('fs');
const path = require('path');

const targetDir = 'D:/Users/Siva/Desktop/open-keyboard/android/app/src/main/assets/dictionaries';

// Load base lists
const baseEng = fs.readFileSync(path.join(targetDir, 'english_words.txt'), 'utf8').split('\n').filter(Boolean);
const baseTam = fs.readFileSync(path.join(targetDir, 'tamil_words.txt'), 'utf8').split('\n').filter(Boolean);

const engSet = new Set(baseEng);
// Add common English variations
const suffixes = ['s', 'ed', 'ing', 'ly', 'er', 'est', 'tion', 'ment', 'able', 'ness'];
baseEng.forEach(w => {
  if (w.length > 2) {
    engSet.add(w + 's');
    engSet.add(w + 'ed');
    engSet.add(w + 'ing');
  }
});
// Common English phrases and everyday words
const extraEng = [
  "how", "are", "you", "doing", "today", "tomorrow", "yesterday", "tonight", "morning", "afternoon", "evening",
  "welcome", "thanks", "thank", "please", "sorry", "excuse", "hello", "hi", "hey", "goodbye", "bye",
  "awesome", "wonderful", "amazing", "beautiful", "excellent", "perfect", "great", "nice", "fine", "cool",
  "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
  "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december",
  "keyboard", "typing", "android", "device", "mobile", "message", "screen", "display", "input", "output",
  "letter", "character", "alphabet", "language", "translate", "dictionary", "suggestion", "complete",
  "number", "symbol", "shift", "space", "enter", "backspace", "delete", "clear", "submit", "send",
  "google", "search", "chrome", "whatsapp", "facebook", "instagram", "twitter", "youtube", "email", "gmail"
];
extraEng.forEach(w => engSet.add(w.toLowerCase()));

// Tamil Set with case markers and conjugations
const tamSet = new Set(baseTam);
// Standard Tamil case endings & postpositions:
// -இல் (locative), -க்கு (dative), -ஐ (accusative), -உடன் (sociative), -ஆல் (instrumental), -இன் (genitive), -ஆக (adverbial), -கள் (plural)
const tamNouns = [
  "தமிழ்", "வணக்கம்", "நன்றி", "நாடு", "ஊர்", "வீடு", "பள்ளி", "கல்லூரி", "வேலை", "பணம்", "நேரம்",
  "மனிதன்", "மக்கள்", "தலைவர்", "ஆசிரியர்", "மாணவன்", "குழந்தை", "குடும்பம்", "நண்பன்", "அரசு",
  "உலகம்", "நீர்", "நிலம்", "காற்று", "மரம்", "பூ", "உணவு", "புத்தகம்", "பாடல்", "மொழி", "செய்தி"
];

tamNouns.forEach(n => {
  tamSet.add(n + "ில்");
  tamSet.add(n + "க்கு");
  tamSet.add(n + "ுக்கு");
  tamSet.add(n + "ை");
  tamSet.add(n + "யை");
  tamSet.add(n + "ுடன்");
  tamSet.add(n + "ோடு");
  tamSet.add(n + "ால்");
  tamSet.add(n + "ின்");
  tamSet.add(n + "ாக");
  tamSet.add(n + "கள்");
  tamSet.add(n + "களில்");
  tamSet.add(n + "களுக்கு");
  tamSet.add(n + "களை");
});

// Common conversational Tamil words
const extraTam = [
  "எப்படி", "இருக்கிறீர்கள்", "நலமா", "என்ன", "விஷயம்", "செய்கிறீர்கள்", "சாப்பிட்டீர்களா", "எப்போது",
  "வருவீர்கள்", "வருகிறேன்", "போகிறேன்", "போய்விட்டு", "வருகிறேன்", "சரி", "ஆகட்டும்", "கண்டிப்பாக",
  "நிச்சயமாக", "உண்மை", "பொய்", "தெரியும்", "தெரியாது", "புரிகிறது", "புரியவில்லை", "பிடிக்கும்",
  "பிடிக்காது", "வேண்டும்", "வேண்டாம்", "முடியும்", "முடியாது", "இருக்கட்டும்", "பார்க்கலாம்", "பேசலாம்",
  "வாழ்க", "வளர்க", "வெல்க", "மகிழ்ச்சி", "வாழ்த்துகள்", "வாழ்த்துக்கள்", "நல்வாழ்த்துகள்", "அன்புடன்",
  "வணக்கங்கள்", "அன்புள்ள", "மதிப்பிற்குரிய", "வணக்கத்திற்குரிய", "நண்பா", "தோழா", "வணக்கம்ங்க", "நன்றிங்க"
];
extraTam.forEach(w => tamSet.add(w));

const finalEng = Array.from(engSet).filter(w => /^[a-z]+$/.test(w) && w.length >= 2).sort();
const finalTam = Array.from(tamSet).sort();

fs.writeFileSync(path.join(targetDir, 'english_words.txt'), finalEng.join('\n'), 'utf8');
fs.writeFileSync(path.join(targetDir, 'tamil_words.txt'), finalTam.join('\n'), 'utf8');

console.log(`Final English words: ${finalEng.length}`);
console.log(`Final Tamil words: ${finalTam.length}`);
