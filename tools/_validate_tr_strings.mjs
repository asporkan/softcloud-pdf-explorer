/**
 * EN vs TR validation (Node equivalent of _validate_tr_strings.py)
 * - same set of string names
 * - no duplicate keys in TR
 * - same placeholder token multiset per key
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const EN = path.join(ROOT, "app/src/main/res/values/strings.xml");
const TR = path.join(ROOT, "app/src/main/res/values-tr/strings.xml");

const STRING_RE = /<string\s+name="([^"]+)"([^>]*)>([\s\S]*?)<\/string>/g;

function parse(xmlPath) {
  const text = fs.readFileSync(xmlPath, "utf8");
  const names = [];
  const map = {};
  let m;
  const re = new RegExp(STRING_RE.source, "g");
  while ((m = re.exec(text))) {
    names.push(m[1]);
    map[m[1]] = m[3];
  }
  const seen = new Set();
  const dupes = [];
  for (const n of names) {
    if (seen.has(n)) dupes.push(n);
    seen.add(n);
  }
  return { text, names, map, dupes };
}

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
      tokens.push(m[0]);
      i += m[0].length;
      continue;
    }
    i++;
  }
  return tokens;
}

function normalizeMultiset(tokens) {
  const c = new Map();
  for (const t of tokens) {
    if (t === "%%") {
      c.set("%%", (c.get("%%") || 0) + 1);
      continue;
    }
    const m = /^%(?:(\d+)\$)?([sdif])$/.exec(t);
    if (!m) {
      c.set(t, (c.get(t) || 0) + 1);
      continue;
    }
    const pos = m[1];
    const typ = m[2];
    // Normalize: when comparing EN %d with TR %d → "%d"
    // When either uses positional, keep "%N$type" so reordering is detectably intentional
    const key = pos ? `%${pos}$${typ}` : `%${typ}`;
    c.set(key, (c.get(key) || 0) + 1);
  }
  return [...c.entries()].sort((a, b) => a[0].localeCompare(b[0]));
}

function typeSequence(tokens) {
  return tokens.map((t) => {
    if (t === "%%") return "%%";
    return /^%(?:\d+\$)?([sdif])$/.exec(t)[1];
  });
}

const en = parse(EN);
const tr = parse(TR);

const enSet = new Set(en.names);
const trSet = new Set(tr.names);
const missing = [...enSet].filter((n) => !trSet.has(n)).sort();
const extra = [...trSet].filter((n) => !enSet.has(n)).sort();

const phMismatches = [];
for (const name of [...enSet].sort()) {
  if (!tr.map[name]) continue;
  const ep = extractPlaceholders(en.map[name]);
  const tp = extractPlaceholders(tr.map[name]);
  const enTypes = typeSequence(ep);
  const trTypes = typeSequence(tp);
  const enMs = JSON.stringify(normalizeMultiset(ep));
  const trMs = JSON.stringify(normalizeMultiset(tp));

  // Require same ordered type sequence (placeholders stay aligned),
  // and same normalized multiset.
  // If EN uses positionals and TR reorders, multiset of pos$key must still match.
  const enHasPos = ep.some((t) => /%\d+\$/.test(t));
  const trHasPos = tp.some((t) => /%\d+\$/.test(t));
  let ok = enTypes.join() === trTypes.join();
  if (enHasPos || trHasPos) {
    ok = enMs === trMs;
  } else {
    ok = ok && enMs === trMs;
  }
  if (!ok) {
    phMismatches.push({ name, en: ep, tr: tp, enBody: en.map[name], trBody: tr.map[name] });
  }
}

const headerOk = tr.text.startsWith(
  '<?xml version="1.0" encoding="utf-8"?>\n<resources>'
);

const report = {
  enCount: en.names.length,
  trCount: tr.names.length,
  trUnique: trSet.size,
  trDupes: tr.dupes,
  missing,
  extra,
  phMismatchCount: phMismatches.length,
  phMismatches,
  headerOk,
  result:
    en.names.length === tr.names.length &&
    tr.dupes.length === 0 &&
    missing.length === 0 &&
    extra.length === 0 &&
    phMismatches.length === 0 &&
    headerOk
      ? "PASS"
      : "FAIL",
};

console.log(JSON.stringify(report, null, 2));
process.exit(report.result === "PASS" ? 0 : 1);
