"""Hostinger Claude Code eklentisi için koruma hook'unu kurar.

Eklentinin araçları genel: `search` (salt okunur), `execute` ve `multi-execute`. İşlem adı
`operation` parametresinde gittiği için izin kuralları ad bazlı yazılamaz. Kararı
`.claude/hooks/hostinger-guard.py` verir:
- deny: ödeme, satın alma, silme ve projede gerekmeyen gruplar (VPS, WordPress, agency, reach…).
- ask: geri kalan her yazma işlemi (yayın, önbellek, DNS…).
- serbest: okuma işlemleri (list, get…).

Betik `.claude/settings.local.json` dosyasına PreToolUse hook'unu ekler ve eski ad bazlı
Hostinger kurallarını siler. Diğer ayarlar korunur. Gerekli grupları açmak için ortamda
`HOSTINGER_KEEP` tanımlanır, ör. WordPress sitesi için `HOSTINGER_KEEP=wordpress_`.

Kullanım (proje kökünde):
    python .claude/sablonlar/hostinger-izinler.py
"""

import argparse
import json
import pathlib

PREFIX = "mcp__plugin_hostinger_hostinger__"
MATCHER = PREFIX + "(execute|multi-execute)"
COMMAND = 'python "$CLAUDE_PROJECT_DIR/.claude/hooks/hostinger-guard.py"'


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--settings", default=".claude/settings.local.json")
    args = ap.parse_args()

    path = pathlib.Path(args.settings)
    settings = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}

    perms = settings.get("permissions", {})
    for key in ("deny", "ask"):
        if key in perms:
            perms[key] = [r for r in perms[key] if not r.startswith(PREFIX)]
            if not perms[key]:
                del perms[key]
    if "permissions" in settings and not perms:
        del settings["permissions"]

    pre = settings.setdefault("hooks", {}).setdefault("PreToolUse", [])
    pre[:] = [h for h in pre if h.get("matcher") != MATCHER]
    pre.append({"matcher": MATCHER, "hooks": [{"type": "command", "command": COMMAND}]})

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(settings, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Hostinger koruma hook'u kuruldu -> {path}")


if __name__ == "__main__":
    main()
