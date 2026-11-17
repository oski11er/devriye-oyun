# 🚓 Devriye: Şehrin Nabzı (native Android, Godot 4)

Native Android 3D polis simülasyonu. WebView yok: Godot 4 motoru, Vulkan "Mobile" renderer.

- Fotogerçekçi PBR dokular ve HDRI gökyüzü (Poly Haven, CC0 — derleme sırasında indirilir)
- Fizikli polis aracı (VehicleBody3D), PBR araç modeli: Khronos "CarConcept" (CC-BY 4.0)
- Animasyonlu karakter: GDQuest "Mannequiny" (CC-BY 4.0, GDQuest and contributors)
- 1. şahıs / 3. şahıs kamera, gece/gündüz, siren
- Dokunmatik kontroller: ◀ ▶ direksiyon, GAZ, FREN, EL FRENİ; yaya modunda joystick; ekranı sürükleyerek kamera

## Kurulum
Releases bölümünden en son `Devriye.apk` dosyasını indir ve kur.

## Derleme
`main` dalına her gönderimde `.github/workflows/build.yml` çalışır: dokuları indirir, oyun içi ekran görüntüleri alır (`ci-out` dalı), APK'yı derler ve Release yayınlar.

Önceki WebView sürümü `web/` klasöründe.
