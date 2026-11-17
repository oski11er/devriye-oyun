class_name C
extends RefCounted
## Ortak sabitler ve yardımcılar (metre cinsinden dünya)

const S := 64.0          # blok aralığı (yol merkezleri arası)
const RW := 14.0         # şehir yolu genişliği
const HW := 24.0         # otoyol genişliği (z = 0 satırı)
const WB := 6            # dünya: -WB..WB-1 blok
const LIM := 384.0       # WB * S
const TS := 0.5          # oyun dakikası / gerçek saniye
const KMH := 3.6

const DIST := {
	"merkez": {"n": "Merkez", "crime": 1.0, "desc": "Dengeli suç, yoğun trafik"},
	"konut": {"n": "Yeşilvadi Konutları", "crime": 0.75, "desc": "Ev hırsızlıkları"},
	"sanayi": {"n": "Sanayi Bölgesi", "crime": 1.25, "desc": "Depolar, kaçakçılık"},
	"liman": {"n": "Liman", "crime": 1.6, "desc": "Organize suç"},
	"eski": {"n": "Eski Şehir", "crime": 1.4, "desc": "Gece kavgaları, gasp"},
}
const RANKS := [
	{"n": "Polis Memuru Adayı", "xp": 0, "sal": 120},
	{"n": "Polis Memuru", "xp": 350, "sal": 160},
	{"n": "Kıdemli Polis Memuru", "xp": 1000, "sal": 210},
	{"n": "Komiser Yardımcısı", "xp": 2200, "sal": 280},
	{"n": "Komiser", "xp": 4000, "sal": 360},
	{"n": "Başkomiser", "xp": 6800, "sal": 460},
	{"n": "Emniyet Amiri", "xp": 10500, "sal": 600},
	{"n": "Emniyet Müdürü", "xp": 16000, "sal": 800},
]
# Araçlar: motor gücü, azami hız (m/s), kütle, boya, tepe lambası
const VEH := {
	"sedan": {"n": "Devriye Sedanı", "eng": 4800.0, "max": 47.0, "mass": 1350.0, "c1": "#f2f4f7", "c2": "#10295f", "lb": true, "rank": 0},
	"suv": {"n": "Devriye SUV", "eng": 5200.0, "max": 44.0, "mass": 1900.0, "c1": "#1d2a3c", "c2": "#e9edf2", "lb": true, "rank": 1, "dur": 0.6},
	"moto": {"n": "Polis Motosikleti", "eng": 3600.0, "max": 52.0, "mass": 380.0, "c1": "#f2f4f7", "c2": "#10295f", "lb": true, "rank": 1, "moto": true, "dur": 1.8},
	"traffic": {"n": "Trafik Polisi Aracı", "eng": 5000.0, "max": 50.0, "mass": 1350.0, "c1": "#ececec", "c2": "#e8c400", "lb": true, "rank": 1, "radar": 1.8},
	"civil": {"n": "Sivil Polis Aracı", "eng": 4900.0, "max": 49.0, "mass": 1350.0, "c1": "#3a3f47", "c2": "#3a3f47", "lb": false, "rank": 2, "civil": true},
	"special": {"n": "Özel Operasyon Aracı", "eng": 6200.0, "max": 42.0, "mass": 2600.0, "c1": "#141a22", "c2": "#141a22", "lb": true, "rank": 4, "dur": 0.35},
	"gt": {"n": "Interceptor GT ★", "eng": 6400.0, "max": 56.0, "mass": 1300.0, "c1": "#0a1a3a", "c2": "#c9d6ea", "lb": true, "rank": 0, "premium": 150},
}
const HOMES := [
	{"n": "Lojman koğuşu", "rent": 0, "price": 0, "com": 38},
	{"n": "Kiralık 1+1 (Eski Şehir)", "rent": 40, "price": 0, "com": 55},
	{"n": "Kiralık 2+1 (Merkez)", "rent": 85, "price": 0, "com": 68},
	{"n": "Satılık daire (Yeşilvadi)", "rent": 0, "price": 9000, "com": 80},
	{"n": "Bahçeli villa", "rent": 0, "price": 40000, "com": 92},
]
const FURN := [
	{"id": "bed", "n": "Ortopedik yatak", "p": 400, "com": 8},
	{"id": "sofa", "n": "Kanepe takımı", "p": 300, "com": 4},
	{"id": "tv", "n": "Akıllı TV", "p": 450, "com": 3},
	{"id": "coffee", "n": "Kahve makinesi", "p": 150, "com": 2},
	{"id": "books", "n": "Kitaplık", "p": 120, "com": 2},
	{"id": "plants", "n": "Bitki ve dekor", "p": 80, "com": 2},
	{"id": "lux", "n": "Lüks dekor paketi ★", "cr": 60, "com": 8},
]
const OUTFITS := [
	{"id": "std", "n": "Standart lacivert üniforma", "p": 0, "col": "#1a2c5c"},
	{"id": "winter", "n": "Kışlık mont", "p": 80, "col": "#2b3a4a"},
	{"id": "civil", "n": "Sivil kıyafet (gizli görevde fark edilmeyi azaltır)", "p": 60, "col": "#6b5b4b"},
	{"id": "night", "n": "Gece Operasyon üniforması ★", "cr": 30, "col": "#111821"},
	{"id": "parade", "n": "Tören üniforması ★", "cr": 40, "col": "#0d2a5c"},
]
const LIVERIES := [
	{"id": "std", "n": "Standart", "cr": 0},
	{"id": "retro", "n": "Retro beyaz-kırmızı ★", "cr": 40},
	{"id": "matte", "n": "Mat siyah ★", "cr": 50},
]
const PCARS := [{"n": "İkinci el hatchback", "p": 2500}, {"n": "Aile sedanı", "p": 7000}, {"n": "Spor coupe", "p": 22000}]
const TRAITS := ["Sürücü", "Dedektif", "Müzakereci", "Taktik"]
const FN := ["Ahmet", "Mehmet", "Mustafa", "Ali", "Hüseyin", "Emre", "Burak", "Can", "Murat", "Kerem", "Ayşe", "Fatma", "Zeynep", "Elif", "Merve", "Selin", "Deniz", "Ece", "Hakan", "Serkan", "Okan", "Gökhan", "Cem", "Tolga", "Yusuf", "Kaan", "Derya", "Gizem"]
const LN := ["Yılmaz", "Kaya", "Demir", "Şahin", "Çelik", "Yıldız", "Aydın", "Öztürk", "Arslan", "Doğan", "Kılıç", "Aslan", "Koç", "Kurt", "Polat", "Erdem", "Güneş", "Tekin", "Bulut", "Korkmaz", "Özdemir", "Karaca"]
const COLORS := [["Beyaz", "#e9ecef"], ["Siyah", "#1d2024"], ["Kırmızı", "#a3201a"], ["Mavi", "#1f4fa8"], ["Gri", "#7d838c"], ["Yeşil", "#2a6e48"], ["Sarı", "#d9a91c"]]
const PRI := ["ACİL", "ÖNCELİKLİ", "NORMAL"]
const DV := [Vector2(1, 0), Vector2(0, 1), Vector2(-1, 0), Vector2(0, -1)]

static func hh(k: int) -> float:
	return HW * 0.5 if k == 0 else RW * 0.5

static func district_at(cx: int, cz: int) -> String:
	if cx >= 2:
		return "liman" if cz < 0 else "sanayi"
	if cx <= -3:
		return "eski"
	if absi(cz) >= 2:
		return "konut"
	return "merkez"

static func district_p(p: Vector2) -> String:
	return district_at(clampi(floori(p.x / S), -WB, WB - 1), clampi(floori(p.y / S), -WB, WB - 1))

static func block_rect(cx: int, cz: int) -> Rect2:
	var x0 := cx * S + RW * 0.5
	var z0 := cz * S + hh(cz)
	var x1 := (cx + 1) * S - RW * 0.5
	var z1 := (cz + 1) * S - hh(cz + 1)
	return Rect2(x0, z0, x1 - x0, z1 - z0)

static func pick(a: Array) -> Variant:
	return a[randi() % a.size()]

static func rname() -> String:
	return pick(FN) + " " + pick(LN)

static func plate() -> String:
	var l := "ABCDEFGHJKLMNPRSTUVYZ"
	return "34 %s%s %d" % [l[randi() % l.length()], l[randi() % l.length()], randi_range(100, 999)]

static func fmt_t(s: float) -> String:
	var t := maxi(0, ceili(s))
	return "%d:%02d" % [t / 60, t % 60]

static func fmt_clock(t: float) -> String:
	var m := int(t)
	return "%02d:%02d" % [(m / 60) % 24, m % 60]

static func yaw_of(d: Vector2) -> float:
	return atan2(d.x, d.y)

static func dir_yaw(dir: int) -> float:
	return [PI / 2, 0.0, -PI / 2, PI][dir]
