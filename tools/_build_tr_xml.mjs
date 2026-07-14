/**
 * Build values-tr/strings.xml from EN XML structure + TR JSON bodies.
 * Validate placeholder parity.
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "..");
const EN_XML = path.join(ROOT, "app/src/main/res/values/strings.xml");
const TR_JSON = path.join(ROOT, "tools/_tr_strings.json");
const OUT_XML = path.join(ROOT, "app/src/main/res/values-tr/strings.xml");
const OUT_ARRAYS = path.join(ROOT, "app/src/main/res/values-tr/arrays.xml");

const tr = JSON.parse(fs.readFileSync(TR_JSON, "utf8"));
const enXml = fs.readFileSync(EN_XML, "utf8");

function extractPlaceholders(s) {
  const tokens = [];
  let i = 0;
  while (i < s.length) {
    if (s[i] !== "%") {
      i++;
      continue;
    }
    if (s[i + 1] === "%") {
      tokens.push("%%");
      i += 2;
      continue;
    }
    const m = /^%(?:(\d+)\$)?([sdif])/.exec(s.slice(i));
    if (m) {
      tokens.push({
        raw: m[0],
        pos: m[1] || null,
        typ: m[2],
      });
      i += m[0].length;
      continue;
    }
    i++;
  }
  return tokens;
}

function phMultisetKey(tokens) {
  // Compare conversion specs: type sequence; positions if both present
  return tokens.map((t) => {
    if (t === "%%") return "%%";
    return t.pos ? `${t.pos}$${t.typ}` : t.typ;
  });
}

function phEqual(a, b) {
  if (a.length !== b.length) return false;
  // Normalize: compare ordered list of types; allow %d vs %1$d only if
  // the other side has matching type at same index without conflicting positionals
  for (let i = 0; i < a.length; i++) {
    const x = a[i];
    const y = b[i];
    if (x === "%%" || y === "%%") {
      if (x !== y) return false;
      continue;
    }
    if (x.typ !== y.typ) return false;
    if (x.pos && y.pos && x.pos !== y.pos) return false;
  }
  // Also require same multiset of raw-normalized forms when EN uses positionals
  const enHasPos = a.some((t) => t !== "%%" && t.pos);
  if (enHasPos) {
    const ka = phMultisetKey(a).slice().sort().join(",");
    const kb = phMultisetKey(b).slice().sort().join(",");
    // Prefer exact token match when EN uses positionals
    const exactA = a.map((t) => (t === "%%" ? "%%" : t.raw)).join(" ");
    const exactB = b.map((t) => (t === "%%" ? "%%" : t.raw)).join(" ");
    if (exactA !== exactB && ka !== kb) return false;
  }
  return true;
}

function parseStrings(xml) {
  const items = [];
  const re = /<string\s+name="([^"]+)"([^>]*)>([\s\S]*?)<\/string>/g;
  let m;
  while ((m = re.exec(xml))) {
    items.push({ name: m[1], attrs: m[2], body: m[3] });
  }
  return items;
}

// Rebuild XML preserving comments / whitespace between strings
const inner = enXml
  .replace(/^[\s\S]*?<resources>/, "")
  .replace(/<\/resources>[\s\S]*$/, "");

const iter =
  /(<!--[\s\S]*?-->)|<string\s+name="([^"]+)"([^>]*)>([\s\S]*?)<\/string>/g;
let match;
let pos = 0;
let out = '<?xml version="1.0" encoding="utf-8"?>\n<resources>';
const missing = [];
const names = [];

while ((match = iter.exec(inner))) {
  out += inner.slice(pos, match.index);
  if (match[1]) {
    out += match[1];
  } else {
    const name = match[2];
    const attrs = match[3];
    names.push(name);
    let body = tr[name];
    if (body === undefined) {
      missing.push(name);
      body = match[4];
    }
    out += `<string name="${name}"${attrs}>${body}</string>`;
  }
  pos = match.index + match[0].length;
}
out += inner.slice(pos);
out += "</resources>\n";

// Normalize to LF; ensure required header form
out = out.replace(/\r\n/g, "\n").replace(/\r/g, "\n");
if (!out.startsWith('<?xml version="1.0" encoding="utf-8"?>\n<resources>')) {
  throw new Error("Output does not start with required XML header");
}

fs.mkdirSync(path.dirname(OUT_XML), { recursive: true });
fs.writeFileSync(OUT_XML, out, "utf8");

// arrays.xml
const arrays = `<?xml version="1.0" encoding="utf-8"?>
<resources>

    <array name="app_theme_mode">
        <item>Açık</item>
        <item>Koyu</item>
        <item>Sistem varsayılanı</item>
    </array>

    <array name="app_theme_mode_values">
        <item>light_theme</item>
        <item>dark_theme</item>
        <item>system_default</item>
    </array>


</resources>
`;
fs.writeFileSync(OUT_ARRAYS, arrays, "utf8");

// Validation
const trXml = fs.readFileSync(OUT_XML, "utf8");
const enItems = parseStrings(enXml);
const trItems = parseStrings(trXml);

const enNames = enItems.map((x) => x.name);
const trNames = trItems.map((x) => x.name);
const enSet = new Set(enNames);
const trSet = new Set(trNames);

const onlyEn = [...enSet].filter((n) => !trSet.has(n));
const onlyTr = [...trSet].filter((n) => !enSet.has(n));
const trDupes = trNames.filter((n, i) => trNames.indexOf(n) !== i);

const enMap = Object.fromEntries(enItems.map((x) => [x.name, x.body]));
const trMap = Object.fromEntries(trItems.map((x) => [x.name, x.body]));

const phMismatches = [];
for (const name of enNames) {
  if (!trMap[name]) continue;
  const ep = extractPlaceholders(enMap[name]);
  const tp = extractPlaceholders(trMap[name]);
  if (!phEqual(ep, tp)) {
    phMismatches.push({
      name,
      en: ep.map((t) => (t === "%%" ? "%%" : t.raw)),
      tr: tp.map((t) => (t === "%%" ? "%%" : t.raw)),
      enBody: enMap[name],
      trBody: trMap[name],
    });
  }
}

// Check file header
const headerOk = trXml.startsWith(
  '<?xml version="1.0" encoding="utf-8"?>\n<resources>'
);

console.log(
  JSON.stringify(
    {
      enCount: enItems.length,
      trCount: trItems.length,
      missingFromJson: missing,
      onlyEn,
      onlyTr,
      trDupes,
      phMismatchCount: phMismatches.length,
      phMismatches,
      headerOk,
      outXml: OUT_XML,
      outArrays: OUT_ARRAYS,
      firstLine: trXml.split("\n").slice(0, 2),
    },
    null,
    2
  )
);
