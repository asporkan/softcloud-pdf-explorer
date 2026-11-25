"""Hostinger eklentisi için PreToolUse koruması.

Eklentinin araçları genel: `execute` ve `multi-execute`, işlem adı `operation` parametresinde gider.
Bu yüzden izin kuralları ad bazlı yazılamaz, kararı bu betik verir:
- deny: ödeme, satın alma, silme ve projede gerekmeyen gruplar.
- ask: geri kalan her yazma işlemi (yayın, CDN, DNS…), kullanıcı her seferinde onaylar.
- karar yok: okuma işlemleri (list, get…), normal izin akışına bırakılır.

Gerekli grupları açmak için `HOSTINGER_KEEP` ortam değişkeni kullanılır, ör. `wordpress_,mail_`.
"""

import json
import os
import re
import sys

DENY_GROUPS = ["billing_", "ecommerce_", "reach_", "vps_", "horizons_", "agency-hosting_", "wordpress_"]

# İşlem adında geçerse her zaman engellenir.
DENY_WORDS = [
    "delete", "purchase", "claim-free", "transfer", "authorization-code", "disable-lock",
    "disable-privacy", "_move_", "dns_records_reset", "snapshots_restore", "uninstall",
    "change-password", "websites_create", "start-setup", "import-wordpress",
]

READ_VERB = re.compile(r"_(list|get|show|search|check|view|retrieve|download)[\w-]*$")


def operations(tool_input):
    if "steps" in tool_input:
        return [str(s.get("operation", "")) for s in tool_input.get("steps") or []]
    return [str(tool_input.get("operation", ""))]


def decide(op, keep):
    groups = [g for g in DENY_GROUPS if g not in keep]
    if not op or op.startswith(tuple(groups)) or any(w in op for w in DENY_WORDS):
        return "deny"
    if READ_VERB.search(op):
        return None
    return "ask"


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    data = json.loads(sys.stdin.buffer.read().decode("utf-8"))
    keep = [k for k in os.environ.get("HOSTINGER_KEEP", "").split(",") if k]
    ops = operations(data.get("tool_input") or {})
    verdicts = {op: decide(op, keep) for op in ops}
    denied = [op for op, v in verdicts.items() if v == "deny"]
    asked = [op for op, v in verdicts.items() if v == "ask"]
    if denied:
        decision, reason = "deny", "Hostinger koruması engelledi: " + ", ".join(denied) + ". Bu işlemi kullanıcı hPanel'den kendisi yapar."
    elif asked:
        decision, reason = "ask", "Hostinger yazma işlemi: " + ", ".join(asked)
    else:
        return
    print(json.dumps({"hookSpecificOutput": {
        "hookEventName": "PreToolUse", "permissionDecision": decision, "permissionDecisionReason": reason}}, ensure_ascii=False))


if __name__ == "__main__":
    main()
