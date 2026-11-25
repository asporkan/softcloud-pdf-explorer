# PROGRESS — aktif ve sıradaki işler
> Format: `- [ ] @ajan · F<faz>-<no>. iş · (bekler: F1-2, …)`. Biten iş buradan silinir ve DONE.md'ye tek satır yazılır.

## Kalıcı teknik kurallar
- adb: `C:/Android/Sdk/platform-tools/adb.exe` (Git Bash'te `MSYS_NO_PATHCONV=1`). Test cihazı Redmi K20 Pro / Android 16, 3 tuşlu gezinme: alt ~130 px gezinme çubuğu.
- Kullanıcı kararı (2026-11-26): silme düzeltmesinden sonra tüm özellikler test edilir, bulgular listelenir, **onay gelmeden yeni düzenleme yapılmaz**.

## Faz 1 — Müşteri hatası: PDF silinmiyor + stabilizasyon (v0.6.1)
- [ ] @kullanıcı · F1-5. Bulgu listesi (B1–B27) onayı bekleniyor → onaylanan paketler F2 olarak buraya yazılır
- [ ] @devops · F1-6. Sürüm 0.6.1 (code 7), CHANGELOG, imzalı AAB + APK, cihaza kurulum (bekler: F1-5 onayı ve F2)
