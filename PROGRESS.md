# PROGRESS — aktif ve sıradaki işler
> Format: `- [ ] @ajan · F<faz>-<no>. iş · (bekler: F1-2, …)`. Biten iş buradan silinir ve DONE.md'ye tek satır yazılır.

## Kalıcı teknik kurallar
- adb: `C:/Android/Sdk/platform-tools/adb.exe` (Git Bash'te `MSYS_NO_PATHCONV=1`). Test cihazı Redmi K20 Pro / Android 16, 3 tuşlu gezinme: alt ~130 px gezinme çubuğu.
- Bulgu numaraları (B1–B27) 2026-11-26 QA raporundan; ayrıntı DONE/commit geçmişinde.
- Paralel çalışmada yeni metinler ajana ait ayrı kaynak dosyasına eklenir (`values/strings_<alan>.xml` + `values-tr/strings_<alan>.xml`); `strings.xml`'e yalnızca @content dokunur.

## Faz 3 — Yayın sonrası (0.6.1)
- [ ] @ana-denetleyici · F3-1. Play incelemesi sonucu takip (Yayın özeti; red/uyarı gelirse çözüm)
- [ ] @ana-denetleyici · F3-2. Yayından 2–3 gün sonra Android vitals (çökme/ANR artık raporlanıyor) ve lansman öncesi rapor (erişilebilirlik) kontrolü
- [ ] @android-dev · F3-3. Sonraki sürüm adayları: Checkbox satırları toggleable, PageScrubber tutamaç boyutu, lint uyarıları (PluralsCandidate, StaticFieldLeak, TrustAllX509TrustManager incelemesi), reklam/ücretli özellik planı
