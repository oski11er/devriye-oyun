# 🚓 Devriye: Şehrin Nabzı

3D açık dünya polis simülasyonu (Android). Three.js ile gerçek zamanlı 3D: gölgeler, gündüz/gece döngüsü, hava durumu, sektör bazlı şehir yükleme.

## Telefona kurulum
1. Bu sayfada sağdaki **Releases** bölümünden en son sürümü aç.
2. `Devriye.apk` dosyasına dokunup indir.
3. Açılan uyarıda **"Bu kaynaktan yüklemeye izin ver"** seçeneğini aç ve **Yükle**'ye bas.

## Nasıl derleniyor
`main` dalına her gönderimde GitHub Actions (`.github/workflows/build-apk.yml`) Capacitor ile Android projesini oluşturur, APK'yı derler ve yeni bir Release yayınlar.

## Dosyalar
- `www/index.html` — oyunun tamamı (mantık + 3D motor)
- `www/three.min.js` — Three.js r160
- `tools/patch_android.py` — yatay ekran, tam ekran, ekranı açık tutma ayarları

## Kontroller
- Sol joystick: sağ-sol direksiyon, yukarı gaz, aşağı fren/geri
- Sağ butonlar: Etkileşim, Fren, El freni, Siren, Korna, Far, İn/Bin, Kamera (vücut kamerası), Takviye, Dosya, Telsiz, Görüş (takip / yukarıdan kamera)
- Duraklat menüsünden grafik kalitesi: Yüksek / Orta / Düşük
