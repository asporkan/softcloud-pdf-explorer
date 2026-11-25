# PROGRESS — aktif ve sıradaki işler
> Format: `- [ ] @ajan · F<faz>-<no>. iş · (bekler: F1-2, …)`. Biten iş buradan silinir ve DONE.md'ye tek satır yazılır.

## Kalıcı teknik kurallar
- adb: `C:/Android/Sdk/platform-tools/adb.exe` (Git Bash'te `MSYS_NO_PATHCONV=1`). Test cihazı Redmi K20 Pro / Android 16, 3 tuşlu gezinme: alt ~130 px gezinme çubuğu.
- Bulgu numaraları (B1–B27) 2026-11-26 QA raporundan; ayrıntı DONE/commit geçmişinde.
- Paralel çalışmada yeni metinler ajana ait ayrı kaynak dosyasına eklenir (`values/strings_<alan>.xml` + `values-tr/strings_<alan>.xml`); `strings.xml`'e yalnızca @content dokunur.

## Faz 2 — Onaylanan düzeltmeler (v0.6.1)
### Dalga 2
- [ ] @content · F2-14. strings.xml'deki ~390 referanssız anahtarın kontrollü silinmesi (her anahtar grep + derleme + test ile doğrulanır)
- [ ] @devops · F2-15. Optimizasyon/Google uyumluluğu: R8 kural analizi (r8-analyzer), lint, kaynak küçültme, boyut, eşleme dosyası + native debug sembolleri, release yapılandırması
- [ ] @ana-denetleyici · F2-16. Play Console: veri güvenliği formu koda göre düzenlenir; yükleme uyarıları, lansman öncesi rapor ve Android vitals okunup F2-15'e girdi verilir
### Kapanış
- [ ] @qa-tester · F2-7. Tüm bulguların cihazda yeniden doğrulanması + regresyon (bekler: F2-14, F2-15)
- [ ] @devops · F2-8. Sürüm 0.6.1 (code 7), CHANGELOG, imzalı AAB + APK, cihaza kurulum (bekler: F2-7, TODO-Faruk #1 keystore)
