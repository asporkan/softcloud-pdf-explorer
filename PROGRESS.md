# PROGRESS — aktif ve sıradaki işler
> Format: `- [ ] @ajan · F<faz>-<no>. iş · (bekler: F1-2, …)`. Biten iş buradan silinir ve DONE.md'ye tek satır yazılır.

## Kalıcı teknik kurallar
- adb: `C:/Android/Sdk/platform-tools/adb.exe` (Git Bash'te `MSYS_NO_PATHCONV=1`). Test cihazı Redmi K20 Pro / Android 16, 3 tuşlu gezinme: alt ~130 px gezinme çubuğu.
- Bulgu numaraları (B1–B27) 2026-11-26 QA raporundan; ayrıntı DONE/commit geçmişinde.
- Paralel çalışmada yeni metinler ajana ait ayrı kaynak dosyasına eklenir (`values/strings_<alan>.xml` + `values-tr/strings_<alan>.xml`); `strings.xml`'e yalnızca @content dokunur.

## Faz 2 — Onaylanan düzeltmeler (v0.6.1)
### Dalga 2
- [ ] @android-dev · F2-9. Okuyucu ve manifest: B28 ek kaydetme MediaStore Downloads'a, B29 "Tekrar dene" çalışır, B30 ölü okuyucu bileşenleri ve SaveDocument silinir; manifest: INTERNET kaldırılır, WRITE `maxSdkVersion=29`, READ `maxSdkVersion=32`, `requestLegacyExternalStorage` Android 10 için kalır
- [ ] @android-dev · F2-6a. Koyu tema token'ları + B4 kontrast (okuyucu hariç tüm ekranlar) + B6 alt menüler (okuyucu hariç) + B7 seçim çubuğu + B8 sekme + Ayarlar: "Varsayılan kayıt klasörü" (OutputFolderStore) ve "Vertical/Light" Türkçesi + `FileOperations` paylaşım başlığı i18n + kullanılmayan `tool_category_*` anahtarları
- [ ] @android-dev · F2-6b. Okuyucu kontrastı + okuyucu alt menüleri (Diğer Seçenekler, Yakınlaştırma, Görüntüleme) (bekler: F2-9, F2-6a)
- [ ] @ana-denetleyici · F2-10. Play Console kontrolü (Edge): All files access beyanı, veri güvenliği formu, gizlilik politikası URL'si
### Kapanış
- [ ] @qa-tester · F2-7. Tüm bulguların cihazda yeniden doğrulanması + regresyon (bekler: F2-6b, F2-9)
- [ ] @devops · F2-8. Sürüm 0.6.1 (code 7), CHANGELOG, imzalı AAB + APK, cihaza kurulum (bekler: F2-7, TODO-Faruk #1 keystore)
