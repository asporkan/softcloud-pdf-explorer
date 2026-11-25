# PDF Explorer

Google Play'deki `com.softcloud.pdfexplorer` uygulaması: Android için çevrimdışı PDF okuyucu ve düzenleyici (görüntüleme, 12 PDF aracı, son açılanlar, favoriler, yer imleri). PdfReaderPro'nun GPL fork'u, mevcut Play uygulamasının yerine aynı imzayla güncelleme olarak yayınlanır. Başarı: kullanıcı şikâyeti olmayan, çökmesiz ve Play politikalarına uyumlu sürümler.
**Tek gerçek kaynak:** [AGENTS.md](../AGENTS.md) (mimari, kodlama kuralları, lisans). Detaylar oradan okunur, buraya kopyalanmaz.

## Hedef
- Çökme ve ANR olmadan, Android 7–16 arasında (minSdk 24, targetSdk 36) dosya işlemlerinin doğru çalışması.
- Her sürüm bağlı cihazda duman testinden geçer; Play'e yalnızca imzalı AAB gider.
- Arayüz TR + EN (ve mevcut diğer diller); anahtar setleri eşit kalır.

## Yığın
Kotlin · Jetpack Compose · Material 3 · Koin · Room · DataStore · PDF.js (WebView) · iText · Gradle KTS (sürümler `gradle/libs.versions.toml`) · Dağıtım: Google Play (AAB), GitHub kaynak deposu.

## Kırmızı çizgiler — asla
- Sır içeren dosyalar (`keystore.properties`, `*.jks`, `local.properties`, şifre) repoya girmez ve MD dosyalarına yazılmaz.
- `applicationId`, namespace ve imza yapılandırması ayrı bir görev olmadan değiştirilmez.
- Lisans dışı bağımlılık eklenmez (bkz. AGENTS.md "License & Compliance"); yeni paket önce kullanıcıya sorulur.
- Kullanıcının cihazındaki gerçek dosyalar testte silinmez; test dosyaları `qa_` önekiyle açılır.

## Kararlar (brief'i geçersiz kılar)
- 2026-11-26: Arayüz TR+EN; tek kaynak AGENTS.md; ekip android-dev/qa-tester/devops/content, Dengeli profil, her faz sonunda durulur.
- 2026-11-26: `.claude/` repoya dahil (yalnızca `settings.local.json` hariç).
- 2026-11-26: Git: `origin` = GitHub (elle push, yalnızca istenince), `usb` = `D:\Git\pdf-explorer.git` (post-commit otomatik push).
- 2026-11-26: Sıradaki sürüm 0.6.1 (versionCode 7); B1–B27 bulgularının tüm paketleri (P1–P4) bu sürüme dahil.
- 2026-11-26: Son Açılanlar kalıcıdır (yeniden başlatmada korunur). Araç çıktıları: ilk kayıtta klasör sorulur, sonra sormadan oraya kaydedilir; araç ekranında "Kayıt yeri · Değiştir" her zaman görünür, Ayarlar'da değiştirme ve "her seferinde sor" seçeneği vardır; geçersiz klasörde yeniden sorulur.
- 2026-11-26: INTERNET izni şimdilik kaldırılır (uygulama tamamen çevrimdışı). İleride reklam ve ücretli özellik gelecek; o zaman yeniden eklenir, gizlilik metni ve Play veri güvenliği formu güncellenir.
- 2026-11-26: İmzalama: kökteki git dışı `keystore.properties` → `C:/Workspace/Android/pdf/keystore/pdf_explorer.jks`, alias `key0` (şifreler yalnızca o dosyada).
- 2026-11-26: Koyu temada koyu zemin üstündeki yazı/simge açık renk olur; renkler `presentation/theme/` token'larından gelir, bileşende sabit `Color(0xFF…)` kullanılmaz.

## Yerel araçlar
- Android SDK: `C:\Users\DEV-PC\AppData\Local\Android\Sdk` (Gradle); adb: `C:/Android/Sdk/platform-tools/adb.exe`.
- Build: `./gradlew :app:assembleDebug` · Test: `./gradlew :app:testDebugUnitTest` · Yayın: `./gradlew :app:bundleRelease`.
- Çeviri doğrulama: `python tools/_validate_tr_strings.py`.
- Test cihazı: Redmi K20 Pro, Android 16 (USB, `ac7dc2c9`).
- Skill'ler: `android-permissions-security`, `play-policy-insights`, `r8-analyzer` (android/skills, `skills-lock.json`).

## Ajan ekibi (`.claude/agents/`)
Ana denetleyici Claude'dur. Kullanıcı "sen <ajan>sın, başla" derse o ajan görevsiz başlatılır ve PROGRESS.md'den kendine ait (`@ajan`) ilk açık işi alır. "<ajan>, şunu yap" denirse ajan o görevle başlatılır.
android-dev (sonnet/high) · qa-tester (sonnet/low) · devops (haiku/medium) · content (sonnet/medium)
**Tek komut: `/ekip`.** Ekip PROGRESS'teki bağımlılık sırasıyla paralel çalışır ve her faz sonunda durur.
Rapor formatı: **Yapılan / Doğrulama / Eksik-riskler / Önerilen sonraki adım / DONE satırı önerisi / Kullanıcıya soru**.

## Çalışma kuralları
- **Onaysız kurulum yok:** skill, plugin, MCP, yazılım ve yığında olmayan paketler önce kullanıcıya sunulur (`find-skills` tablosu: kaynak, kurulum, yıldız, güvenlik notu). Toplu paket kurulmaz.
- **Hafıza hijyeni:** [PROGRESS.md](../PROGRESS.md) yalnızca aktif/sıradaki işler; biten ve doğrulanan iş silinir, [DONE.md](../DONE.md)'ye tek satır yazılır. Kullanıcıdan beklenenler [TODO-Faruk.md](../TODO-Faruk.md). Geçici dosyalar (ekran görüntüsü, logcat dökümü, test PDF'i) iş bitince silinir.
- **Git:** Her biten ve doğrulanan iş tek commit; mesaj Türkçe `<alan>: <ne yapıldı>`. Commit öncesi `git status` ile sır kontrolü. GitHub'a push yalnızca kullanıcı isterse.
- **Paralel ajanlar:** Aynı dosyaya dokunan iki iş aynı anda başlatılmaz. Ajanlar `git stash`, `checkout`, `reset` kullanmaz; commit'te yalnızca o işin dosyaları stage edilir.
- **Kullanıcıya sorma:** Kullanıcının kararlarında varsayım yapılmaz; engelleyen soru varsa o iş bekletilir.
- **Rapor:** Her faz sonunda kısa durum raporu; ham ajan çıktısı aktarılmaz.
- **Yayın:** Play Console'a yükleme kullanıcı onayıyla yapılır; her yayından önce hangi build'in (sürüm, dosya yolu) yükleneceği söylenir.
