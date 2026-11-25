# PROGRESS — aktif ve sıradaki işler
> Format: `- [ ] @ajan · F<faz>-<no>. iş · (bekler: F1-2, …)`. Biten iş buradan silinir ve DONE.md'ye tek satır yazılır.

## Kalıcı teknik kurallar
- adb: `C:/Android/Sdk/platform-tools/adb.exe` (Git Bash'te `MSYS_NO_PATHCONV=1`). Test cihazı Redmi K20 Pro / Android 16, 3 tuşlu gezinme: alt ~130 px gezinme çubuğu.
- Bulgu numaraları (B1–B27) 2026-11-26 QA raporundan; ayrıntı DONE/commit geçmişinde.
- Paralel çalışmada yeni metinler ajana ait ayrı kaynak dosyasına eklenir (`values/strings_<alan>.xml` + `values-tr/strings_<alan>.xml`); `strings.xml`'e yalnızca @content dokunur.

## Faz 2 — Onaylanan düzeltmeler (v0.6.1)
### Dalga 1 (paralel)
- [~] @android-dev · F2-2. Okuyucu (B9, B12, B21, B26, okuyucu i18n): kod tamam, compileDebugKotlin temiz; commit ve DONE, `ReaderViewModelTest` çalışınca (B22'ye bağlı). Dosyalar: screens/reader/{ReaderViewModel,ReaderState,ReaderScreen}.kt, reader/components/{PageScrubber,MoreOptionsSheet,AutoScrollSheet,AutoScrollOverlay,FloatingControlBar,ReaderTopBar,EnhancedTableOfContents}.kt, components/pdf/WebInterface.kt, assets/.../pdfjs/helper.css, values*/strings_reader.xml, test ReaderViewModelTest.kt
- [ ] @android-dev · F2-3. Araçlar: B2 yanlış şifre boş dosya, B3 küçük resim zemini, B13 sonuç yolu/adı, B14 son klasörü hatırla, B16, B17, B18, B20, B11 kategori başlıkları, araç i18n, B22 testlerin onarımı
### Dalga 2
- [ ] @android-dev · F2-6. B4 koyu tema kontrastı (tema token'ları, sabit renklerin kaldırılması) + B6 alt menüler + B7 seçim çubuğu + B8 sekme + `FileOperations.kt` paylaşım başlığı "Share N PDFs" i18n + Ayarlar'a "Varsayılan kayıt klasörü" satırı (değiştir / her seferinde sor; F2-3'ün OutputFolderStore API'si) (bekler: F2-1, F2-2, F2-3, F2-4)
- [ ] @android-dev · F2-9. Manifest izin temizliği (Play taraması): WRITE_EXTERNAL_STORAGE `maxSdkVersion=29`, READ_EXTERNAL_STORAGE `maxSdkVersion=32`; `requestLegacyExternalStorage` Android 10 için kalır; INTERNET kararı TODO-Faruk #2'ye bağlı (bekler: F2-1)
### Kapanış
- [ ] @qa-tester · F2-7. Tüm bulguların cihazda yeniden doğrulanması + regresyon (bekler: F2-6)
- [ ] @devops · F2-8. Sürüm 0.6.1 (code 7), CHANGELOG, imzalı AAB + APK, cihaza kurulum (bekler: F2-7, TODO-Faruk #1 keystore)
