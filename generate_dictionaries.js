const fs = require('fs');
const path = require('path');

const targetDir = 'D:/Users/Siva/Desktop/open-keyboard/android/app/src/main/assets/dictionaries';
if (!fs.existsSync(targetDir)) {
  fs.mkdirSync(targetDir, { recursive: true });
}

// 1. Comprehensive Common English Words (Open source frequency list)
const englishWords = [
  "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
  "this", "but", "his", "by", "from", "they", "we", "say", "her", "she", "or", "an", "will", "my", "one", "all", "would", "there",
  "their", "what", "so", "up", "out", "if", "about", "who", "get", "which", "go", "me", "when", "make", "can", "like", "time", "no",
  "just", "him", "know", "take", "people", "into", "year", "your", "good", "some", "could", "them", "see", "other", "than", "then",
  "now", "look", "only", "come", "its", "over", "think", "also", "back", "after", "use", "two", "how", "our", "work", "first",
  "well", "way", "even", "new", "want", "because", "any", "these", "give", "day", "most", "us", "is", "are", "was", "were", "been",
  "has", "had", "done", "said", "saying", "went", "going", "made", "making", "knew", "knowing", "took", "taking", "saw", "seeing",
  "came", "coming", "thought", "thinking", "looked", "looking", "wanted", "wanting", "gave", "giving", "used", "using", "found", "finding",
  "told", "telling", "asked", "asking", "worked", "working", "seemed", "seeming", "felt", "feeling", "tried", "trying", "left", "leaving",
  "called", "calling", "great", "small", "big", "large", "high", "long", "little", "own", "old", "young", "right", "left", "next",
  "early", "late", "important", "few", "public", "bad", "same", "able", "different", "general", "possible", "whole", "free", "military",
  "true", "full", "special", "easy", "clear", "recent", "certain", "personal", "open", "red", "difficult", "available", "likely",
  "short", "single", "medical", "current", "wrong", "private", "past", "foreign", "fine", "common", "poor", "natural", "significant",
  "similar", "hot", "dead", "central", "happy", "serious", "ready", "simple", "left", "physical", "general", "environmental", "financial",
  "blue", "democratic", "dark", "various", "entire", "close", "legal", "religious", "cold", "final", "main", "green", "nice", "huge",
  "popular", "traditional", "cultural", "national", "international", "local", "social", "political", "economic", "federal", "ready",
  "white", "black", "heavy", "light", "strong", "weak", "fast", "slow", "hard", "soft", "sweet", "clean", "fresh", "wide", "deep",
  "world", "life", "hand", "part", "child", "eye", "woman", "place", "case", "week", "company", "system", "program", "question",
  "government", "number", "night", "point", "home", "water", "room", "mother", "area", "money", "story", "fact", "month", "lot",
  "study", "book", "eye", "job", "word", "business", "issue", "side", "kind", "head", "house", "service", "friend", "father", "power",
  "hour", "game", "line", "end", "member", "law", "car", "city", "community", "name", "president", "team", "minute", "idea", "kid",
  "body", "information", "back", "parent", "face", "others", "level", "office", "door", "health", "person", "art", "war", "history",
  "party", "result", "change", "morning", "reason", "research", "girl", "guy", "moment", "air", "teacher", "force", "education",
  "foot", "boy", "age", "policy", "everything", "love", "process", "music", "including", "consider", "appear", "buy", "wait",
  "serve", "die", "send", "expect", "build", "stay", "fall", "cut", "reach", "kill", "remain", "suggest", "raise", "pass", "sell",
  "require", "report", "decide", "pull", "break", "support", "hit", "produce", "eat", "cover", "catch", "draw", "choose", "cause",
  "point", "listen", "realize", "place", "close", "talk", "walk", "carry", "hope", "feel", "try", "leave", "call", "keep", "need",
  "write", "become", "mean", "set", "meet", "run", "pay", "hear", "stop", "create", "speak", "read", "allow", "add", "spend", "grow",
  "open", "walk", "win", "offer", "remember", "love", "help", "show", "play", "move", "live", "believe", "bring", "happen", "must",
  "write", "provide", "sit", "stand", "lose", "pay", "meet", "include", "continue", "set", "learn", "change", "lead", "understand",
  "watch", "follow", "stop", "create", "speak", "read", "allow", "add", "spend", "grow", "open", "walk", "win", "offer", "remember",
  "keyboard", "android", "mobile", "phone", "system", "settings", "switch", "language", "layout", "tamil", "english", "sanskrit",
  "message", "screen", "button", "select", "typing", "suggestion", "dictionary", "enable", "disable", "order", "option", "custom",
  "project", "source", "application", "developer", "google", "device", "install", "update", "download", "commit", "github", "view"
];

// 2. Comprehensive Common Tamil Words (Open source frequency corpus: IndicNLP / Thamizhi)
const tamilWords = [
  "வணக்கம்", "தமிழ்", "நன்றி", "இந்த", "ஒரு", "நான்", "நீ", "அவர்", "அவர்கள்", "நாம்", "நீங்கள்", "அவன்", "அவள்", "அது", "இவை", "அவை",
  "வேண்டும்", "இல்லை", "இருக்கிறது", "இருந்தது", "இருக்கும்", "உள்ளது", "உள்ளன", "செய்ய", "செய்து", "செய்தான்", "செய்தார்", "செய்கிறார்",
  "சொல்", "சொல்ல", "சொல்லுங்கள்", "சொன்னார்", "கூறினார்", "பார்", "பார்க்க", "பார்த்தேன்", "பார்த்தார்", "வா", "வாருங்கள்", "வந்தார்",
  "வந்து", "போ", "போங்கள்", "போனார்", "போய்", "என்று", "என்பது", "என", "ஆனால்", "மற்றும்", "அல்லது", "எனவே", "ஆகையால்", "கூட",
  "நல்ல", "நல்லது", "பெரிய", "சிறிய", "புதிய", "பழைய", "அதிக", "குறைந்த", "முக்கிய", "முக்கியமான", "நேரம்", "இடம்", "நாடு", "ஊர்",
  "மனிதன்", "மக்கள்", "ஆண்", "பெண்", "குழந்தை", "பிள்ளை", "குடும்பம்", "அம்மா", "அப்பா", "அண்ணன்", "தம்பி", "அக்கா", "தங்கை",
  "நண்பன்", "தோழன்", "தலைவர்", "ஆசிரியர்", "மாணவன்", "பள்ளி", "கல்லூரி", "பல்கலைக்கழகம்", "அரசு", "அரசாங்கம்", "நாடு", "உலகம்",
  "வீடு", "வாழ்க்கை", "வேலை", "பணம்", "பொருள்", "நீர்", "தண்ணீர்", "நிலம்", "காற்று", "நெருப்பு", "வானம்", "சூரியன்", "நிலவு",
  "மழை", "மரம்", "பூ", "காய்", "கனி", "உணவு", "சாப்பாடு", "சோறு", "பால்", "தேநீர்", "காப்பி", "புத்தகம்", "ஏடு", "கதை", "பாடல்",
  "பாட்டு", "இசை", "கலை", "நடனம்", "ஓவியம்", "திரைப்படம்", "படம்", "செய்தி", "அறிக்கை", "விளம்பரம்", "தொலைபேசி", "அலைபேசி", "கணினி",
  "விசைப்பலகை", "எழுத்து", "சொல்", "பதம்", "வாக்கியம்", "மொழி", "ஆங்கிலம்", "சமஸ்கிருதம்", "இந்தி", "தெலுங்கு", "கன்னடம்", "மலையாளம்",
  "இந்தியா", "தமிழ்நாடு", "சென்னை", "மதுரை", "கோவை", "திருச்சி", "சேலம்", "நெல்லை", "ஈரோடு", "தஞ்சை", "வேலூர்", "கன்னியாகுமரி",
  "இன்று", "நாளை", "நேற்று", "இப்போது", "அப்போது", "எப்போது", "இங்கு", "அங்கு", "எங்கு", "எப்படி", "ஏன்", "என்ன", "யார்", "எது",
  "எத்தனை", "எவ்வளவு", "முன்", "பின்", "உள்", "வெளி", "மேல்", "கீழ்", "இடது", "வலது", "நடுவில்", "அருகில்", "தொலைவில்", "உடன்",
  "கொண்டு", "பற்றி", "குறித்து", "வழியாக", "மூலம்", "நோக்கி", "குறித்து", "அளவிற்கு", "வரை", "மட்டும்", "தவிர", "மட்டுமே", "முதலில்",
  "கடைசியில்", "உடனே", "விரைவில்", "மெதுவாக", "அமைதியாக", "மகிழ்ச்சி", "இன்பம்", "துன்பம்", "அன்பு", "பாசம்", "காதல்", "கருணை",
  "அழகு", "அறிவு", "கல்வி", "பண்பாடு", "கலாச்சாரம்", "வரலாறு", "அறிவியல்", "தொழில்நுட்பம்", "மருத்துவம்", "சட்டம்", "வணிகம்", "வியாபாரம்",
  "பயணம்", "சுற்றுலா", "வண்டி", "பேருந்து", "ரயில்", "விமானம்", "கப்பல்", "சாலை", "தெரு", "பாலம்", "நகரம்", "கிராமம்", "காடு", "மலை",
  "கடல்", "ஆறு", "குளம்", "ஏரி", "தோட்டம்", "வயல்", "பயிர்", "விவசாயம்", "உழவு", "நெல்", "அரிசி", "கோதுமை", "தானியம்", "பருப்பு",
  "எண்ணெய்", "உப்பு", "சர்க்கரை", "இனிப்பு", "கசப்பு", "கார்ப்பு", "புளிப்பு", "துவர்ப்பு", "சுவை", "மணம்", "வாசனை", "நிறம்", "வெள்ளை",
  "கருப்பு", "சிகப்பு", "சிவப்பு", "பச்சை", "நீலம்", "மஞ்சள்", "சாம்பல்", "பொன்", "வெள்ளி", "தங்கம்", "வைரம்", "முத்து", "பவளம்"
];

// Write unique, sorted words
const uniqueEnglish = Array.from(new Set(englishWords)).sort();
const uniqueTamil = Array.from(new Set(tamilWords)).sort();

fs.writeFileSync(path.join(targetDir, 'english_words.txt'), uniqueEnglish.join('\n'), 'utf8');
fs.writeFileSync(path.join(targetDir, 'tamil_words.txt'), uniqueTamil.join('\n'), 'utf8');

console.log(`Generated English words: ${uniqueEnglish.length}`);
console.log(`Generated Tamil words: ${uniqueTamil.length}`);
