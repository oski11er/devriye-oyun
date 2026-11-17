# ⚽ Futbol Menajer (Android)

Tamamen **çevrimdışı**, Türkçe, Football Manager / Soccer Manager tarzı futbol menajerlik simülasyonu.
Kotlin + Jetpack Compose · Android 8.0+ (minSdk 26) · APK boyutu ≈ 1,5 MB

---

## 1. Hızlı başlangıç (APK kurulumu)

1. `FutbolMenajer.apk` dosyasını telefona indirin.
2. Ayarlar → Güvenlik → **Bilinmeyen kaynaklardan uygulama yükleme**ye izin verin (Chrome/Dosyalar için).
3. APK'ya dokunup kurun. İnternet gerekmez.

Yeni sürümler GitHub Actions ile otomatik derlenir ve **Releases** sayfasına `FutbolMenajer-v1.0.N.apk` olarak eklenir.

---

## 2. Özellikler

| Sistem | İçerik |
|---|---|
| **Veritabanı** | 50 ülke, 75 lig (alt ligler dahil), ~1.270 kulüp, ~36.000 oyuncu, 64 milli takım, 250 oyuncu menajeri (ajan) |
| **Gerçek oyuncular** | 47 büyük kulübün 2025-26 kadroları (510 gerçek oyuncu) – `RealPlayers.kt`; kadronun geri kalanı kulüp seviyesine göre üretilir |
| **Yetenekler** | Hız, şut, pas, top sürme, savunma, fizik, oyun görüşü, soğukkanlılık, çalışkanlık, liderlik, kalecilik + potansiyel, kişilik, form, moral, kondisyon |
| **Turnuvalar** | Ligler (yükselme/düşme, play-off), yerel kupalar, süper kupalar, **UEFA Şampiyonlar Ligi / Avrupa Ligi / Konferans Ligi** (36 takımlı lig aşaması + play-off + eleme), milli maç araları, **Dünya Kupası** (48 takım) ve **EURO** |
| **Taktik** | 8 diziliş, oyun anlayışı, tempo, pres, pas stili, savunma hattı, 9 bireysel talimat, kaptan/penaltıcı/frikikçi, otomatik rotasyon |
| **Antrenman** | Takım + bireysel odak, 3 yoğunluk (gelişim ↔ yorgunluk/sakatlık riski), yaşa ve potansiyele bağlı gelişim/gerileme |
| **Gençlik akademisi** | Her Mart yeni gençler; scout seviyesine göre potansiyel tahmini; A takıma yükseltme |
| **Transfer** | Bonservis (taksit, sonradan satış payı), **takas**, **kiralık** (maaş katkısı, bedel, satın alma opsiyonu), serbest oyuncu, serbest kalma bedeli, karşı teklif/pazarlık turları, **ajan görüşmesi**, kişisel şartlar (maaş, süre, imza parası, ajan ücreti, rol vaadi), AI kulüplerin alım-satımı ve size teklifleri |
| **Maç motoru** | Dakika dakika metin + istatistik simülasyonu, canlı izleme (1x/2x/4x/anında), maç içi taktik ve oyuncu değişikliği, VAR, xG, penaltı, uzatma, seri penaltı, maç raporu ve reytingler |
| **2025-26 kuralları** | 5 değişiklik / 3 pencere (devre arası sayılmaz), uzatmada +1 değişiklik, kaleci 8 saniye kuralı (rakibe korner), deplasman golü yok, yalnızca kaptan hakemle konuşabilir |
| **Diğer** | Bütçe & finans (bilet, TV, sponsorluk, maaş, bakım), **tesis yükseltmeleri** (stadyum, antrenman, akademi, sağlık, scout, ticari), basın toplantıları, moral, sakatlık & disiplin (5 sarı = 1 maç, kırmızı = 1-3 maç), sezon hedefleri, yönetim güveni, kovulma ve iş teklifleri, 4 kayıt yuvası |

---

## 3. Mimari

```
FutbolMenajer/
├── app/src/main/java/com/futbolmenajer/
│   ├── MainActivity.kt                 Compose giriş noktası
│   ├── data/
│   │   ├── model/Models.kt             Tüm veri modelleri (@Serializable)
│   │   ├── db/LeagueCatalog.kt         Ülke → lig → kulüp kataloğu
│   │   ├── db/RealPlayers.kt           Gerçek oyuncu veritabanı (metin tablo)
│   │   ├── db/NameBank.kt              Uyruklara göre isim havuzları, milli takımlar
│   │   ├── db/WorldGenerator.kt        Katalog + gerçek oyunculardan dünya üretimi
│   │   └── SaveManager.kt              JSON + GZIP kaydet/yükle (4 yuva)
│   ├── engine/
│   │   ├── MatchEngine.kt              Adım adım maç simülasyonu (MatchSim, TeamSide)
│   │   ├── TransferMarket.kt           Transfer/takas/kiralık/pazarlık/ajan/AI transferleri
│   │   ├── Scheduler.kt                Sezon takvimi, fikstür, kura, eleme ilerlemesi
│   │   ├── GameEngine.kt               Gün akışı, sonuçlar, antrenman, finans, gençlik,
│   │   │                               tesisler, milli takımlar, sezon sonu
│   │   ├── Economy.kt                  Piyasa değeri, maaş, gelir/gider formülleri
│   │   ├── Lineups.kt                  Otomatik kadro seçimi
│   │   └── Press.kt                    Basın toplantıları
│   └── ui/
│       ├── GameViewModel.kt            Durum + navigasyon + arka plan işleri
│       ├── App.kt                      Scaffold, çekmece menü, DEVAM düğmesi
│       ├── Components.kt               Tema ve ortak bileşenler
│       └── screens/                    Menü, kulüp seçimi, ana sayfa, kadro, taktik,
│                                       antrenman, transfer, maç, tablo, finans, tesis...
├── app/src/test/.../SeasonSimulationTest.kt   2 sezonluk uçtan uca motor testi
└── .github/workflows/build-apk.yml     Otomatik test + APK derleme + Release
```

**Akış:** `DEVAM` → `GameEngine.advance()` günleri ilerletir; her gün o günün tüm maçları
`MatchSim` ile oynanır, turnuvalar ilerletilir, günlük/haftalık işlemler (sakatlık, kondisyon,
antrenman, maaş, transfer yanıtları, AI transferleri) yapılır. Kullanıcının maç günü, basın
toplantısı veya önemli bir mesaj geldiğinde durur. Ağır işler arka planda (`Dispatchers.Default`)
çalışır, arayüz o sırada yükleme ekranı gösterir.

### Maç motoru özeti
Her dakika: orta saha gücüne göre topa sahip olan taraf → hücum/savunma oranına göre pozisyon
olasılığı → xG üretimi → şutör seçimi (pozisyon, şut, talimat) → bitiricilik vs kaleci ile gol
olasılığı. Faul/kart, korner, ofsayt, sakatlık, VAR iptali, penaltı, 8 saniye kuralı ayrıca
zarlanır. Yorgunluk dakika başı düşer (pres ve tempo artırır) ve güce yansır. Test ölçümü:
maç başı ≈ 2,5-2,7 gol, ev sahibi galibiyeti ≈ %42, beraberlik ≈ %24.

### Transfer akışı
`submitOffer` → (1-2 gün) kulüp değerlendirir: kabul / karşı teklif (en fazla 3 tur) / ret →
`acceptCounter` veya `improveOffer` → `negotiateTerms` (ajanla 3 tur) → `complete`.
Takasta oyuncular satıcının ihtiyacına ve kalitesine göre %30-100 değerle sayılır ve takas
oyuncularının da transfere razı olması gerekir.

---

## 4. Kaynaktan derleme

### A) GitHub Actions (bilgisayar gerekmez)
1. Projeyi bir GitHub reposuna yükleyin (`.github/workflows/build-apk.yml` dahil).
2. Workflow'daki `branches: [ futbol-menajer ]` satırını kendi dalınıza göre değiştirin (ör. `main`).
3. Her push'ta testler çalışır, APK derlenir; **Actions → Artifacts** veya **Releases**'tan indirin.

### B) Android Studio
1. Android Studio (Ladybug veya üstü) ile `FutbolMenajer` klasörünü açın, Gradle senkronunu bekleyin.
2. `Build → Build App Bundle(s) / APK(s) → Build APK(s)`.
3. Çıktı: `app/build/outputs/apk/release/app-release.apk`.

### C) Komut satırı
```bash
# JDK 17 ve Android SDK (ANDROID_HOME) kurulu olmalı
./gradlew testReleaseUnitTest   # motor testleri (2 sezon simülasyonu)
./gradlew assembleRelease       # → app/build/outputs/apk/release/
```

### Kendi anahtarınızla imzalama (Play Store için)
```bash
keytool -genkey -v -keystore futbol.jks -keyalg RSA -keysize 2048 -validity 10000 -alias futbol
```
Proje köküne `keystore.properties`:
```
storeFile=futbol.jks
storePassword=******
keyAlias=futbol
keyPassword=******
```
Bu dosya yoksa APK, kurulabilir olması için debug anahtarıyla imzalanır.

---

## 5. Veritabanını genişletme
* **Oyuncu eklemek:** `RealPlayers.kt` içinde kulübün bloğuna `Ad Soyad;POZ;DoğumYılı;UYRUK;Güç;Potansiyel` satırı ekleyin.
* **Lig/kulüp eklemek:** `LeagueCatalog.kt` içinde ülkeye `LeagueDef` ekleyin; kulüp adı verilmezse şehir listesinden üretilir.
* **Kulüp gücü:** `WorldGenerator.repOverride`.

## 6. Notlar ve sınırlar
* Gerçek oyuncu güç değerleri oyun dengesi için tahminidir; kadrolar 2025-26 sezonu bilgisine dayanır, 2026 yaz transferleri eksik olabilir.
* Oyuncu fotoğrafları ve resmî lig/kulüp logoları telif nedeniyle eklenmemiştir.
* Kayıt dosyası ≈ 3-5 MB (sıkıştırılmış); düşük seviye telefonlarda kaydetme birkaç saniye sürebilir.
