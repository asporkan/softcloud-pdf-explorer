---
name: kurulum
description: .claude şablonunu bu projeye uyarlar. CLAUDE.md'nin ilk satırında "SABLON-DURUMU: KURULMADI" varsa veya kullanıcı "/kurulum", "şablonu kur", "projeyi hazırla" dediğinde kullan. Proje klasörünü inceler, bilemediği alanları soru sorarak netleştirir ve CLAUDE.md, PROGRESS, DONE, TODO dosyaları ile ajan ekibini oluşturur.
---

# /kurulum — şablonu projeye uyarla

Amaç: `{{...}}` alanlarını doldurmak, ekibi projeye göre kırpmak ve hafıza dosyalarını kurmak. Bunu en az soruyla yapmak.

## 1. Önce keşfet, sonra sor
Soru sormadan önce klasörü tara ve bulduklarını kısa bir listeyle kullanıcıya göster:
- **Dosya yapısı:** `package.json`, `composer.json`, `pyproject.toml`, `requirements.txt`, `build.gradle*`, `*.csproj`, `go.mod`, `Cargo.toml`, `Dockerfile`, `.env.example`
- **Doküman:** README, brief, `*.md` dosyaları, `docs/`
- **Git:** `git status`, `git remote -v`, son commit'ler, `git config user.name`
- **Kod:** mevcutsa çatı, test aracı ve barındırma ipuçları
- **Global kurallar:** `~/.claude/CLAUDE.md` varsa ona uy ve burada tekrarlama

Çıkarabildiğin bilgiyi soru olarak sorma. "Şunu gördüm, doğru mu?" diye onaya sun.

## 2. Soru turları (AskUserQuestion, tur başına en fazla 4 soru)
Seçenekleri keşfettiklerine göre doldur ve önerdiğin seçeneği ilk sıraya "(Önerilen)" etiketiyle koy. Cevabı zaten bilinen soruyu atla. Kullanıcı her zaman "Other" ile serbest yazabilir.

**Tur 1 — Proje kimliği**
- Proje türü: web sitesi / web uygulaması / mobil uygulama / script-otomasyon / kütüphane / diğer
- Proje adı ve tek cümlelik amacı: önce kendi önerini yaz, kullanıcı düzeltir.
- Tek gerçek kaynak: mevcut brief'i kullan / birlikte BRIEF.md yazalım (sablonlar/BRIEF.md) / gerek yok
- Arayüz dili: TR / EN / TR+EN / diğer

**Tur 2 — Teknik çerçeve**
- Yığın: tespit ettiğini öner. Yoksa proje türüne göre 2–3 seçenek sun.
- Barındırma ve dağıtım hedefi. Seçenekler: Hostinger / Vercel-Netlify / VPS / henüz yok. Kök klasörde korunması gereken dosya var mı (`app-ads.txt`, doğrulama dosyası)?
- Kalite hedefleri (çoklu seçim): performans bütçesi, erişilebilirlik, SEO, test kapsamı, güvenlik denetimi
- Kırmızı çizgiler: projeye özel yasaklar varsa (serbest metin)

**Tur 3 — Ekip ve tempo**
- Ajanlar (çoklu seçim, proje türüne uyanları öner): designer, frontend-coder, backend, content, motion-3d, qa-tester, devops
- Model profili:
  - Dengeli (Önerilen): aşağıdaki tablo
  - Ekonomik: her ajan bir kademe aşağı
  - Maksimum kalite: kritik ajanlar opus/high
- Çalışma temposu: her faz sonunda dur ve onay bekle (Önerilen) / kesintisiz devam et
- Kullanıcıdan beklenenler dosyasının adı: `TODO-<ad>.md`. Adı git kullanıcı adından öner.

**Tur 4 — Git**
- `D:\Git\_araclar\git-yeni-proje` varsa: "D:\Git yerel depoya bağla (Önerilen)" / mevcut uzak depo / git kullanma
- Commit düzeni: her biten iş tek commit (Önerilen) / faz sonunda toplu

Belirsiz kalan ve sonra netleşecek bilgiyi uydurma, TODO dosyasına yaz.

## 3. Model profili (Dengeli)
| Ajan | Model | Efor | Ne zaman |
|---|---|---|---|
| designer | opus | high | Görsel yön, bölüm şartnamesi, bileşen seçimi |
| motion-3d | opus | xhigh | Animasyon, WebGL, scroll anlatımı |
| frontend-coder | sonnet | high | Sayfa, bileşen, routing, entegrasyon |
| backend | sonnet | medium | API, veritabanı, form, sunucu |
| content | sonnet | medium | Metin, çeviri, doğruluk denetimi |
| qa-tester | sonnet | low | Test, a11y, performans ölçümü |
| devops | haiku | medium | SEO, build, dağıtım dokümanı |

Ekonomik profil: opus→sonnet, sonnet→haiku, efor bir kademe aşağı. Maksimum profil: designer, frontend ve backend opus/high.

## 4. Dosyaları üret
1. **`.claude/CLAUDE.md`:** Kökte ayrıca bir `CLAUDE.md` varsa iki dosya birlikte yüklenir. Bu durumda içeriği tek dosyada birleştir: kullanıcıya hangisinin kalacağını sor ve öbürünü sil. Şablon kutusunu ve ilk satırdaki işareti sil. `{{...}}` alanlarını doldur. Dosya ≤80 satır olsun, detayı brief'e bırak. Doldurulamayan alanı silme, "TODO: <ne bekleniyor>" yaz ve TODO dosyasına da ekle.
2. **Kök dosyalar:** `.claude/sablonlar/` içinden kopyalanır, mevcutsa üzerine yazılmaz, birleştirilir.
   - `PROGRESS.md`: Faz 0 (kurulum) ve Faz 1 işleri `@ajan` + `bekler:` etiketleriyle.
   - `DONE.md`: ilk satır "kurulum".
   - `TODO-<ad>.md`: açık sorular.
   - Seçildiyse `BRIEF.md`.
3. **Ajanlar:**
   - Seçilmeyen ajan dosyalarını `.claude/agents/` altından sil.
   - Kalanların frontmatter'ına profile göre `model` ve `effort` yaz.
   - Gövdeye projenin yığınına özgü 3–6 kural ekle (build/test komutu, dizin yapısı, yasaklar).
   - Seçilen listede olmayan bir rol gerekiyorsa `.claude/sablonlar/ajan.md`'den yeni ajan üret.
4. **Skill önerisi:** `.claude/sablonlar/skill-katalogu.md`'deki havuzdan yığına ve seçilen ajanlara uyanları, `find-skills` varsa onun bulduklarıyla birlikte tablo halinde sun (ad, kaynak, kurulum sayısı, yıldız, güvenlik notu, ne yapar). **Onay olmadan kurma.** ECC gibi toplu paketlerden yalnızca tek tek skill kurulur; plugin, kurulum betiği, kanca, komut ve MCP şablonu kurulmaz. Kurulumdan sonra `SKILL.md`'yi oku, betik/ağ çağrısı/proje dışına yazma varsa bildir. Onaylananları ilgili ajanın `skills:` listesine ekle, `skills-lock.json`'ı commit et.
5. **Hosting:** Barındırma Hostinger ise kullanıcıya Hostinger Claude Code eklentisini öner. Seçenekler: "Kur (Önerilen)" / "Dosya yöneticisiyle devam".
   - Onay gelirse `claude plugin install hostinger@claude-plugins-official --scope local` çalıştır.
   - Eklentinin `SKILL.md` ve `.mcp.json` dosyalarını oku.
   - `python .claude/sablonlar/hostinger-izinler.py` çalıştır, koruma hook'unu kurar. WordPress gibi gereken grup varsa `HOSTINGER_KEEP=wordpress_` olarak ortamda tanımlanır.
   - Kullanıcıya oturumu yeniden başlatmasını ve `/mcp` ile Hostinger girişini onaylamasını söyle.
   - Yeniden başlatınca `execute` ile `vps_guardtest_list` çağırarak hook'un engellediğini doğrula.
   - Kök klasörde korunacak dosyaları `public/` (ya da build'e giren klasör) altına kopyala.
   - Kuralların özeti CLAUDE.md "Yayın / hosting" bölümünde.
6. **Git:** Seçime göre `git-yeni-proje <klasör>` ya da `git init` çalıştır. `.gitignore`'da sır kalıpları olduğunu doğrula (`.env*`, `*.key`, `config.php` vb.). `git status` ile kontrol et, ardından ilk commit'i at: `kurulum: .claude şablonu projeye uyarlandı`.
7. `.claude/sablonlar/` klasörü kurulumdan sonra silinebilir. Hostinger kullanılıyorsa `.claude/hooks/hostinger-guard.py` kalır; `hostinger-izinler.py` yeniden kurulum için tutulabilir. Yeni ajan üretme ihtimali için tutmak istenip istenmediğini kullanıcıya sor.

## 5. Kapanış
Kullanıcıya kısa bir özet ver:
- Proje özeti (tek satır)
- Seçilen ekip ve model profili
- Oluşturulan dosyalar
- TODO dosyasındaki açık sorular
- Sıradaki komut: `/ekip` ile Faz 1'i başlat ya da "sen <ajan>sın, başla" ile tek ajan.
