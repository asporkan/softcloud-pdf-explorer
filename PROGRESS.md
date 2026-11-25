# PROGRESS — aktif ve sıradaki işler
> Format: `- [ ] @ajan · F<faz>-<no>. iş · (bekler: F1-2, …)`. Biten iş buradan silinir ve DONE.md'ye tek satır yazılır.

## Kalıcı teknik kurallar
- adb: `C:/Android/Sdk/platform-tools/adb.exe` (Git Bash'te `MSYS_NO_PATHCONV=1`). Test cihazı Redmi K20 Pro / Android 16, 3 tuşlu gezinme: alt ~130 px gezinme çubuğu.
- Bulgu numaraları (B1–B27) 2026-11-26 QA raporundan; ayrıntı DONE/commit geçmişinde.
- Paralel çalışmada yeni metinler ajana ait ayrı kaynak dosyasına eklenir (`values/strings_<alan>.xml` + `values-tr/strings_<alan>.xml`); `strings.xml`'e yalnızca @content dokunur.

## Faz 2 — Onaylanan düzeltmeler (v0.6.1)
### Dalga 2
- [ ] @android-dev · F2-17. Lint hataları (LocalContextGetResourceValueCall 37, NewApi MediaStore.Downloads, PropertyEscape, SuspiciousIndentation) + lansman öncesi erişilebilirlik (dokunma hedefi ≥48dp, içerik etiketleri, "Selected/Not selected" TR) + lint onaylı 60 kullanılmayan string
- [ ] @android-dev · F2-18. B31 çökme raporlama: GlobalErrorHandler çökmeyi önceki handler'a iletir (Play vitals görür), sonraki açılışta kısa bilgi; kullanılmayan 7 renk + 2 dizi kaynağı; lottie-compose kaldırılır (lisans satırı F2-17 sonrası)
### Kapanış
- [ ] @qa-tester · F2-7. Tüm bulguların cihazda yeniden doğrulanması + regresyon (bekler: F2-17, F2-18) — release APK ile; iText/BC daraltması için şifreli PDF, kilitle/kilit aç, birleştir, görsel→PDF, sayfa no/filigran, okuyucu JS köprüsü mutlaka denenir
- [ ] @devops · F2-8. Sürüm 0.6.1 (code 7), CHANGELOG, imzalı AAB + APK, cihaza kurulum (bekler: F2-7, TODO-Faruk #1 keystore)
