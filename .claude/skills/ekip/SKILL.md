---
name: ekip
description: Proje ajan ekibini PROGRESS.md'deki bağımlılık sırasına göre paralel çalıştırır. Kullanıcı "/ekip", "ekibi başlat" veya "tüm ajanlar başlasın" dediğinde kullan. İsteğe bağlı argüman olarak faz numarası veya tek bir ajan adı alır.
---

# /ekip — ekip orkestrasyonu

Ana denetleyici sensin. Ajanları `.claude/agents/` altındaki tanımlarıyla çalıştır. Model ve efor ayarları bu tanımlarda yazılı, Agent çağrısında **override etme**.

> CLAUDE.md'nin ilk satırında "SABLON-DURUMU: KURULMADI" varsa önce `kurulum` skill'ini öner. Kurulum bitmeden ekibi çalıştırma.

## Döngü
1. `PROGRESS.md` ve `TODO-<kullanıcı>.md` dosyalarını oku.
2. **Hazır işleri bul:** Açık (`- [ ]`) ve `bekler:` kısmındaki işlerin hepsi kapanmış işler. Argüman verildiyse listeyi o faza veya o ajana göre filtrele.
3. **Paralel başlat:**
   - Hazır işleri tek mesajda, birden çok Agent çağrısıyla arka planda başlat (`subagent_type` = ajan adı).
   - **Doluluk hedefi: aynı anda en az 3, en çok 5–6 ajan.** Hazır iş varsa ajanları boşta bırakma. Aynı ajan tipinden birden çok örnek açılabilir; iki örnek aynı dosyaya dokunmaz.
   - **Karara bağlı işi böl:** kullanıcı cevabı bekleyen bir işin karara bağlı olmayan kısmını ve kod dışı hazırlığını (araştırma, metin taslağı, tasarım şartnamesi, ADR'nin karara bağlı olmayan bölümü) ayrı iş satırı yapıp hemen başlat. Karar gelince çalışan ajana SendMessage ile ilet.
   - **Aynı dosyaya dokunacak iki işi paralel başlatma.** Kaçınılmazsa her ajana dokunabileceği ve dokunamayacağı dosyaları açıkça yaz, ortak dosyayı düzenlemeden önce yeniden okumasını söyle.
   - Prompt'a şunları koy: iş satırı, ilgili brief bölüm numaraları (brief'in tamamını değil), önceki işlerden gelen kancalar ve kararlar, çakışma kuralı, "git stash, checkout veya reset kullanma", "commit atma, PROGRESS ve DONE'u güncelleme", rapor formatı.
4. **Rapor gelince:**
   - **Doğrula:** Ajanın beyanına güvenme. Build, lint, tip ve test komutlarını kendin çalıştır. Görsel iş ise ekran görüntüsüne kendin bak. Rakam ve iddiaları kaynağa karşı grep'le.
   - **Yeterliyse:** İşi PROGRESS'ten sil ve DONE.md'ye tek satır yaz (tarih · iş · doğrulama). Ardından o iş için tek commit at. Paralel ajan varken yalnızca o işin dosyalarını stage et ve `git status` ile sır kontrolü yap.
   - **Eksikse:** Aynı ajana düzeltme görevi ver. Bir işte en fazla 2 tur düzeltme yapılır, sonrasında kullanıcıya bildir.
   - **Sorular:** "Kullanıcıya soru" maddelerini TODO dosyasına ekle. Soru işi engelliyorsa o işi beklet, diğerlerine devam et. Kullanıcı karar verince kararı CLAUDE.md "Kararlar" bölümüne tek satır yaz.
   - **Kancalar:** Sonraki işler için kalıcı teknik bilgiyi (dosya yolu, veri sözleşmesi, ölçüm tabanı) PROGRESS'teki "Kalıcı teknik kurallar" bölümüne ekle.
   - **Haber ver:** Bekleyen işin bağımlılığı açıldıysa çalışan ajana SendMessage ile bildir.
5. **Doluluk kontrolü:** her rapordan sonra çalışan ajan sayısına bak. Sayı 2'ye ya da altına düştüyse PROGRESS'teki sıradaki hazır işleri hemen ata ve sayıyı 3–6 aralığına geri çıkar. Hazır iş yoksa 3. adımdaki gibi bekleyen işleri böl.
   - **Paylaşılan kaynaklar sırayla:** ortak test ortamını (ör. paylaşılan Docker Compose, tek veritabanı) kullanan testler ve ortak build çıktısı (ör. `.next`) aynı anda tek ajanda koşar. Tam CI koşarken kodu değiştiren işler başlatılmaz; o sırada kod dışı işler verilir.
6. Yeni hazır iş kalmayana kadar 2. adıma dön.

## Durma noktaları (kullanıcıya dön)
- **Faz bitince** durum raporu ver:
  - ne yapıldı, ne eksik;
  - bekleyen kullanıcı soruları;
  - ölçümler ve ekran görüntüsü yolları.
  Ardından sonraki fazın işlerini PROGRESS'e `@ajan` ve `bekler:` etiketleriyle ekle, `find-skills` önerisini sun ve "devam" komutunu bekle.
- **Kurulum gerektiğinde** onay iste: yazılım, skill, MCP ya da yığında olmayan npm, pip veya composer paketi.
- **Kırmızı çizgi riski** varsa ya da kullanıcı kararı gerektiren bir engel çıkarsa dur ve sor.

## Maliyet disiplini
- Basit, mekanik düzeltmeleri (tek satır, yazım hatası, import) ajan başlatmadan kendin yap.
- Raporları özetleyerek birleştir. Kullanıcıya ham ajan çıktısını aktarma.
- Kullanıcı başka bir iş isterse (proje dışı da olabilir) ekibi bozmadan onu yap. Çalışan ajanlar arka planda devam eder.
