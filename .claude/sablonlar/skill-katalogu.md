# Skill kataloğu (şablon)

`/kurulum` 4. adımında kullanılır. Bu liste bir **öneri havuzudur**, hiçbiri kendiliğinden kurulmaz. Kullanıcı tablodan seçer, onay verir, sonra tek tek kurulur.

- Sayılar 2026-11-01 tarihli (skills.sh). Kurulum anında `find-skills` ve skills.sh ile yeniden kontrol et; 1K altı kurulum ya da <100 yıldız ise kullanıcıyı uyar.
- Kurulum: `npx skills add <owner/repo@skill>`. `-y` yalnızca kullanıcı o skill'i onayladıktan sonra. Kilit dosyası `skills-lock.json` commit edilir; `.claude/skills/*` bağlantıları git'e girmez, yeni klonda aynı komutla yeniden kurulur.
- Kurulumdan sonra skill'in `SKILL.md` dosyasını ve klasörünü oku. Çalıştırılabilir betik, ağ çağrısı (curl/wget/fetch), `eval`, proje dışına yazma ya da API anahtarı isteği varsa kullanıcıya bildir.
- Onaylanan skill ilgili ajanın frontmatter `skills:` listesine eklenir.

## ECC (`affaan-m/ecc`, MIT)

Büyük bir Claude Code paketi: ~300 skill, 71 ajan, 98 komut, 28 kod çalıştıran kanca, MCP şablonları. **Yalnızca tek tek skill kurulur.** Plugin (`ecc@ecc`), `npx ecc-universal setup`, kancalar, komutlar ve MCP şablonları kurulmaz. Gerekçe: kancalar her araç çağrısında Node betiği çalıştırır, `~/.claude/homunculus` ve `~/.claude/metrics` altına yazar, `.claude/settings.json`'ı değiştirir, `auto-update` komutu kendini günceller; ajanların bir kısmı "PROACTIVELY" ile kendiliğinden devreye girer; `security-review`, `code-review` ve `plan` adları yerleşik araçlarla çakışır.

| Ajan | Skill | Kurulum (2026-11-01) | Ne zaman öner |
|---|---|---|---|
| backend | `affaan-m/ecc@backend-patterns` | ~6,5K | her backend projesi |
| backend | `affaan-m/ecc@api-design` | ~6,0K | API tasarlanacaksa |
| backend | `affaan-m/ecc@postgres-patterns` | ~5,7K | PostgreSQL varsa |
| backend | `affaan-m/ecc@nestjs-patterns` | ~5,5K | NestJS varsa (skills.sh denetimi: Gen Trust Hub, Socket, Snyk "Pass") |
| backend, devops | `affaan-m/ecc@docker-patterns` | ~7,0K | Docker/Compose varsa |
| frontend-coder | `affaan-m/ecc@frontend-patterns` | ~6,5K | her web arayüzü |
| frontend-coder | `affaan-m/ecc@react-patterns` | ~5,4K | React/Next.js varsa |
| qa-tester | `affaan-m/ecc@e2e-testing` | ~5,8K | e2e testi yazılacaksa |
| qa-tester | `affaan-m/ecc@tdd-workflow` | ~5,7K | test önce çalışılacaksa |
| qa-tester | `affaan-m/ecc@verification-loop` | ~5,7K | doğrulama döngüsü isteniyorsa |
| devops | `affaan-m/ecc@deployment-patterns` | ~5,3K | yayın/CI kurulacaksa |
| devops, content | `affaan-m/ecc@seo` | ~5,5K | tanıtım sitesi varsa |

**Uyarılar:** `affaan-m/ecc@typescript-patterns` yalnızca ~178 kurulum (1K altı, uyar). `security-review` ECC skill'i yerleşik `security-review` ile aynı adı taşır, kurma.

## Diğer kaynaklar

Resmi kaynakları öne al: `anthropics/skills` (ör. frontend-design), `vercel-labs/agent-skills` (React/Next.js), `microsoft`. Proje türüne göre `find-skills` ile ara, aynı tabloyu doldur: ad, kaynak (owner/repo), kurulum sayısı, GitHub yıldızı, güvenlik değerlendirmesi, ne işe yaradığı.
