# PROGRESS — aktif ve sıradaki işler
> Format: `- [ ] @ajan · F<faz>-<no>. iş · (bekler: F1-2, …)`. Biten iş buradan silinir ve DONE.md'ye tek satır yazılır.

## Kalıcı teknik kurallar
- adb: `C:/Android/Sdk/platform-tools/adb.exe` (Git Bash'te `MSYS_NO_PATHCONV=1`). Test cihazı Redmi K20 Pro / Android 16, 3 tuşlu gezinme: alt ~130 px gezinme çubuğu.
- Bulgu numaraları (B1–B27) 2026-11-26 QA raporundan; ayrıntı DONE/commit geçmişinde.
- Paralel çalışmada yeni metinler ajana ait ayrı kaynak dosyasına eklenir (`values/strings_<alan>.xml` + `values-tr/strings_<alan>.xml`); `strings.xml`'e yalnızca @content dokunur.

## Faz 2 — Onaylanan düzeltmeler (v0.6.1)
### Dalga 2
- [ ] @android-dev · F2-6b. Okuyucu kontrastı + okuyucu alt menüleri (Diğer Seçenekler, Yakınlaştırma, Görüntüleme) + RemoveFavoriteSheet; token API: MaterialTheme.accents.<rol>.{color,container,onContainer,onColor}
- [ ] @android-dev · F2-12. Açık temada ikincil metin kontrastı (onSurfaceVariant alpha 0.4–0.7 → ≥4.5:1) + PDF'den Görseller'e dosya adı öneki (Böl'deki gibi); okuyucu hariç
- [ ] @ana-denetleyici · F2-13. Play Console "Tüm dosyalara erişim" gerekçe metni güncellemesi (Edge)
### Kapanış
- [ ] @qa-tester · F2-7. Tüm bulguların cihazda yeniden doğrulanması + regresyon (bekler: F2-6b)
- [ ] @devops · F2-8. Sürüm 0.6.1 (code 7), CHANGELOG, imzalı AAB + APK, cihaza kurulum (bekler: F2-7, TODO-Faruk #1 keystore)
