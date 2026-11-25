# PROGRESS — aktif ve sıradaki işler
> Format: `- [ ] @ajan · F<faz>-<no>. iş · (bekler: F1-2, …)`. Biten iş buradan silinir ve DONE.md'ye tek satır yazılır.

## Kalıcı teknik kurallar
- adb: `C:/Android/Sdk/platform-tools/adb.exe` (Git Bash'te `MSYS_NO_PATHCONV=1`). Test cihazı Redmi K20 Pro / Android 16, 3 tuşlu gezinme: alt ~130 px gezinme çubuğu.
- Kullanıcı kararı (2026-11-26): silme düzeltmesinden sonra tüm özellikler test edilir, bulgular listelenir, **onay gelmeden yeni düzenleme yapılmaz**.

## Faz 1 — Müşteri hatası: PDF silinmiyor + stabilizasyon (v0.6.1)
- [ ] @qa-tester · F1-5. Tüm özelliklerin cihazda testi (okuyucu, 12 araç, klasör, arama, favori/son, ayarlar, toplu silme, yeniden adlandırma) → bulgu listesi kullanıcıya sunulur
- [ ] @devops · F1-6. Sürüm 0.6.1 (code 7), CHANGELOG, imzalı AAB + APK, cihaza kurulum (bekler: F1-5 ve onaylanan düzeltmeler)
