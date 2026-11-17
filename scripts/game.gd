extends Node
## Oyun mantığı: kariyer, vardiya, görevler, diyaloglar, delil, vücut kamerası, yolsuzluk, kişisel yaşam, hikâye

const SAVE := "user://devriye_save.json"
var M
var G := {}
var SH = null
var mission = null
var offer = null
var offer_t := 0.0
var next_offer := 8.0
var stop_target = null
var stop_timer := 0.0
var backup_cd := 0.0
var chatter_t := 15.0
var cam_on := true
var interact = null
var int_spots: Array = []
var int_mission = null
var hub_tab := "durum"
var state := "title"     # title / hub / play / interior / over
var hud_t := 0.0

func setup(main) -> void:
	M = main

func playing() -> bool:
	return state == "play" or state == "interior"

# ---------------------------------------------------------------- yardımcılar
func dist_n(k: String) -> String:
	return C.DIST[k]["n"]

func now_abs() -> float:
	return float(G["day"]) * 1440.0 + float(G["time"])

func night_f() -> float:
	if G.size() == 0:
		return 0.0
	var h: float = float(G["time"]) / 60.0
	if h < 5 or h >= 21:
		return 1.0
	if h < 7:
		return 1.0 - (h - 5) / 2.0
	if h >= 19:
		return (h - 19) / 2.0
	return 0.0

func crime_mul() -> float:
	return 1.0 + night_f() * 0.7 - (0.15 if M.weather == "rain" else 0.0)

func radio(msg: String, kind: String = "") -> void:
	M.ui.toast(msg, kind)

func modal(title: String, body: String, buttons: Array, extra: Callable = Callable()) -> void:
	M.ui.modal(title, body, buttons, extra)

func B(t: String, f: Callable = Callable(), c: String = "", dis: bool = false, keep: bool = false) -> Dictionary:
	return {"t": t, "f": f, "c": c, "dis": dis, "keep": keep}

func pos_of(e) -> Vector2:
	return e.pos

func pp() -> Vector2:
	return M.player_pos()

func mix_pct(v: float) -> String:
	return "%" + str(roundi(v * 100))

# ---------------------------------------------------------------- kayıt
func new_game(name: String) -> void:
	G = {"name": name, "day": 1, "time": 420.0, "money": 300, "credits": 20, "xp": 0, "rank": 0, "disc": 0, "integrity": 50, "suspicion": 0.0, "corrupt": 0, "investigation": false, "suspended": false, "fatigue": 10.0, "hunger": 20.0, "stress": 10.0, "hp": 100.0, "home": 0, "own_homes": [0], "furn": [], "meals": 2, "outfit": "std", "outfits": ["std"], "livery": "std", "liveries": ["std"], "premium": {}, "pcar": -1, "team": [], "recruits": [], "recruit_day": 0, "story": {"stage": 0, "path": "", "info": 0, "ending": "", "planned": 0, "kuzgun_pay": false}, "stats": {"done": 0, "fail": 0, "arrests": 0, "stops": 0}, "bodycam": [], "complaints": [], "rel": 30, "shift_type": "day", "region": "merkez", "veh": "sedan", "medal": false, "engaged": false, "coffee_day": -1, "late": {}}

func save() -> void:
	var f := FileAccess.open(SAVE, FileAccess.WRITE)
	if f:
		f.store_string(JSON.stringify(G))

func load_save() -> Dictionary:
	if not FileAccess.file_exists(SAVE):
		return {}
	var f := FileAccess.open(SAVE, FileAccess.READ)
	if f == null:
		return {}
	var d = JSON.parse_string(f.get_as_text())
	return d if d is Dictionary else {}

func status_text() -> String:
	var nx := ""
	if int(G["rank"]) < 7:
		nx = " / " + str(C.RANKS[int(G["rank"]) + 1]["xp"])
	return "Rütbe: [b]%s[/b]\nXP: %d%s\nDisiplin puanı: %d • Dürüstlük: %d/100\nBu vardiya: %d başarılı / %d başarısız" % [C.RANKS[int(G["rank"])]["n"], int(G["xp"]), nx, int(G["disc"]), int(G["integrity"]), SH["done"] if SH else 0, SH["fail"] if SH else 0]

# ---------------------------------------------------------------- vücut kamerası, disiplin, yolsuzluk
func bc(text: String, f: bool = false) -> void:
	if cam_on and G.size() > 0:
		G["bodycam"].append({"d": G["day"], "t": C.fmt_clock(G["time"]), "x": text, "f": f})
		if G["bodycam"].size() > 160:
			G["bodycam"].pop_front()
	if mission != null:
		mission["log"].append(text if cam_on else "(kayıt yok — kamera kapalı)")

func flag(text: String, d: int, cp: float) -> void:
	bc("⚠ " + text, true)
	if SH == null:
		return
	SH["flags"].append(text)
	SH["disc"] += ceili(d / 3.0)
	if randf() < cp:
		G["complaints"].append({"text": text, "cam": cam_on, "d": d, "day": G["day"], "just": false})

func just_force(text: String) -> void:
	bc("Güç kullanımı (gerekçeli): " + text)
	if randf() < 0.15:
		G["complaints"].append({"text": "Güç kullanımı şikâyeti: " + text, "cam": cam_on, "d": 2, "day": G["day"], "just": true})

func witnesses() -> float:
	var n := 0.0
	var p := pp()
	for e in M.traffic.peds:
		if not e.down and e.kind != "suspect" and e.pos.distance_to(p) < 18.0:
			n += 1.0
	for c in M.traffic.cars:
		if c.kind == "civ" and c.pos.distance_to(p) < 16.0:
			n += 0.5
	return n

func cops_near() -> int:
	var n := 0
	var p := pp()
	for c in M.traffic.cars:
		if c.kind == "cop" and c.pos.distance_to(p) < 36.0:
			n += 1
	return n

func risk_calc() -> float:
	return clampf(0.08 + witnesses() * 0.045 + (0.45 if cam_on else 0.0) + cops_near() * 0.22 + (0.15 if G["investigation"] else 0.0) + (0.1 if G["story"]["path"] == "wire" else 0.0), 0.03, 0.97)

func corrupt_act(text: String, amt: int, risk: float, sev: int) -> void:
	var sh: Dictionary = SH if SH != null else G["late"]
	if not sh.has("dirty"):
		sh["dirty"] = 0
		sh["corrupt_n"] = 0
		sh["detections"] = []
	G["money"] += amt
	sh["dirty"] += amt
	G["corrupt"] += sev
	G["integrity"] = clampi(int(G["integrity"]) - 6 * sev, 0, 100)
	G["suspicion"] = clampf(float(G["suspicion"]) + risk * 22.0 * sev, 0, 100)
	sh["corrupt_n"] += 1
	bc("✖ %s ($%d)" % [text, amt], true)
	if randf() < risk:
		sh["detections"].append({"text": text, "sev": sev})
		if randf() < 0.5:
			radio(C.pick(["Bir tanığın telefonla kayıt aldığını fark ettin...", "Yakındaki ekip seni dikkatle izliyordu...", "Bir şeylerin kayda geçtiğini hissediyorsun."]), "warn")

func honest(text: String, n: int) -> void:
	G["integrity"] = clampi(int(G["integrity"]) + n, 0, 100)
	if SH:
		SH["honest"] += 1
	bc("✔ " + text)

func toggle_cam() -> void:
	if cam_on:
		bc("Vücut kamerası kapatıldı")
		if mission != null or stop_target != null:
			G["suspicion"] = clampf(float(G["suspicion"]) + 3.0, 0, 100)
	cam_on = not cam_on
	if cam_on:
		bc("Vücut kamerası açıldı")
	radio("Vücut kamerası " + ("AÇIK" if cam_on else "KAPALI"), "" if cam_on else "warn")

# ---------------------------------------------------------------- girdi
func tap(a: String) -> void:
	if not playing() or M.ui.modal_open:
		return
	match a:
		"act":
			if interact != null:
				(interact["fn"] as Callable).call()
			else:
				radio("Yakında etkileşim yok.")
		"enter":
			M.toggle_enter()
		"siren":
			M.set_siren(not M.siren)
			if M.siren:
				bc("Siren açıldı")
		"lights":
			M.lights_on = not M.lights_on
		"cam":
			toggle_cam()
		"backup":
			call_backup()
		"file":
			open_file()
		"radio":
			radio_menu()
		"view":
			M.cycle_view()
			radio({"tps": "Kamera: 3. şahıs", "fps": "Kamera: 1. şahıs", "top": "Kamera: yukarıdan"}[M.view])

# ---------------------------------------------------------------- etkileşim
func find_interact():
	var best = null
	var bd := 1e9
	var p := pp()
	if state == "interior":
		var lp: Vector2 = M.interior_local()
		for sp in int_spots:
			if sp["done"]:
				continue
			var d = lp.distance_to(Vector2(sp["x"], sp["z"]))
			if d < 1.4 and d < bd:
				bd = d
				var spr: Dictionary = sp
				best = {"label": sp["l"], "fn": func(): _use_spot(spr)}
		var de := lp.distance_to(Vector2(8, 10.2))
		if de < 1.3 and de < bd:
			best = {"label": "Dışarı çık", "fn": exit_interior}
		return best
	if stop_target != null and stop_target.stopped and stop_target.mission == null:
		var d := p.distance_to(stop_target.pos)
		if d < 6.0 and d < bd:
			bd = d
			best = {"label": "Sürücüyle konuş" if M.mode == "foot" else "Araçtan in (kontrol)", "fn": func(): _need_foot(func(): traffic_stop(stop_target, null))}
	if mission != null:
		var r = _m_inter(mission)
		if r != null and float(r["d"]) < float(r.get("max", 5.0)) and float(r["d"]) < bd:
			bd = r["d"]
			best = r
	if M.mode == "foot":
		var dc: float = p.distance_to(M.car_pos())
		if dc < 4.0 and dc + 1.0 < bd:
			bd = dc + 1.0
			best = {"label": "Araca bin", "fn": M.toggle_enter}
	for door in M.city.doors_near(p, 6.0):
		var d: float = (door["p"] as Vector2).distance_to(p)
		if d >= bd:
			continue
		if door["kind"] == "karakol":
			bd = d
			best = {"label": "Karakola gir" if M.mode == "foot" else "Karakol (araçtan in)", "fn": func(): _need_foot(karakol_menu)}
		elif door["kind"] == "shop" and M.mode == "foot" and d < 3.5:
			bd = d
			best = {"label": "Markete gir", "fn": shop_menu}
	if M.mode == "car" and M.city.gas_near(p) and 4.0 < bd:
		best = {"label": "Akaryakıt ve lastik", "fn": gas_menu}
	return best

func _need_foot(f: Callable) -> void:
	if M.mode == "car":
		M.toggle_enter()
		if M.mode == "car":
			return
	f.call()

func shop_menu() -> void:
	modal("🛒 Mahalle Marketi", "Kısa bir mola. Bakiye: $%d" % G["money"], [
		B("Simit + ayran ($5) — açlık -25", func(): _buy(5, func(): G["hunger"] = maxf(0, G["hunger"] - 25))),
		B("Enerji içeceği ($6) — yorgunluk -12", func(): _buy(6, func(): G["fatigue"] = maxf(0, G["fatigue"] - 12); G["stress"] += 2)),
		B("Esnafla sohbet et (bölge bilgisi)", func(): radio("Esnaf: \"" + C.pick(["Gece Eski Şehir'de kavga eksik olmuyor.", "Limandaki depolarda garip kamyonlar var.", "Yeşilvadi'de son zamanlarda çok hırsızlık oluyor.", "Sabah 8'de otoyol felç."]) + "\"")),
		B("Çık")])

func _buy(cost: int, f: Callable) -> void:
	if int(G["money"]) < cost:
		radio("Paran yetmiyor.", "bad")
		return
	G["money"] -= cost
	f.call()

func gas_menu() -> void:
	modal("⛽ Akaryakıt İstasyonu", "Yakıt: %%%d • Lastik: %%%d\nEmniyet kartı ile ödenir, vardiya süresinden harcar." % [roundi(M.fuel), roundi(M.tire)], [
		B("Depoyu doldur (10 dk)", func(): M.fuel = 100.0; G["time"] += 10; radio("Depo dolduruldu.")),
		B("Lastikleri değiştir (20 dk)", func(): M.tire = 100.0; G["time"] += 20; radio("Lastikler yenilendi.")),
		B("Kapat")])

func karakol_menu() -> void:
	var left: float = SH["end_abs"] - now_abs()
	modal("🏢 Merkez Karakolu", "Vardiya sonuna: [b]%s[/b]\nAraç hasarı: %%%d • Yakıt: %%%d" % ["BİTTİ" if left <= 0 else C.fmt_clock(left).replace(":", " sa ") + " dk", roundi(M.dmg), roundi(M.fuel)], [
		B("🔧 Garaj: aracı onar (30 dk)", func(): M.dmg = 0.0; M.tire = 100.0; M.fuel = 100.0; G["time"] += 30; radio("Araç bakımdan çıktı."), "", M.dmg < 1),
		B("☕ Mola ver (20 dk, yorgunluk -10)", func(): G["fatigue"] = maxf(0, G["fatigue"] - 10); G["time"] += 20; G["hunger"] = maxf(0, G["hunger"] - 10)),
		B("📹 Kamera kayıtlarım", func(): show_bodycam(karakol_menu)),
		B("🗂 Görev dosyası", open_file),
		B("✅ Vardiyayı bitir" if left <= 0 else "Vardiyayı erken bitir (performans cezası)", func(): end_shift(left > 0), "good" if left <= 0 else "dark", mission != null),
		B("Kapat")])

# ---------------------------------------------------------------- görev sistemi
const MT := {
	"patrol": {"n": "Rutin Devriye", "pri": 2, "rank": 0, "win": "3 kontrol noktasını süre içinde dolaş"},
	"speed": {"n": "Otoyolda Hız İhlali", "pri": 2, "rank": 0, "win": "Aracı durdur, kontrol et, doğru işlemi yap"},
	"accident": {"n": "Trafik Kazası", "pri": 1, "rank": 0, "win": "Alanı güvene al, ifadeleri al, kusuru doğru tespit et"},
	"burglary": {"n": "Ev/İşyeri Hırsızlığı", "pri": 1, "rank": 0, "win": "Delil topla, doğru şüpheliyi belirle ve yakala"},
	"fight": {"n": "Kavga İhbarı", "pri": 1, "rank": 0, "win": "Kavgayı ayır, başlatanı tespit edip işlem yap"},
	"flee": {"n": "Kaçan Gasp Şüphelisi", "pri": 1, "rank": 1, "win": "Tarife uyan şüpheliyi bul ve yakala"},
	"robbery": {"n": "Silahlı Soygun", "pri": 0, "rank": 1, "win": "Kaçan soyguncuları durdur ve güvenle etkisiz hale getir"},
	"undercover": {"n": "Gizli Operasyon: Takip", "pri": 1, "rank": 2, "win": "Fark edilmeden 3 fotoğraf delil topla"},
	"hostage": {"n": "Rehine Krizi", "pri": 0, "rank": 3, "win": "Müzakereyle veya baskınla rehineleri kurtar"},
	"meet": {"n": "★ Muhbir: Serçe", "pri": 1, "rank": 0, "win": "Muhbirle buluş ve bilgi al"},
	"final": {"n": "★ Son Operasyon: Kuzgun", "pri": 0, "rank": 3, "win": "Kuzgun'u yakala"},
}

func gen_desc() -> String:
	return "%s, %d yaşlarında, %s" % [C.pick(["Erkek", "Kadın", "Erkek"]), randi_range(18, 55), C.pick(["siyah montlu", "gri kapüşonlu", "kot ceketli", "eşofmanlı", "takım elbiseli"])]

func make_mission(type: String, o: Dictionary = {}) -> Dictionary:
	var T: Dictionary = MT[type]
	var m = {"id": randi_range(1000, 9999), "type": type, "n": T["n"], "pri": T["pri"], "ev": [], "st": [], "log": [], "stage": "go", "limit": 0.0, "spawned": false, "story": "", "sdesc": "", "no_timer": false, "rew_mul": 1.0, "p": Vector2.ZERO}
	for k in o.keys():
		m[k] = o[k]
	_m_setup(m)
	m["dist"] = C.district_p(m["p"])
	var crime: float = C.DIST[m["dist"]]["crime"]
	var rb: Array = [[260, 150], [150, 85], [75, 40]][m["pri"]]
	var rm = 1.0 + int(G["rank"]) * 0.12
	var st = 1.6 if m["story"] != "" else 1.0
	m["rew"] = {"m": roundi(rb[0] * rm * (0.8 + crime * 0.25) * st), "xp": roundi(rb[1] * (0.8 + crime * 0.2) * (1.8 if m["story"] != "" else 1.0))}
	m["pen"] = {"m": [120, 70, 30][m["pri"]], "xp": [60, 35, 15][m["pri"]], "d": [2, 1, 1][m["pri"]]}
	if float(m["limit"]) <= 0.0:
		m["limit"] = roundf((m["p"] as Vector2).distance_to(pp()) / 16.0 + [40, 60, 85][m["pri"]])
	m["t"] = m["limit"]
	if m["sdesc"] == "":
		m["sdesc"] = gen_desc()
	return m

func gen_offer() -> Dictionary:
	var so = story_offer()
	if so != null:
		return so
	var pool: Array = []
	var n := night_f()
	var add := func(t: String, w: int):
		if int(G["rank"]) >= int(MT[t]["rank"]):
			for i in w:
				pool.append(t)
	add.call("speed", 5 if G["region"] == "merkez" else 3)
	add.call("patrol", 2)
	add.call("accident", 3 + (3 if M.traffic.traffic_mul() > 1.4 else 0))
	add.call("burglary", 3 + (3 if G["region"] == "konut" else 0))
	add.call("fight", 2 + roundi(n * 4) + (2 if G["region"] == "eski" else 0))
	add.call("flee", 3)
	add.call("robbery", 1 + roundi(n * 2))
	add.call("undercover", 2 + (2 if G["region"] in ["liman", "sanayi"] else 0))
	add.call("hostage", 1)
	return make_mission(C.pick(pool))

func show_offer(m: Dictionary) -> void:
	offer = m
	offer_t = 24.0
	var pc = ["ff5050", "ffb020", "6aa8ff"][m["pri"]]
	var bb = "[color=#%s][b]📻 MERKEZ ANONSU — %s[/b][/color]%s\n[b]Görev #%d • %s[/b]\n📍 %s — %.1f km\n⏱ Süre: %s\n👤 Şüpheli: %s\n✅ Başarı: %s\n❌ Başarısızlık: süre aşımı, kaçış, hatalı işlem\n💰 +$%d • +%d XP    ⚠ -$%d / %d disiplin" % [pc, C.PRI[m["pri"]], "  ★ HİKAYE" if m["story"] != "" else "", m["id"], m["n"], dist_n(m["dist"]), (m["p"] as Vector2).distance_to(pp()) / 1000.0 * 8.0, C.fmt_t(m["limit"]), m["sdesc"], MT[m["type"]]["win"], m["rew"]["m"], m["rew"]["xp"], m["pen"]["m"], m["pen"]["d"]]
	M.ui.show_offer(bb)

func accept_offer() -> void:
	if offer == null:
		return
	mission = offer
	offer = null
	M.ui.hide_offer()
	radio("Görev #%d kabul edildi. Olay yerine intikal et." % mission["id"], "good")
	bc("Görev kabul edildi: #%d %s" % [mission["id"], mission["n"]])

func decline_offer(auto: bool) -> void:
	if offer == null:
		return
	var m: Dictionary = offer
	offer = null
	M.ui.hide_offer()
	SH["declined"] += 1
	radio(("Cevap yok. #%d başka ekibe verildi." % m["id"]) if auto else ("#%d reddedildi. Ekip %d olaya intikal ediyor." % [m["id"], randi_range(2, 40)]), "warn")
	if m["story"] != "":
		SH["story_given"] = false
	if m["pri"] == 0:
		SH["disc"] += 1
	next_offer = 10 + randf() * 10

func upd_offers(dt: float) -> void:
	if offer != null:
		offer_t -= dt
		M.ui.set_offer_timer(offer_t)
		if offer_t <= 0:
			decline_offer(true)
		return
	if mission != null or stop_target != null:
		return
	next_offer -= dt * crime_mul() * (0.8 + float(C.DIST[G["region"]]["crime"]) * 0.25)
	if next_offer <= 0:
		next_offer = 14 + randf() * 16
		show_offer(gen_offer())

func upd_mission(dt: float) -> void:
	var m = mission
	if m == null:
		return
	for e in m["ev"]:
		if float(e.get("pending", 0)) > 0:
			e["pending"] -= dt
			if float(e["pending"]) <= 0:
				e["pending"] = 0
				e["n"] = e["done"]
				radio("Sonuç geldi: " + e["done"], "good")
				bc("Rapor alındı: " + e["done"])
	if not m["no_timer"]:
		m["t"] -= dt
		if float(m["t"]) <= 0:
			fail(m, "Süre doldu")
			return
		if not m.get("esc", false) and float(m["t"]) < float(m["limit"]) * 0.33:
			m["esc"] = true
			radio("Merkez: Olay yerinde durum kötüleşiyor, acele et!", "warn")
	if not m["spawned"] and (m["p"] as Vector2).distance_to(pp()) < 110.0:
		m["spawned"] = true
		_m_spawn(m)
	if not m["spawned"] and m["type"] == "flee":
		m["p"] = (m["p"] as Vector2) + Vector2(randf() - 0.5, randf() - 0.5) * 7.0 * dt
	if mission == m:
		_m_update(m, dt)

func cleanup(m: Dictionary) -> void:
	for c in M.traffic.cars.duplicate():
		if c.mission == m:
			if c.wreck or m.get("remove_cars", false):
				M.traffic.remove_car(c)
			else:
				c.keep = false
				c.mission = null
				c.flee = false
				c.stopping = false
				c.stopped = false
				M.traffic._upd_base(c)
		elif c.backup:
			c.backup = false
			c.keep = false
			M.traffic._upd_base(c)
	for e in M.traffic.peds.duplicate():
		if e.mission == m:
			M.traffic.remove_ped(e)
	for pr in m.get("props", []):
		if is_instance_valid(pr):
			pr.queue_free()
	if stop_target != null and stop_target.mission == m:
		stop_target = null

func success(m: Dictionary, mult: float = 1.0, note: String = "") -> void:
	if mission != m:
		return
	mission = null
	cleanup(m)
	G["stats"]["done"] += 1
	SH["done"] += 1
	M.beep()
	report(m, true, mult * float(m["rew_mul"]), note)

func fail(m: Dictionary, reason: String) -> void:
	if mission != m:
		return
	mission = null
	cleanup(m)
	G["stats"]["fail"] += 1
	SH["fail"] += 1
	SH["fines"] += m["pen"]["m"]
	SH["disc"] += m["pen"]["d"]
	G["xp"] = maxi(0, int(G["xp"]) - int(m["pen"]["xp"]))
	G["stress"] = clampf(float(G["stress"]) + 8, 0, 100)
	radio("Görev #%d BAŞARISIZ: %s" % [m["id"], reason], "bad")
	bc("Görev başarısız: " + reason)
	if m["story"] != "":
		SH["story_given"] = false
	report(m, false, 0.0, reason)

func report(m: Dictionary, ok: bool, mult: float, note: String) -> void:
	var has_flags := false
	for l in m["log"]:
		if (l as String).begins_with("⚠") or (l as String).begins_with("✖"):
			has_flags = true
	var blk = null
	if ok and int(G["story"]["stage"]) >= 1 and m["ev"].size() > 0 and (m["dist"] in ["liman", "sanayi"] or G["story"]["path"] == "corrupt") and randf() < 0.45:
		blk = {"n": m["ev"][0]["n"], "amt": randi_range(4, 10) * 100}
	var body = "[b]Görev:[/b] #%d %s — %s%s\n[b]Konum:[/b] %s • [b]Öncelik:[/b] %s\n\n[b]İfadeler[/b]\n" % [m["id"], m["n"], "[color=#5ee08a]BAŞARILI[/color]" if ok else "[color=#ff6b6b]BAŞARISIZ[/color]", ("\n" + note) if note != "" else "", dist_n(m["dist"]), C.PRI[m["pri"]]]
	if m["st"].is_empty():
		body += "• İfade alınmadı\n"
	for s in m["st"]:
		body += "• " + s + "\n"
	body += "\n[b]Yapılan işlemler (kamera kaydı)[/b]\n"
	for l in m["log"].slice(-8):
		body += "• " + l + "\n"
	if blk != null:
		body += "\n[color=#ffbf47]📱 Bilinmeyen numara: \"Dosyadaki %s kaybolursa %d$ senin. Kimse bilmeyecek.\" — Kuzgun[/color]" % [blk["n"], blk["amt"]]
	var checks: Array = []
	var sum_opt = [null]
	var extra := func(box: VBoxContainer):
		box.add_child(M.ui.mk_label("Deliller (dosyaya eklenecekler):", 22))
		if m["ev"].is_empty():
			box.add_child(M.ui.mk_label("  Delil yok", 20))
		for e in m["ev"]:
			var cb := CheckBox.new()
			cb.text = e["n"] + (" (sonuç bekleniyor)" if float(e.get("pending", 0)) > 0 else "")
			cb.button_pressed = true
			cb.add_theme_font_size_override("font_size", 20)
			box.add_child(cb)
			checks.append(cb)
		box.add_child(M.ui.mk_label("Olay özeti:", 22))
		var ob := OptionButton.new()
		ob.add_theme_font_size_override("font_size", 20)
		ob.add_item("Olayları eksiksiz ve doğru anlat")
		ob.add_item("Kısa ve yüzeysel geç")
		if has_flags:
			ob.add_item("Kendi hatalarını rapora yazma")
		box.add_child(ob)
		sum_opt[0] = ob
	var submit := func(dark: bool):
		var inc := 0
		for cb in checks:
			if (cb as CheckBox).button_pressed:
				inc += 1
		var tot := checks.size()
		var sm: int = (sum_opt[0] as OptionButton).selected if sum_opt[0] != null else 0
		var q = (float(inc) / tot if tot > 0 else 0.7) * 0.5 + (0.2 if m["st"].size() > 0 else 0.1) + [0.3, 0.12, 0.22][sm]
		var lines: Array = []
		if dark:
			corrupt_act("Delil karartma: " + blk["n"], blk["amt"], risk_calc() * 0.6 + 0.1, 3)
			q -= 0.2
			lines.append("[color=#ff6b6b]Delil dosyadan çıkarıldı.[/color]")
		elif blk != null:
			honest("Delil karartma teklifini reddetti", 4)
			lines.append("[color=#5ee08a]Teklifi reddettin ve iç denetime bildirdin. (+dürüstlük)[/color]")
		if sm == 2 and has_flags and randf() < (0.6 if cam_on else 0.3):
			SH["detections"].append({"text": "Rapor tahrifatı (kamera kaydıyla çelişki)", "sev": 1})
			SH["disc"] += 2
			lines.append("[color=#ff6b6b]Rapor kamera kaydıyla çelişiyor! Disiplin işlemi.[/color]")
		if tot > 0 and inc < tot and not dark:
			lines.append("[color=#ffbf47]Eksik delil: savcılık dosyayı zayıf buldu.[/color]")
		if ok:
			var mm = roundi(float(m["rew"]["m"]) * mult * (0.55 + 0.45 * q))
			var xp = roundi(float(m["rew"]["xp"]) * mult * (0.55 + 0.45 * q) * (1.15 if int(G["integrity"]) >= 70 else 1.0))
			SH["earn"] += mm
			SH["xp"] += xp
			G["xp"] += xp
			lines.push_front("Rapor kalitesi: [b]%%%d[/b] → +$%d, +%d XP" % [roundi(q * 100), mm, xp])
		else:
			lines.push_front("Ceza: -$%d, -%d XP, +%d disiplin" % [m["pen"]["m"], m["pen"]["xp"], m["pen"]["d"]])
		modal("📄 Rapor gönderildi", "\n".join(lines), [B("Devriyeye dön", _after_report.bind(m, ok), "pri")])
	var btns = [B("Raporu gönder", func(): submit.call(false), "pri", false, true)]
	if blk != null:
		btns.append(B("Delili karart ve gönder (+$%d) risk %s" % [blk["amt"], mix_pct(risk_calc() * 0.6 + 0.1)], func(): submit.call(true), "dark", false, true))
	modal("📝 Görev Raporu #%d" % m["id"], body, btns, extra)

func _after_report(m: Dictionary, ok: bool) -> void:
	if ok and m["story"] != "":
		story_advance(m)

func start_chase(m: Dictionary, c) -> void:
	c.flee = true
	c.stopping = false
	c.stopped = false
	c.hp = 100.0
	c.keep = true
	c.mission = m
	M.traffic._upd_base(c)
	m["stage"] = "chase"
	m["lost"] = 0.0
	radio("Şüpheli araç kaçıyor! Takibe başla. (TAKVİYE isteyebilirsin)", "bad")
	bc("Araç takibi başladı: " + c.plate)

func chase_update(m: Dictionary, dt: float) -> void:
	var c = m["car"]
	m["p"] = c.pos
	var d: float = c.pos.distance_to(pp())
	if d > 140.0:
		m["lost"] += dt
		if float(m["lost"]) > 12:
			fail(m, "Şüpheli araç gözden kayboldu")
			return
	else:
		m["lost"] = maxf(0.0, float(m["lost"]) - dt)
	if d < 7.0 and c.v < 8.0:
		m["box"] = float(m.get("box", 0.0)) + dt
	else:
		m["box"] = maxf(0.0, float(m.get("box", 0.0)) - dt * 0.5)
	if c.hp <= 0 or float(m["box"]) > 2.2:
		c.flee = false
		c.wreck = true
		c.v = 0.0
		radio("Şüpheli araç durdu! Şüpheli araçtan iniyor!", "warn")
		bc("Kaçan araç durduruldu")
		m["ev"].append({"n": "Kaçış aracı (" + c.plate + ")"})
		var s = M.traffic.make_ped("suspect", c.pos + Vector2(2.2, 1.5), Color(0.1, 0.1, 0.1) if m.get("armed", false) else Color(0.45, 0.32, 0.2))
		s.keep = true
		s.mission = m
		s.armed = m.get("armed", false)
		s.ai = "flee" if randf() < (0.5 if s.armed else 0.55) else "idle"
		m["sus"] = s
		m["stage"] = "foot"
		if s.ai == "flee":
			bc("Şüpheli yaya olarak kaçıyor")

func foot_stage(m: Dictionary, dt: float) -> void:
	var s = m["sus"]
	if s == null:
		return
	m["p"] = s.pos
	var d: float = s.pos.distance_to(pp())
	if s.ai == "flee" and d > 120.0:
		m["lost"] = float(m.get("lost", 0.0)) + dt
		if float(m["lost"]) > 10:
			fail(m, "Şüpheli gözden kayboldu")
			return
	else:
		m["lost"] = 0.0
	if s.ai == "idle" and d < 11.0 and not s.warned:
		s.warned = true
		if randf() < 0.3:
			s.ai = "flee"
			radio("Şüpheli kaçıyor!", "warn")
	if M.mode == "foot" and d < 1.7 and s.ai != "held" and not M.ui.modal_open:
		arrest_dialog(s, m)

func team_bonus(tr: String) -> float:
	var b := 0.0
	for t in G["team"]:
		if t["trait"] == tr and t["assign"] == "takviye":
			b = maxf(b, float(t["skill"]) / 100.0)
	return b

func call_backup() -> void:
	if mission == null or not (mission["stage"] in ["chase", "foot", "find"]):
		radio("Takviye için aktif bir takip yok.")
		return
	if backup_cd > 0:
		radio("Takviye zaten yolda.")
		return
	backup_cd = 45.0
	var mem: Array = G["team"].filter(func(t): return t["assign"] == "takviye")
	var n := mini(2, mem.size()) if mem.size() > 0 else 1
	for i in n:
		var c = M.traffic.spawn_car("cop", 45, 70)
		if c != null:
			c.backup = true
			c.keep = true
			c.skill = float(mem[i]["skill"]) if i < mem.size() else 50.0
			M.traffic._upd_base(c)
	mission["backup"] = true
	mission["rew_mul"] = float(mission["rew_mul"]) * 0.9
	radio((mem[0]["name"] + " takviyeye geliyor!") if mem.size() > 0 else "Merkez: En yakın ekip takviyeye yönlendirildi.", "good")
	bc("Takviye talep edildi")

func hit_ped(e) -> void:
	e.down = true
	e.down_t = 25.0
	M.shake = 0.8
	if e.kind == "suspect":
		flag("Şüpheliye araçla çarpma", 5, 0.85)
		e.ai = "held"
		e.state = "comply"
		radio("Şüpheli araç çarpması sonucu yaralandı. Ambulans çağrıldı.", "bad")
	else:
		flag("Yayaya çarpma — yaralı vatandaş", 6, 1.0)
		radio("Merkez: Yaya yaralandı! Ambulans yönlendirildi. İç denetime bildirildi.", "bad")
		if SH:
			SH["peds"] += 1

func on_car_hit(c, imp: float) -> void:
	if c.flee:
		c.hp -= imp * 4.5
		bc("Kaçan araca müdahale (çarpma)")
	elif c.kind == "civ" and imp > 3.0:
		c.angry = 3.0
		if imp > 10.0 and not (mission != null and mission["stage"] == "chase"):
			flag("Sivil araca çarpma (kaza)", 1, 0.35)
	elif c.kind == "cop" and imp > 10.0:
		flag("Ekip aracına çarpma", 1, 0.2)

# ---------------------------------------------------------------- görev türleri
func _m_setup(m: Dictionary) -> void:
	var p := pp()
	match m["type"]:
		"patrol":
			m["cps"] = [M.city.road_point(p, G["region"], 30, 300)["p"], M.city.road_point(p, G["region"], 30, 300)["p"], M.city.road_point(p, G["region"], 30, 300)["p"]]
			m["ci"] = 0
			m["p"] = m["cps"][0]
			m["sdesc"] = "Yok — görünürlük devriyesi"
			m["limit"] = 170.0
		"speed":
			m["p"] = M.city.hw_point(p)
			m["kmh"] = randi_range(148, 178)
			m["col"] = C.pick(C.COLORS)
			m["pl"] = C.plate()
			m["sdesc"] = "%s araç, plaka %s, %d km/h" % [m["col"][0], m["pl"], m["kmh"]]
		"accident":
			if not m.has("given"):
				var rp: Dictionary = M.city.road_point(p, "", 40, 280)
				m["p"] = rp["p"]
				m["dir"] = rp["dir"]
			m["guilty"] = randi() % 2
			m["D"] = {}
			m["drv"] = [C.rname(), C.rname()]
			m["sdesc"] = "İki araç çarpıştı, 1 hafif yaralı"
		"burglary":
			var d: Dictionary = M.city.find_door("house", G["region"], p)
			if d.is_empty():
				d = M.city.find_door("shop", "", p)
			if d.is_empty():
				d = {"p": p + Vector2(40, 0), "kind": "house", "out": 1}
			m["door"] = d
			m["p"] = d["p"]
			m["limit"] = roundf((d["p"] as Vector2).distance_to(p) / 16.0 + 190)
			var hs = ["uzun boylu", "kısa boylu", "orta boylu"]
			var gh: String = C.pick(hs)
			var gc: Array = C.pick(C.COLORS)
			var oh: String = C.pick(hs.filter(func(h): return h != gh))
			var oc: Array = C.pick(C.COLORS.filter(func(c): return c != gc))
			var g = {"name": C.rname(), "h": gh, "car": gc[0], "pl": C.plate(), "rec": "Sabıkalı (hırsızlık)"}
			var d1 = {"name": C.rname(), "h": gh, "car": oc[0], "pl": C.plate(), "rec": "Sabıkalı (gasp)"}
			var d2 = {"name": C.rname(), "h": oh, "car": gc[0], "pl": C.plate(), "rec": C.pick(["Temiz", "Sabıkalı (dolandırıcılık)"])}
			var lst = [g, d1, d2]
			lst.shuffle()
			m["list"] = lst
			m["g"] = g
			m["sdesc"] = "Bilinmiyor — olay yerinde delil toplanmalı"
			m["phone"] = 0
		"fight":
			m["p"] = M.city.side_point(p, G["region"])
			m["inst"] = randi() % 2
			m["nm"] = [C.rname(), C.rname()]
			m["cl"] = [["kırmızı tişörtlü", Color(0.75, 0.15, 0.12)], ["sarı ceketli", Color(0.85, 0.72, 0.15)]]
			m["sdesc"] = "İki şahıs: kırmızı tişörtlü ve sarı ceketli"
			m["F"] = {}
		"flee":
			m["p"] = M.city.side_point(p, G["region"])
			m["c"] = C.pick([["kırmızı", Color(0.85, 0.15, 0.12)], ["sarı", Color(0.95, 0.78, 0.12)], ["yeşil", Color(0.15, 0.7, 0.3)], ["turuncu", Color(1, 0.5, 0.1)]])
			m["sdesc"] = "Erkek, 20'li yaşlar, %s montlu, çanta gasbı" % m["c"][0]
		"robbery":
			var rp2: Dictionary = M.city.road_point(p, G["region"], 50)
			m["p"] = rp2["p"]
			m["dir"] = rp2["dir"]
			m["armed"] = true
			m["sdesc"] = "İki maskeli şahıs, bıçaklı. Siyah SUV."
		"undercover":
			m["p"] = M.city.side_point(p, C.pick(["liman", "sanayi"]), 60, 380)
			m["sus_v"] = 0.0
			m["ph"] = 0
			m["cd"] = 0.0
			m["sdesc"] = "Şüpheli alışveriş: hedef ve bağlantısı"
			m["limit"] = roundf((m["p"] as Vector2).distance_to(p) / 16.0 + 150)
		"hostage":
			var d3: Dictionary = M.city.find_door("shop", "", p)
			if d3.is_empty():
				d3 = M.city.find_door("house", "", p)
			if d3.is_empty():
				d3 = {"p": p + Vector2(30, 0), "kind": "shop", "out": 1}
			m["door"] = d3
			m["p"] = d3["p"]
			m["sdesc"] = "Silahlı tek şahıs, 3 rehine (market)"
		"meet":
			m["p"] = M.city.side_point(p, "liman", 50, 500)
			m["sdesc"] = "Kod adı \"Serçe\". Liman."
			m["limit"] = roundf((m["p"] as Vector2).distance_to(p) / 16.0 + 120)
		"final":
			var rp3: Dictionary = M.city.road_point(p, "liman", 60)
			m["p"] = rp3["p"]
			m["dir"] = rp3["dir"]
			m["armed"] = true
			m["sdesc"] = "Demir \"Kuzgun\" Aksoy — siyah zırhlı SUV"
			m["limit"] = roundf((m["p"] as Vector2).distance_to(p) / 16.0 + 150)

func _m_spawn(m: Dictionary) -> void:
	var T = M.traffic
	match m["type"]:
		"speed":
			var dir = 0 if (m["p"] as Vector2).y > 0 else 2
			var c = T.make_car("civ", m["p"], dir, Color(m["col"][1]))
			c.speeder = true
			c.sbase = float(m["kmh"]) / C.KMH
			c.plate = m["pl"]
			c.keep = true
			c.mission = m
			T._upd_base(c)
			c.v = c.base
			c.drv = gen_driver(c, {"speed": m["kmh"], "limit": 120})
			m["car"] = c
		"accident":
			var dv: Vector2 = C.DV[int(m.get("dir", 0))]
			for i in 2:
				var c2 = T.make_car("civ", (m["p"] as Vector2) + dv * (2.6 if i == 1 else -2.6), int(m.get("dir", 0)))
				c2.wreck = true
				c2.v = 0.0
				c2.yaw += 0.5 if i == 0 else 2.2
				c2.mission = m
				c2.keep = true
			for i in 2:
				var e = T.make_ped("civ", (m["p"] as Vector2) + Vector2(4.0 * (1 if i else -1), 5.0))
				e.mission = m
				e.keep = true
		"fight":
			m["f"] = []
			for i in 2:
				var e2 = T.make_ped("fighter", m["p"], m["cl"][i][1])
				e2.keep = true
				e2.mission = m
				e2.ai = "fight"
				e2.center = m["p"]
				e2.side = 0.9 if i else -0.9
				m["f"].append(e2)
			for i in 4:
				var w = T.make_ped("civ", (m["p"] as Vector2) + Vector2(randf_range(-6, 6), randf_range(-5, 5)))
				w.keep = true
				w.mission = m
		"flee":
			var s = T.make_ped("suspect", m["p"], m["c"][1])
			s.keep = true
			s.mission = m
			s.ai = "idle"
			m["sus"] = s
			m["stage"] = "find"
		"robbery", "final":
			var c3 = T.make_car("civ", m["p"], int(m.get("dir", 0)), Color(0.05, 0.05, 0.06))
			c3.keep = true
			c3.mission = m
			c3.stopped = true
			c3.v = 0.0
			m["car"] = c3
		"undercover":
			m["tg"] = []
			for i in 2:
				var t = T.make_ped("target", (m["p"] as Vector2) + Vector2(1.3 * (1 if i else -1), 0), Color(0.15, 0.15, 0.17) if i else Color(0.35, 0.24, 0.48))
				t.keep = true
				t.mission = m
				t.face = Vector2(-1 if i else 1, 0)
				m["tg"].append(t)
		"meet":
			var inf = T.make_ped("informant", m["p"], Color(0.33, 0.42, 0.18))
			inf.keep = true
			inf.mission = m

func _m_update(m: Dictionary, dt: float) -> void:
	var p := pp()
	match m["type"]:
		"patrol":
			var c: Vector2 = m["cps"][m["ci"]]
			if p.distance_to(c) < 8.0:
				m["ci"] += 1
				radio("Kontrol noktası %d/3 tamam." % m["ci"])
				bc("Devriye noktası kontrol edildi")
				if int(m["ci"]) >= 3:
					success(m)
					return
				m["p"] = m["cps"][m["ci"]]
		"speed":
			var c = m.get("car")
			if c == null:
				return
			if m["stage"] in ["go", "stop"]:
				m["p"] = c.pos
			if m["stage"] == "go":
				if M.mode == "car" and M.siren and c.pos.distance_to(p) < 17.0:
					m["pull"] = float(m.get("pull", 0.0)) + dt
					if float(m["pull"]) > 1.2:
						if randf() < 0.22 + int(G["rank"]) * 0.02:
							start_chase(m, c)
						else:
							c.stopping = true
							m["stage"] = "stop"
							stop_target = c
							radio("Araç sağa çekiyor.")
							bc("Araç durduruldu: " + c.plate)
				else:
					m["pull"] = 0.0
			elif m["stage"] == "chase":
				chase_update(m, dt)
			elif m["stage"] == "foot":
				foot_stage(m, dt)
		"burglary":
			if m["stage"] == "arrest":
				if m.get("sus") == null and p.distance_to(m["p"]) < 75.0:
					var s = M.traffic.make_ped("suspect", (m["p"] as Vector2) + Vector2(0, 1.0 * float(m["door"]["out"])), Color(0.25, 0.25, 0.27))
					s.keep = true
					s.mission = m
					m["sus"] = s
					m["stage"] = "foot"
			elif m["stage"] == "foot":
				foot_stage(m, dt)
		"flee":
			if m["stage"] == "find":
				var s2 = m["sus"]
				m["p"] = s2.pos
				if s2.pos.distance_to(p) < 14.0:
					s2.ai = "flee"
					m["stage"] = "foot"
					radio("Şüpheli seni fark etti, kaçıyor!", "bad")
					bc("Yaya takibi başladı")
			elif m["stage"] == "foot":
				foot_stage(m, dt)
		"robbery":
			var c4 = m.get("car")
			if c4 == null:
				return
			if m["stage"] == "go":
				m["p"] = c4.pos
				if c4.pos.distance_to(p) < 40.0:
					start_chase(m, c4)
			elif m["stage"] == "chase":
				chase_update(m, dt)
			elif m["stage"] == "foot":
				foot_stage(m, dt)
		"final":
			var c5 = m.get("car")
			if c5 == null:
				return
			if m["stage"] == "go":
				m["p"] = c5.pos
				if c5.pos.distance_to(p) < 38.0:
					if G["story"]["path"] == "corrupt" and not m.get("asked", false):
						m["asked"] = true
						final_choice(m)
					else:
						start_chase(m, c5)
						c5.hp = 150.0
			elif m["stage"] == "chase":
				chase_update(m, dt)
			elif m["stage"] == "foot":
				foot_stage(m, dt)
		"undercover":
			if m.get("tg") == null:
				return
			var d = p.distance_to(m["p"])
			m["cd"] = float(m["cd"]) - dt
			var marked: bool = M.mode == "car" and not C.VEH[M.car_type].get("civil", false)
			var outfit = 1.6 if (M.mode == "foot" and G["outfit"] != "civil") else 1.0
			if d < 26.0:
				if M.siren:
					m["sus_v"] = 100.0
				elif d < 8.0:
					m["sus_v"] = float(m["sus_v"]) + dt * 40.0 * outfit
				else:
					m["sus_v"] = float(m["sus_v"]) + dt * (9.0 if marked else 1.2) * outfit
			else:
				m["sus_v"] = maxf(0.0, float(m["sus_v"]) - dt * 4.0)
			if float(m["sus_v"]) >= 100:
				fail(m, "Kimliğin deşifre oldu, hedef kaçtı")

func _m_inter(m: Dictionary):
	var p := pp()
	match m["type"]:
		"speed":
			var c = m.get("car")
			if m["stage"] == "stop" and c != null and c.stopped:
				return {"d": p.distance_to(c.pos), "label": "Sürücüyle konuş" if M.mode == "foot" else "Araçtan in (kontrol)", "fn": func(): _need_foot(func(): traffic_stop(c, m)), "max": 7.0}
		"accident":
			return {"d": p.distance_to(m["p"]), "label": "Kaza yerini incele", "fn": func(): _need_foot(func(): accident_dialog(m)), "max": 10.0}
		"burglary":
			if m["stage"] in ["go", "inv"]:
				return {"d": p.distance_to(m["door"]["p"]), "label": "Olay yerine gir" if M.mode == "foot" else "Araçtan in (olay yeri)", "fn": func(): _need_foot(func(): enter_scene(m)), "max": 8.0}
		"fight":
			return {"d": p.distance_to(m["p"]), "label": "Kavgaya müdahale et", "fn": func(): _need_foot(func(): fight_dialog(m)), "max": 10.0}
		"hostage":
			return {"d": p.distance_to(m["p"]), "label": "Müzakereye başla", "fn": func(): _need_foot(func(): _start_neg(m)), "max": 10.0}
		"meet":
			return {"d": p.distance_to(m["p"]), "label": "Muhbirle konuş" if M.mode == "foot" else "Araçtan in (muhbir)", "fn": func(): _need_foot(func(): meet_dialog(m)), "max": 8.0}
		"undercover":
			var d = p.distance_to(m["p"])
			if m.get("tg") != null and d > 8.0 and d < 26.0 and float(m["cd"]) <= 0:
				return {"d": 1.0, "label": "📷 Fotoğraf çek (%d/3)" % m["ph"], "fn": func(): _photo(m), "max": 30.0}
	return null

func _photo(m: Dictionary) -> void:
	m["cd"] = 2.0
	m["ph"] += 1
	m["ev"].append({"n": "Gizli takip fotoğrafı #%d" % m["ph"]})
	bc("Gizli fotoğraf çekildi")
	if int(m["ph"]) >= 3:
		m["st"].append("Muhbir notu: teslimat haftaya, Liman 7. depo")
		success(m, 1.0, "Operasyon delilleri toplandı")

func obj_text(m: Dictionary) -> String:
	match m["type"]:
		"patrol": return "Kontrol noktası %d/3" % (int(m["ci"]) + 1)
		"speed": return {"go": "Aracı bul, SİREN aç ve yakın takip et", "stop": "Araç durdu — araçtan in, sürücüyle konuş", "chase": "Kaçan aracı durdur (çarp / sıkıştır)", "foot": "Şüpheliyi yaya olarak yakala"}.get(m["stage"], "")
		"accident": return "Çekici çağır ve tutanağı kapat" if m["D"].has("dec") else ("İfadeleri al, delil topla, kusuru belirle" if m["D"].has("secure") else "Olay yerine git ve alanı güvene al")
		"burglary": return {"go": "Olay yerine git ve içeri gir", "inv": "DOSYA: delilleri incele, şüpheliyi suçla", "arrest": "Şüpheliyi adresinde yakala", "foot": "Şüpheliyi yakala"}.get(m["stage"], "")
		"fight": return "Tanıkları dinle ve işlem yap" if m["F"].has("sep") else "Olay yerine git, kavgayı ayır"
		"flee": return ("Tarife uyan şahsı bul (%s mont)" % m["c"][0]) if m["stage"] == "find" else "Şüpheliyi yakala (yaya, yaklaş)"
		"robbery": return {"go": "Soygun yerine git", "chase": "SUV'yi durdur — TAKVİYE iste!", "foot": "Silahlı şüpheliyi etkisiz hale getir"}.get(m["stage"], "")
		"undercover": return "Mesafeyi koru (8-25 m) • Şüphe: %%%d" % roundi(m["sus_v"])
		"hostage": return "Olay yerine git ve müzakereye başla"
		"meet": return "Limandaki buluşma noktasına git (yaya konuş)"
		"final": return {"go": "Kuzgun'un konvoyuna yaklaş", "chase": "Zırhlı SUV'yi durdur! TAKVİYE iste", "foot": "Kuzgun'u yakala"}.get(m["stage"], "")
	return ""

# ---------------------------------------------------------------- trafik kontrolü
func gen_driver(c, o: Dictionary = {}) -> Dictionary:
	var d = {"name": C.rname(), "plate": c.plate, "lic": "Geçerli" if randf() < 0.82 else ("Süresi dolmuş" if randf() < 0.5 else "Yok"), "drunk": randf() < (0.24 if night_f() > 0.5 else 0.07), "stolen": randf() < 0.06, "contra": randf() < 0.1, "speed": o.get("speed", roundi(c.base * C.KMH) if c.speeder else 0), "limit": o.get("limit", 120 if absf(c.pos.y) < C.HW * 0.5 + 0.5 else 50)}
	d["smell"] = d["drunk"] and randf() < 0.7
	d["bribe"] = randi_range(3, 12) * 25 if randf() < 0.45 else 0
	return d

func _has_cause(d: Dictionary) -> bool:
	return d["smell"] or (d["drunk"] and d.get("alc", false)) or (d["stolen"] and d.get("doc", false))

func traffic_stop(c, m) -> void:
	if c.drv == null:
		c.drv = gen_driver(c)
	var d: Dictionary = c.drv
	if not d.get("started", false):
		d["started"] = true
		bc("Trafik kontrolü: %s — %s" % [d["plate"], d["name"]])
	_ts_chk(d)
	var info = "[b]Sürücü:[/b] %s\n[b]Plaka:[/b] %s\n" % [d["name"], d["plate"]]
	if int(d["speed"]) > 0:
		info += "[b]Radar:[/b] %s%d km/h%s (limit %d)\n" % ["[color=#ff6b6b]" if int(d["speed"]) > int(d["limit"]) else "", d["speed"], "[/color]" if int(d["speed"]) > int(d["limit"]) else "", d["limit"]]
	else:
		info += "[b]Radar:[/b] ölçüm yok\n"
	if d.get("doc", false):
		info += "[b]Ehliyet:[/b] %s\n[b]Ruhsat/kayıt:[/b] %s\n" % [d["lic"], "[color=#ff6b6b]ÇALINTI ARAÇ KAYDI![/color]" if d["stolen"] else "Temiz"]
	if d.get("alc", false):
		info += "[b]Alkolmetre:[/b] %s\n" % ("[color=#ff6b6b]1.24 promil[/color]" if d["drunk"] else "0.00 promil")
	if d.get("srch", false):
		info += "[b]Arama:[/b] %s\n" % ("[color=#ff6b6b]Bagajda kaçak sigara ve ruhsatsız bıçak[/color]" if d["contra"] else "Bir şey bulunamadı")
	if d["smell"]:
		info += "[i]Araçtan yoğun alkol kokusu geliyor.[/i]\n"
	if d.get("bshown", false) and not d.get("brefused", false):
		info += "\n[color=#ffbf47]Sürücü alçak sesle: \"Memur bey, şu %d$ sizde kalsın, kimse görmedi...\"[/color]" % d["bribe"]
	var bt: Array = []
	if not d.get("doc", false):
		bt.append(B("🪪 Ehliyet ve ruhsat iste", func(): _ts_doc(c, m)))
	if not d.get("alc", false):
		bt.append(B("🍺 Alkol testi", func(): _ts_alc(c, m)))
	if not d.get("srch", false):
		bt.append(B("🔍 Aracı ara" + ("" if _has_cause(d) else " (gerekçe yok!)"), func(): _ts_search(c, m)))
	bt.append(B("🧾 Ceza yaz", func(): _ts_end(c, m, "fine")))
	bt.append(B("🗣 Sözlü uyarı", func(): _ts_end(c, m, "warn")))
	bt.append(B("🚛 Araca el koy (çekici)", func(): _ts_end(c, m, "impound")))
	bt.append(B("🔗 Gözaltına al", func(): _ts_end(c, m, "arrest")))
	if d.get("bshown", false) and not d.get("brefused", false):
		var rk := risk_calc()
		bt.append(B("💵 Rüşveti al ve bırak (+$%d) risk %s" % [d["bribe"], mix_pct(rk)], func(): _ts_bribe(c, m, rk), "dark"))
		bt.append(B("✋ Rüşveti reddet, tutanağa geçir", func(): _ts_refuse(c, m), "good"))
	bt.append(B("Serbest bırak", func(): _ts_end(c, m, "release")))
	modal("🚓 Trafik Kontrolü", info, bt)

func _ts_chk(d: Dictionary) -> void:
	if not d.get("bshown", false) and int(d["bribe"]) > 0 and ((d["drunk"] and d.get("alc", false)) or (d["stolen"] and d.get("doc", false)) or (d["lic"] != "Geçerli" and d.get("doc", false)) or int(d["speed"]) > int(d["limit"])):
		d["bshown"] = true

func _ts_doc(c, m) -> void:
	c.drv["doc"] = true
	bc("Belge kontrolü yapıldı" + (" — çalıntı kaydı" if c.drv["stolen"] else ""))
	_ts_chk(c.drv)
	traffic_stop(c, m)

func _ts_alc(c, m) -> void:
	c.drv["alc"] = true
	bc("Alkol testi" + (" — POZİTİF" if c.drv["drunk"] else " — negatif"))
	_ts_chk(c.drv)
	traffic_stop(c, m)

func _ts_search(c, m) -> void:
	var d: Dictionary = c.drv
	d["srch"] = true
	if not _has_cause(d):
		flag("Gerekçesiz araç araması", 2, 0.5)
	else:
		bc("Araç araması (gerekçeli)")
	if d["contra"] and m != null:
		m["ev"].append({"n": "Araçta ele geçirilen kaçak malzeme"})
	traffic_stop(c, m)

func _ts_bribe(c, m, rk: float) -> void:
	corrupt_act("Rüşvet alıp serbest bırakma (%s)" % c.drv["plate"], c.drv["bribe"], rk, 2)
	_ts_end(c, m, "bribe")

func _ts_refuse(c, m) -> void:
	c.drv["brefused"] = true
	honest("Rüşvet teklifini reddetti", 4)
	traffic_stop(c, m)

func _ts_end(c, m, kind: String) -> void:
	var d: Dictionary = c.drv
	var kn = {"speed": int(d["speed"]) > int(d["limit"]), "lic": d.get("doc", false) and d["lic"] != "Geçerli", "drunk": d.get("alc", false) and d["drunk"], "stolen": d.get("doc", false) and d["stolen"], "contra": d.get("srch", false) and d["contra"]}
	var any := false
	for v in kn.values():
		if v:
			any = true
	var serious: bool = kn["drunk"] or kn["stolen"] or kn["contra"]
	var score := 0.0
	var txt := ""
	match kind:
		"fine":
			if any:
				score = 1.0
				txt = "Ceza tutanağı düzenlendi."
				bc("Trafik cezası yazıldı")
			else:
				flag("Gerekçesiz ceza", 1, 0.4)
				txt = "İhlal tespit edilmeden ceza yazdın!"
			if serious:
				score = 0.5
				txt += " Ancak ciddi suç için sadece ceza yetersiz."
		"warn":
			score = 0.0 if serious else (0.6 if any else 0.8)
			txt = "Ciddi suçu uyarıyla geçiştirdin." if serious else "Sürücü uyarıldı."
			bc("Sözlü uyarı")
			if serious:
				SH["disc"] += 1
		"impound":
			if (kn["lic"] and d["lic"] == "Yok") or kn["drunk"] or kn["stolen"]:
				score = 1.2
				txt = "Araç yasal prosedürle otoparka çekildi."
				bc("Araca el konuldu (yasal)")
			else:
				flag("Hukuka aykırı el koyma", 3, 0.75)
				txt = "Yasal dayanak olmadan araca el koydun!"
		"arrest":
			if serious:
				score = 1.4
				txt = "Şüpheli gözaltına alındı."
				bc("Sürücü gözaltına alındı")
				G["stats"]["arrests"] += 1
			else:
				flag("Haksız gözaltı", 3, 0.8)
				txt = "Gözaltı için yeterli sebep yoktu!"
		"release":
			score = 0.0 if serious else (0.3 if any else 0.6)
			txt = "Ciddi suç işleyen sürücüyü bilerek serbest bıraktın." if serious else "Sürücü serbest bırakıldı."
			if serious:
				G["suspicion"] = clampf(float(G["suspicion"]) + 5, 0, 100)
				bc("⚠ Şüpheliyi bilerek serbest bırakma", true)
				SH["disc"] += 1
		"bribe":
			txt = "Para cebinde. Sürücü gülümseyerek uzaklaştı."
	if kind == "impound" or kind == "arrest":
		M.traffic.remove_car(c)
	else:
		c.stopped = false
		c.stopping = false
		c.keep = false
		c.mission = null
	if stop_target == c:
		stop_target = null
	if m != null:
		if score > 0.0:
			success(m, minf(1.3, score), txt)
		else:
			fail(m, txt if txt != "" else "Uygunsuz işlem")
	else:
		SH["stops"] += 1
		G["stats"]["stops"] += 1
		var mm = roundi(25 * score * (1 + int(G["rank"]) * 0.1))
		var xp := roundi(20 * score)
		SH["earn"] += mm
		SH["xp"] += xp
		G["xp"] += xp
		radio("Proaktif kontrol: %s %s" % [txt, ("(+$%d, +%d XP)" % [mm, xp]) if score > 0 else ""], "good" if score > 0 else "warn")

# ---------------------------------------------------------------- yakalama
func arrest_dialog(s, m: Dictionary) -> void:
	s.ai = "held"
	var first: bool = s.state == ""
	if first:
		s.state = "armed" if s.armed else ("resist" if randf() < 0.35 else "comply")
		bc("Şüpheliye yetişildi")
	var desc: String = {"armed": "Şüphelinin elinde bıçak var! Geri çekilmiyor.", "resist": "Şüpheli kollarını savuruyor, kelepçeye direniyor.", "comply": "Şüpheli ellerini kaldırdı, teslim oluyor."}[s.state]
	var bb = 0.2 if m.get("backup", false) else 0.0
	var bt: Array = []
	var again := func(): arrest_dialog(s, m)
	var arrested := func():
		bc("Şüpheli kelepçelendi")
		G["stats"]["arrests"] += 1
		var done := func(): success(m, 1.0, "Şüpheli yakalandı")
		modal("🔗 Şüpheli kelepçelendi", "Şüpheli ekip otosuna alınacak. Haklarını okudun.", [
			B("🔍 Üst araması yap", func():
				bc("Üst araması (yakalama sonrası)")
				var f: String = C.pick(["Çalıntı cüzdan", "Uyuşturucu paketçikleri", "Sahte kimlik", "Kilit açma aletleri", "Bir anahtar ve not: \"Liman 7. depo\""])
				m["ev"].append({"n": "Üst aramasında bulunan: " + f})
				radio("Aramada bulundu: " + f)
				done.call()),
			B("Ekip otosuna al", done, "good")])
	if s.state != "comply":
		bt.append(B("🗣 \"Yere yat, ellerini göster!\"", func():
			var pr = (0.5 if s.state == "armed" else 0.55) + bb + team_bonus("Müzakereci") * 0.2
			bc("Sözlü talimat verildi")
			if randf() < pr:
				s.state = "comply"
				radio("Şüpheli talimata uydu.")
			else:
				radio("Şüpheli talimata uymuyor!", "warn")
			again.call()))
	bt.append(B("🔗 Kelepçele", func():
		if s.state == "comply":
			arrested.call()
		elif s.state == "resist":
			radio("Şüpheli kurtulup kaçtı!", "bad")
			bc("Şüpheli kelepçe sırasında kaçtı")
			s.ai = "flee"
			s.stam = 7.0
		else:
			G["hp"] = maxf(5.0, float(G["hp"]) - 22.0)
			radio("Bıçaklı şüpheliye korumasız yaklaştın, yaralandın!", "bad")
			bc("Memur yaralandı")
			s.ai = "flee"
			s.stam = 8.0))
	bt.append(B("✋ Kontrollü güç (bilek kilidi)", func():
		if s.state == "comply":
			flag("Teslim olan şüpheliye güç kullanımı", 3, 0.7)
		elif s.state == "resist":
			just_force("Direnen şüpheliye bilek kilidi")
		else:
			if randf() < 0.5:
				just_force("Silahlı şüpheliyi etkisiz hale getirme")
			else:
				G["hp"] = maxf(5.0, float(G["hp"]) - 12.0)
				radio("Boğuşmada yaralandın.", "bad")
				again.call()
				return
		s.state = "comply"
		again.call()))
	bt.append(B("⚡ Elektroşok (taser)", func():
		if s.state == "armed":
			just_force("Silahlı şüpheliye taser")
		elif s.state == "resist":
			bc("Direnen şüpheliye taser")
			if randf() < 0.25:
				G["complaints"].append({"text": "Taser kullanımı şikâyeti", "cam": cam_on, "d": 2, "day": G["day"], "just": true})
		else:
			flag("Teslim olan şüpheliye taser", 4, 0.85)
		s.state = "comply"
		again.call()))
	bt.append(B("👊 Aşırı güç uygula", func(): flag("Aşırı güç kullanımı", 5, 0.9); s.state = "comply"; again.call(), "dark"))
	if s.bribe < 0:
		s.bribe = randi_range(4, 16) * 50 if randf() < 0.3 else 0
	if s.bribe > 0 and m["type"] != "final":
		var rk := risk_calc()
		bt.append(B("💵 \"%d$ vereyim, bırak beni\" — kabul et (risk %s)" % [s.bribe, mix_pct(rk)], func(): corrupt_act("Rüşvet karşılığı şüpheliyi serbest bırakma", s.bribe, rk, 3); fail(m, "Şüpheli \"kaçtı\" (bilerek serbest bırakıldı)"), "dark"))
	bt.append(B("Serbest bırak", func(): bc("⚠ Şüpheli serbest bırakıldı", true); G["suspicion"] = clampf(float(G["suspicion"]) + 4, 0, 100); fail(m, "Şüpheli serbest bırakıldı")))
	modal("🚨 Şüpheli Müdahalesi", "%s\n[font_size=18]Kamera: %s • Takviye: %s[/font_size]" % [desc, "[color=#ff6b6b]● KAYITTA[/color]" if cam_on else "kapalı", "var" if m.get("backup", false) else "yok"], bt)

# ---------------------------------------------------------------- kaza
func accident_dialog(m: Dictionary) -> void:
	var D: Dictionary = m["D"]
	var g: int = m["guilty"]
	var AB = ["A", "B"]
	var body = "Sürücü A: %s\nSürücü B: %s\n" % [m["drv"][0], m["drv"][1]]
	if m["st"].size() > 0:
		body += "\n[b]İfadeler[/b]\n"
		for s in m["st"]:
			body += "• " + s + "\n"
	if m["ev"].size() > 0:
		body += "\n[b]Deliller[/b]\n"
		for e in m["ev"]:
			body += "• " + (e["n"] + (" (%d sn)" % ceili(e["pending"])) if float(e.get("pending", 0)) > 0 else e["n"]) + "\n"
	if D.has("photo"):
		body += "\n📷 Fren izi analizi: [b]%s[/b] aracı hatalı görünüyor (güvenilirlik ~%%70)." % AB[m["ph"]]
	var stt := func(i: int) -> String:
		return C.pick(["\"Yeşil yanıyordu bence... O çok hızlı geliyordu!\"", "\"Telefona bakmıyordum, yemin ederim. O çıktı önüme.\""]) if i == g else C.pick(["\"Ben yeşildeydim, o kırmızıda daldı!\"", "\"Sinyal verdim, yavaşladım, arkadan vurdu!\""])
	var re := func(): accident_dialog(m)
	var dec := func(i: int, bribed: bool):
		D["dec"] = true
		D["ok"] = i == g
		bc("Kusur tespiti: Sürücü " + AB[i])
		if not D["ok"] and not bribed:
			flag("Hatalı kusur tespiti", 1, 0.35)
		accident_dialog(m)
	var bt: Array = []
	if not D.has("secure"):
		bt.append(B("🚧 Alanı güvenliğe al (koni, ışıklar)", func():
			D["secure"] = true
			bc("Kaza alanı güvenliğe alındı")
			m["props"] = []
			for i in 4:
				var cone := MeshInstance3D.new()
				var cm := CylinderMesh.new()
				cm.top_radius = 0.03
				cm.bottom_radius = 0.22
				cm.height = 0.7
				cone.mesh = cm
				var cmat := StandardMaterial3D.new()
				cmat.albedo_color = Color(1, 0.42, 0.06)
				cone.material_override = cmat
				var a := i * PI / 2
				var cp: Vector2 = (m["p"] as Vector2) + Vector2(cos(a) * 6.0, sin(a) * 4.0)
				cone.position = Vector3(cp.x, 0.35, cp.y)
				M.add_child(cone)
				m["props"].append(cone)
			re.call()))
	if not D.has("a"):
		bt.append(B("🗒 İfade al: Sürücü A", func(): D["a"] = true; m["st"].append("A (%s): %s" % [m["drv"][0], stt.call(0)]); bc("Sürücü A ifadesi alındı"); re.call()))
	if not D.has("b"):
		bt.append(B("🗒 İfade al: Sürücü B", func(): D["b"] = true; m["st"].append("B (%s): %s" % [m["drv"][1], stt.call(1)]); bc("Sürücü B ifadesi alındı"); re.call()))
	if not D.has("photo"):
		bt.append(B("📷 Olay yerini fotoğrafla", func(): D["photo"] = true; m["ph"] = g if randf() < 0.7 else 1 - g; m["ev"].append({"n": "Olay yeri fotoğrafları"}); bc("Olay yeri fotoğraflandı"); re.call()))
	if not D.has("cam"):
		bt.append(B("📹 Trafik kamerası kaydı iste (20 sn)", func(): D["cam"] = true; m["ev"].append({"n": "Trafik kamerası kaydı (bekleniyor)", "pending": 20.0, "done": "Kamera kaydı: %s aracı kırmızı ışıkta geçmiş." % AB[g]}); bc("Kamera kaydı talep edildi"); re.call()))
	if D.has("a") and D.has("b") and not m.has("bribe"):
		m["bribe"] = randi_range(4, 12) * 50 if randf() < 0.35 else 0
	if D.has("secure") and not D.has("dec"):
		bt.append(B("⚖ Kusurlu: Sürücü A", func(): dec.call(0, false)))
		bt.append(B("⚖ Kusurlu: Sürücü B", func(): dec.call(1, false)))
		if int(m.get("bribe", 0)) > 0:
			var rk := risk_calc()
			bt.append(B("💵 %s sürücüsü: \"Kusuru ona yaz, %d$ senin\" (risk %s)" % [AB[g], m["bribe"], mix_pct(rk)], func(): corrupt_act("Rüşvet karşılığı kusur değiştirme", m["bribe"], rk, 2); dec.call(1 - g, true), "dark"))
	if D.has("dec"):
		bt.append(B("🚛 Çekici çağır, tutanağı kapat", func(): bc("Çekici çağrıldı, tutanak kapatıldı"); success(m, 1.0 if D["ok"] else 0.45, "Kusur doğru tespit edildi" if D["ok"] else "Hatalı kusur tespiti — itiraz edildi"), "good"))
	bt.append(B("Kapat"))
	modal("🚑 Trafik Kazası #%d" % m["id"], body, bt)

# ---------------------------------------------------------------- kavga
func fight_dialog(m: Dictionary) -> void:
	var F: Dictionary = m["F"]
	var cl: Array = m["cl"]
	var body = "%s: %s\n%s: %s\n%s" % [cl[0][0], m["nm"][0], cl[1][0], m["nm"][1], "[color=#5ee08a]Taraflar ayrıldı.[/color]" if F.has("sep") else "[color=#ff6b6b]Taraflar yumruklaşıyor![/color]"]
	if F.has("wit"):
		body += "\n\n👥 Tanıklar: \"Kavgayı [b]%s[/b] olan başlattı, diğeri kendini savundu.\"" % cl[m["inst"]][0]
	var re := func(): fight_dialog(m)
	var sep := func():
		F["sep"] = true
		for f in m["f"]:
			f.ai = "idle"
		bc("Taraflar ayrıldı")
	var fin := func(i: int):
		bc("Gözaltı: " + m["nm"][i])
		if i == m["inst"]:
			G["stats"]["arrests"] += 1
			success(m, 1.15 if F.has("wit") else 1.0, "Kavgayı başlatan gözaltına alındı")
		else:
			flag("Haksız gözaltı (yanlış kişi)", 2, 0.6)
			success(m, 0.35, "Yanlış kişi gözaltına alındı")
	var bt: Array = []
	if not F.has("sep"):
		bt.append(B("🗣 Sözlü müdahale ile ayır", func():
			bc("Sözlü müdahale")
			if randf() < 0.55 + team_bonus("Müzakereci") * 0.3 + int(G["rank"]) * 0.03:
				sep.call()
				radio("Taraflar sakinleşti.", "good")
			else:
				radio("Dinlemiyorlar!", "warn")
			re.call()))
		bt.append(B("🌶 Biber gazı ile ayır", func(): just_force("Kavgayı biber gazıyla ayırma"); sep.call(); re.call()))
		bt.append(B("👊 Copla dağıt (aşırı güç)", func(): flag("Kavgada aşırı güç kullanımı", 4, 0.8); sep.call(); re.call(), "dark"))
	else:
		if not F.has("wit"):
			bt.append(B("👥 Tanıkları dinle", func(): F["wit"] = true; m["st"].append("Tanıklar: kavgayı %s şahıs başlattı" % cl[m["inst"]][0]); bc("Tanık ifadeleri alındı"); re.call()))
		if not F.has("st"):
			bt.append(B("🗒 Taraflardan ifade al", func(): F["st"] = true; m["st"].append(m["nm"][0] + ": \"O bana saldırdı!\""); m["st"].append(m["nm"][1] + ": \"Asıl o başlattı!\""); bc("Taraf ifadeleri alındı"); re.call()))
		bt.append(B("🔗 Gözaltı: " + cl[0][0], func(): fin.call(0)))
		bt.append(B("🔗 Gözaltı: " + cl[1][0], func(): fin.call(1)))
		bt.append(B("🗣 İkisini de uyar ve dağıt", func(): bc("Taraflar uyarılıp dağıtıldı"); success(m, 0.6, "Taraflar uyarıldı")))
	bt.append(B("Kapat"))
	modal("👊 Kavga #%d" % m["id"], body, bt)

# ---------------------------------------------------------------- hırsızlık: olay yeri (iç mekân)
func enter_scene(m: Dictionary) -> void:
	var g: Dictionary = m["g"]
	var owner := C.rname()
	int_mission = m
	int_spots = [
		{"x": 2.2, "z": 0.7, "l": "Zorlanmış pencereyi incele", "done": false, "k": "win"},
		{"x": 14.2, "z": 2.0, "l": "Kasadan parmak izi al", "done": false, "k": "print"},
		{"x": 8.0, "z": 6.0, "l": "Ev sahibinden ifade al", "done": false, "k": "owner", "owner": owner},
		{"x": 14.5, "z": 9.0, "l": "Güvenlik kamerasını kontrol et", "done": false, "k": "cam"}]
	if m["story"] == "kuzgun1":
		int_spots.append({"x": 3.0, "z": 5.0, "l": "Yerdeki garip figür", "done": false, "k": "raven"})
	if m["stage"] == "go":
		m["stage"] = "inv"
		bc("Olay yerine girildi")
	state = "interior"
	M.enter_interior(int_spots)

func _use_spot(sp: Dictionary) -> void:
	sp["done"] = true
	if sp.has("node") and is_instance_valid(sp["node"]):
		sp["node"].visible = false
	var m: Dictionary = int_mission
	var g: Dictionary = m["g"]
	match sp["k"]:
		"win":
			m["ev"].append({"n": "Alet izi: pencere levye ile zorlanmış"})
			bc("Delil: alet izi")
			radio("Profesyonel iş: levye izi.")
		"print":
			m["ev"].append({"n": "Parmak izi (laboratuvarda)", "pending": 25.0, "done": "Parmak izi eşleşti: %s (sabıka kaydı)" % g["name"]})
			bc("Delil: parmak izi laboratuvara gönderildi")
			radio("Parmak izi laboratuvara gönderildi (25 sn).")
		"owner":
			m["st"].append("%s (ev sahibi): \"Gece sesler duydum. Pencereden %s biri atladı, karanlıktı.\"" % [sp["owner"], g["h"]])
			bc("Tanık ifadesi alındı")
			radio("İfade alındı.")
		"cam":
			m["ev"].append({"n": "Kamera: %s renkli bir araç hızla uzaklaşıyor, plaka okunmuyor" % (g["car"] as String).to_lower()})
			bc("Delil: güvenlik kamerası")
			radio("Kamera görüntüsü alındı.")
		"raven":
			m["ev"].append({"n": "Kasada bırakılmış siyah kuzgun figürü"})
			bc("Delil: kuzgun figürü")
			radio("Bu bir imza... \"Kuzgun\" mu?", "warn")

func exit_interior() -> void:
	var m = int_mission
	state = "play"
	if m != null:
		M.exit_interior(m["door"]["p"], float(m["door"]["out"]))
		if m["stage"] == "inv":
			radio("DOSYA butonundan delilleri inceleyip şüpheliyi suçlayabilirsin.")
	else:
		M.exit_interior(pp(), 1.0)
	int_spots = []

func open_file() -> void:
	var m = mission
	if m == null:
		modal("🗂 Dosya", "Aktif görev yok.\n\n" + status_text(), [B("Kapat")])
		return
	var body = "[b]#%d %s[/b] — %s\n📍 %s\n👤 %s\n🎯 %s\n⏱ %s\n\n[b]Deliller[/b]\n" % [m["id"], m["n"], C.PRI[m["pri"]], dist_n(m["dist"]), m["sdesc"], obj_text(m), "—" if m["no_timer"] else C.fmt_t(m["t"])]
	if m["ev"].is_empty():
		body += "• yok\n"
	for e in m["ev"]:
		body += "• " + e["n"] + ((" (%d sn)" % ceili(e["pending"])) if float(e.get("pending", 0)) > 0 else "") + "\n"
	body += "\n[b]İfadeler[/b]\n"
	if m["st"].is_empty():
		body += "• yok\n"
	for s in m["st"]:
		body += "• " + s + "\n"
	var bt: Array = []
	if m["type"] == "burglary" and m["stage"] == "inv":
		body += "\n[b]Şüpheliler (sabıka ve plaka sorgusu)[/b]\n"
		for s in m["list"]:
			body += "• %s — %s — araç: %s (%s) — %s\n" % [s["name"], s["h"], s["car"], s["pl"], s["rec"]]
		if int(m["phone"]) == 2:
			body += "\n📞 Telefon kayıtları: [b]%s[/b] olay saatinde bölgedeki baz istasyonuna bağlanmış." % m["g"]["name"]
		if int(m["phone"]) == 0:
			bt.append(B("📞 Telefon kayıtlarını iste (savcılık, 20 sn)", func():
				m["phone"] = 1
				bc("Telefon kayıtları talep edildi")
				get_tree().create_timer(20.0, false).timeout.connect(func():
					if mission == m:
						m["phone"] = 2
						m["ev"].append({"n": "Telefon kayıtları: %s olay yerinde" % m["g"]["name"]})
						radio("Telefon kayıtları geldi.", "good"))))
		for s in m["list"]:
			var sd: Dictionary = s
			bt.append(B("⚖ Suçla: " + sd["name"], func(): _accuse(m, sd), "dark"))
	bt.append(B("Kapat"))
	modal("🗂 Soruşturma Dosyası", body, bt)

func _accuse(m: Dictionary, s: Dictionary) -> void:
	var n: int = m["ev"].size() + m["st"].size()
	bc("Suçlama: " + s["name"])
	if s == m["g"]:
		if n < 2:
			SH["disc"] += 1
		m["ev"].append({"n": "Savcılık arama/yakalama kararı: " + s["name"]})
		var d: Dictionary = M.city.find_door("house", "", pp(), 50.0, 350.0)
		if d.is_empty():
			d = m["door"]
		m["door"] = d
		m["p"] = d["p"]
		m["stage"] = "arrest"
		m["t"] = float(m["t"]) + 90.0
		m["sdesc"] = "%s, %s, araç: %s %s" % [s["name"], s["h"], s["car"], s["pl"]]
		radio("Karar çıktı! %s adresinde. Yakala." % s["name"], "good")
	else:
		flag("Yanlış suçlama: " + s["name"], 3, 0.9)
		fail(m, "Yanlış kişi suçlandı — masum vatandaş mağdur oldu")

# ---------------------------------------------------------------- rehine
func _start_neg(m: Dictionary) -> void:
	m["no_timer"] = true
	negotiate(m)

func negotiate(m: Dictionary) -> void:
	if not m.has("trust"):
		m["trust"] = 40
		m["round"] = 0
		m["hk"] = C.pick(["korkmuş", "öfkeli", "çaresiz"])
	var hk: String = m["hk"]
	var lines := {"korkmuş": "\"Yaklaşmayın! Hapse girmek istemiyorum!\"", "öfkeli": "\"Herkes bedelini ödeyecek! Patron beni kovdu!\"", "çaresiz": "\"Çocuğumu göremiyorum... başka çarem kalmadı.\""}
	var opts = [["🤝 Empati: \"Seni anlıyorum, konuşalım.\"", {"korkmuş": 15, "öfkeli": 5, "çaresiz": 20}], ["🧱 Kararlı ol: \"Silahı bırak, başka çıkış yok.\"", {"korkmuş": -10, "öfkeli": 12, "çaresiz": -5}], ["📜 Söz ver: \"Avukatın ve adil bir süreç olacak.\"", {"korkmuş": 12, "öfkeli": 0, "çaresiz": 12}], ["⚠ Tehdit: \"Özel harekat kapıda!\"", {"korkmuş": -20, "öfkeli": -15, "çaresiz": -15}]]
	var bt: Array = []
	for o in opts:
		var od: Array = o
		bt.append(B(od[0], func():
			m["trust"] = clampi(int(m["trust"]) + int(od[1][hk]) + randi_range(-5, 5) + roundi(team_bonus("Müzakereci") * 10), 0, 100)
			m["round"] += 1
			bc("Müzakere: " + (od[0] as String).substr(2, 28))
			if int(m["round"]) >= 3 or int(m["trust"]) >= 85 or int(m["trust"]) <= 8:
				resolve_hostage(m)
			else:
				negotiate(m)))
	var bar = "█".repeat(int(m["trust"]) / 5) + "░".repeat(20 - int(m["trust"]) / 5)
	modal("🎙 Rehine Müzakeresi — Tur %d/3" % (int(m["round"]) + 1), "Rehine alan şahıs [b]%s[/b] görünüyor.\n\n%s\n\nGüven: %s %d" % [hk, lines[hk], bar, m["trust"]], bt)

func resolve_hostage(m: Dictionary) -> void:
	if int(m["trust"]) >= 60:
		bc("Rehineler serbest, şüpheli teslim oldu")
		m["ev"].append({"n": "Teslim tutanağı"})
		G["stats"]["arrests"] += 1
		success(m, 1.3, "Kansız çözüm — rehineler kurtarıldı")
		return
	var bt = [B("🛡 Özel harekat baskını iste", func():
		var pr = 0.55 + team_bonus("Taktik") * 0.25 + int(G["rank"]) * 0.03
		if randf() < pr:
			bc("Baskın başarılı")
			success(m, 0.9, "Baskınla çözüldü")
		else:
			bc("Baskın başarısız")
			fail(m, "Baskın sırasında rehine yaralandı"))]
	if not m.get("extra", false):
		bt.append(B("Bir tur daha dene", func(): m["extra"] = true; m["round"] = 2; negotiate(m)))
	modal("Müzakere tıkandı", "Şüpheli taleplerini sertleştiriyor. Karar senin.", bt)

# ---------------------------------------------------------------- hikâye görevleri
func meet_dialog(m: Dictionary) -> void:
	m["no_timer"] = true
	var fin := func(t: String):
		m["st"].append(t)
		success(m, 1.0, "Muhbir görüşmesi tamamlandı")
	modal("🐦 Serçe", "Gölgeden bir ses: \"Kuzgun'u soruyormuşsun. Liman depolarından mal giriyor, gümrükte de birileri göz yumuyor. Ama bilgi bedava değil memur bey.\"\n\nBir an duraksıyor: \"Bir de... Kuzgun seni tanımak istiyor. Ayda 500$. Sadece bazı devriyeleri görmezden gel.\"", [
		B("💵 Kendi cebinden 200$ öde (iyi bilgi)", func():
			if int(G["money"]) < 200:
				radio("Yeterli paran yok.", "bad")
				meet_dialog(m)
				return
			G["money"] -= 200
			G["story"]["info"] += 2
			bc("Muhbire ödeme yapıldı")
			fin.call("Serçe: \"Teslimatlar Sanayi'deki eski fabrikada. Bir de... sizin içeriden biri var. Rütbeli.\"")),
		B("📋 Resmi muhbir olarak kaydet (prosedür)", func(): G["story"]["info"] += 1; honest("Muhbiri usulüne uygun kaydetti", 3); fin.call("Serçe: \"Kayıt mı? Peki... Sanayi bölgesinde bir depo. Dikkatli ol.\""), "good"),
		B("😠 Tehdit ederek konuştur", func(): G["story"]["info"] += 1; G["integrity"] = clampi(int(G["integrity"]) - 4, 0, 100); flag("Muhbire tehdit", 2, 0.3); fin.call("Serçe korkuyla: \"Sanayi... depo... başka bir şey bilmiyorum!\"")),
		B("🤝 Kuzgun'un teklifini kabul et (500$/vardiya)", func(): G["story"]["path"] = "corrupt"; G["story"]["kuzgun_pay"] = true; corrupt_act("Organize suç örgütünden düzenli ödeme anlaşması", 500, risk_calc() * 0.5, 3); fin.call("Serçe gülümsüyor: \"Akıllı adamsın. Kuzgun unutmaz.\""), "dark")])

func final_choice(m: Dictionary) -> void:
	modal("📱 Kuzgun arıyor", "\"Bana borçlusun memur. Bu gece gözünü kapat, yarın zengin uyan. Ya da... herkes senin aldığın paraları öğrenir.\"", [
		B("🤝 Kuzgun'a kaçış yolu aç (+$20.000)", func():
			corrupt_act("Organize suç liderinin kaçmasına yardım", 20000, 0.3 + float(G["suspicion"]) / 200.0, 5)
			mission = null
			cleanup(m)
			ending("fall" if (float(G["suspicion"]) > 60 or G["investigation"]) else "shadow"), "dark"),
		B("⚖ Hayır. Operasyona devam.", func(): honest("Kuzgun'un şantajını reddetti", 10); G["story"]["path"] = "redeemed"; start_chase(m, m["car"]); m["car"].hp = 150.0, "good")])

func story_offer():
	var st: Dictionary = G["story"]
	if SH["story_given"] or st["ending"] != "":
		return null
	var m = null
	if int(st["stage"]) == 0 and int(G["stats"]["done"]) >= 2:
		m = make_mission("burglary", {"story": "kuzgun1", "n": "★ Kuzgun'un İzi"})
	elif int(st["stage"]) == 1 and int(G["rank"]) >= 1:
		m = make_mission("meet", {"story": "meet"})
	elif int(st["stage"]) == 2 and int(G["rank"]) >= 2:
		m = make_mission("undercover", {"story": "depo", "n": "★ Gizli Operasyon: Fabrika"})
	elif int(st["stage"]) == 4 and int(G["rank"]) >= 3 and int(st["planned"]) == 1:
		m = make_mission("final", {"story": "final"})
	if m != null:
		SH["story_given"] = true
	return m

func story_advance(m: Dictionary) -> void:
	var st: Dictionary = G["story"]
	match m["story"]:
		"kuzgun1":
			st["stage"] = 1
			modal("★ Kuzgun'un İzi", "Kasada bırakılan siyah kuzgun figürü... Arşivde aynı imza son iki yılda 14 dosyada geçiyor ve hepsi \"faili meçhul\" kapanmış. Biri bu dosyaları kapatıyor.\n\n[i]Polis Memuru rütbesine ulaştığında bir muhbir seninle temas kuracak.[/i]", [B("Devam")])
		"meet":
			st["stage"] = 2
			modal("★ Serçe", "Bilgiler bir depoyu işaret ediyor. Kıdemli Polis Memuru olduğunda gizli operasyona yetkin olacak.", [B("Devam")])
		"depo":
			st["stage"] = 3
			modal("★ Fabrika", "Fotoğraflardan birinde, teslimatı izleyen bir araç var: plaka [b]Başkomiser Selim Kaya[/b]'ya ait. Senin amirin.", [B("Devam")])
		"final":
			ending("redeem" if st["path"] in ["corrupt", "redeemed"] else "hero")

func betrayal() -> void:
	modal("★ İhanet — Başkomiser Kaya'nın odası", "Kaya kapıyı kapatıyor. Masaya kalın bir zarf koyuyor.\n\n\"Fabrika dosyasını gördüm evlat. O fotoğraf yanlış anlaşılabilir. Zarfta 5.000$ var. Dosyayı kapat, kariyerin yükselsin. Kapatmazsan... bu şehirde polislik zor iştir.\"", [
		B("💵 Zarfı al, dosyayı kapat", func(): G["story"]["path"] = "corrupt"; corrupt_act("Amirden rüşvet alıp soruşturma dosyasını kapatma", 5000, 0.25, 4); G["story"]["stage"] = 4; plan_op(), "dark"),
		B("⚖ Reddet ve İç Denetime bildir", func(): G["story"]["path"] = "honest"; honest("Amirin rüşvet teklifini İç Denetime bildirdi", 10); G["story"]["stage"] = 4; modal("İç Denetim", "Müfettiş Ayla Demir: \"Cesur bir karar. Kaya gözaltında ama Kuzgun hâlâ dışarıda. Operasyonu sen yöneteceksin.\"", [B("Devam", plan_op)]), "good"),
		B("🎙 Kabul eder gibi yap, gizli kayıt yap" + ("" if int(G["integrity"]) >= 55 else " (dürüstlük ≥55 gerekir)"), func(): G["story"]["path"] = "wire"; honest("Gizli kayıtla amirin suçunu belgeledi", 8); G["story"]["stage"] = 4; G["story"]["info"] += 2; modal("Gizli Kayıt", "Kaya'nın sözleri kayıtta. İç Denetim seni \"çift ajan\" olarak görevlendirdi.", [B("Devam", plan_op)]), "", int(G["integrity"]) < 55)])

func plan_op() -> void:
	if int(G["rank"]) < 3:
		G["story"]["planned"] = 0
		modal("Plan beklemede", "Son operasyonu planlamak için Komiser Yardımcısı rütbesi gerekiyor.", [B("Brifinge dön", briefing)])
		return
	var tb := func(t: String) -> int: return roundi(team_bonus(t) * 100)
	var go := func():
		G["story"]["planned"] = 1
		radio("Operasyon planlandı. Kuzgun sinyali gelince anons edilecek.", "good")
		briefing()
	modal("★ Özel Operasyon Planlama", "Hedef: Demir \"Kuzgun\" Aksoy. Liman.\nBilgi seviyesi: %d\nEkip: Taktik %%%d, Sürücü %%%d, Dedektif %%%d" % [G["story"]["info"], tb.call("Taktik"), tb.call("Sürücü"), tb.call("Dedektif")], [B("🌅 Şafak baskını (Taktik)", go), B("🚧 Yol kesme (Sürücü)", go), B("🕵 Gizli takip (Dedektif)", go)])

const ENDINGS := {
	"hero": ["⚖ Son: Şehrin Kahramanı", "Kuzgun kelepçeli. Kaya ve gümrükteki ortakları tutuklandı. Basın seni \"Dürüst Memur\" diye anıyor. Teşkilat seni Emniyet Amirliğine hazırlıyor.", false],
	"redeem": ["🌗 Son: Geç Gelen Adalet", "Kuzgun'u yakaladın ama geçmişteki hatalar dosyanda duruyor. Mahkemede kendi ifadeni de verdin. Şehir biraz daha temiz, sen biraz daha yorgun.", false],
	"shadow": ["🕶 Son: Gölgelerin Efendisi", "Kuzgun gece ortadan kayboldu, yerine sen geçtin. Rütben yükseliyor, hesabın büyüyor... ama her siren sesinde kapının çalınmasını bekliyorsun.", false],
	"fall": ["⛓ Son: Çöküş", "Kuzgun'a yardım ettiğin gece İç Denetim tüm konuşmaları dinliyordu. Kelepçe bu sefer senin bileklerinde.", true],
	"expel": ["🚫 Meslekten İhraç", "Disiplin kurulu kararı kesin: meslekten ihraç edildin. Rozetini ve silahını teslim ettin.", true],
	"arrest": ["⛓ Tutuklandın", "İç denetimin gizli soruşturması tamamlandı. Rüşvet ve görevi kötüye kullanmaktan tutuklandın.", true],
}

func ending(k: String) -> void:
	var e: Array = ENDINGS[k]
	G["story"]["ending"] = k
	state = "hub"
	M.ui.set_hud_visible(false)
	M.set_siren(false)
	if k == "hero":
		G["rank"] = maxi(int(G["rank"]), 5)
	if e[2]:
		modal(e[0], e[1] + "\n\n" + status_text(), [B("Yeni kariyer", func(): DirAccess.remove_absolute(ProjectSettings.globalize_path(SAVE)); title_screen(), "pri")])
	else:
		modal(e[0], e[1] + "\n\n" + status_text(), [B("Serbest oyuna devam", func(): save(); open_hub(), "pri")])

# ---------------------------------------------------------------- vardiya
func briefing() -> void:
	if G["suspended"]:
		G["suspended"] = false
		modal("⛔ Görevden Uzaklaştırma", "Bu vardiyaya katılamazsın. Maaş ödenmedi. Bir gün geçti.", [B("Eve dön", func(): G["day"] += 1; G["fatigue"] = maxf(0, G["fatigue"] - 30); save(); open_hub())])
		return
	if int(G["story"]["stage"]) == 3 and int(G["rank"]) >= 2:
		betrayal()
		return
	var sel := {}
	var vs: Array = []
	for k in C.VEH.keys():
		var v: Dictionary = C.VEH[k]
		if (v.has("premium") and G["premium"].get(k, false)) or (not v.has("premium") and int(G["rank"]) >= int(v["rank"])):
			vs.append(k)
	var extra := func(box: VBoxContainer):
		if float(G["fatigue"]) > 60:
			box.add_child(M.ui.mk_label("⚠ Çok yorgunsun! Dikkatin ve tepki süren düşük olacak.", 20))
		var mk := func(label: String, items: Array, key: String, cur: int):
			box.add_child(M.ui.mk_label(label, 20))
			var ob := OptionButton.new()
			ob.add_theme_font_size_override("font_size", 20)
			ob.custom_minimum_size = Vector2(0, 48)
			for it in items:
				ob.add_item(it)
			ob.selected = cur
			box.add_child(ob)
			sel[key] = ob
		mk.call("Vardiya", ["Gündüz (08:00–16:00) — yoğun trafik", "Gece (20:00–04:00) — daha çok suç"], "shift", 0)
		var regs: Array = C.DIST.keys()
		var rn: Array = regs.map(func(r): return "%s — %s (suç x%.2f)" % [C.DIST[r]["n"], C.DIST[r]["desc"], C.DIST[r]["crime"]])
		mk.call("Devriye bölgesi", rn, "reg", maxi(0, regs.find(G["region"])))
		mk.call("Araç", vs.map(func(k): return C.VEH[k]["n"]), "veh", maxi(0, vs.find(G["veh"])))
		box.add_child(M.ui.mk_label("Kilitli araçlar rütbeyle açılır: SUV / Motosiklet / Trafik (Polis Memuru), Sivil (Kıdemli), Özel Operasyon (Komiser).", 17))
	modal("📋 Vardiya Brifingi — Gün %d" % G["day"], "", [B("🚓 Devriyeye çık", func():
		var sh = "day" if (sel["shift"] as OptionButton).selected == 0 else "night"
		var rg: String = C.DIST.keys()[(sel["reg"] as OptionButton).selected]
		var vh: String = vs[(sel["veh"] as OptionButton).selected]
		begin_shift(sh, rg, vh), "pri"), B("Geri", open_hub)], extra)

func begin_shift(sh: String, rg: String, vh: String) -> void:
	G["shift_type"] = sh
	G["region"] = rg
	G["veh"] = vh
	G["time"] = 480.0 if sh == "day" else 1200.0
	SH = {"end_abs": now_abs() + 480.0, "done": 0, "fail": 0, "earn": 0, "fines": 0, "xp": 0, "disc": 0, "flags": [], "detections": [], "dirty": 0, "honest": 0, "stops": 0, "patrol": 0.0, "declined": 0, "cam_off": 0.0, "corrupt_n": 0, "story_given": false, "peds": 0, "end_warned": false}
	var late: Dictionary = G["late"]
	if late.has("dirty"):
		SH["dirty"] += late["dirty"]
		SH["corrupt_n"] += late["corrupt_n"]
		SH["detections"].append_array(late["detections"])
		G["late"] = {}
	mission = null
	offer = null
	stop_target = null
	next_offer = 7.0
	backup_cd = 0.0
	M.ui.hide_offer()
	M.traffic.clear_all()
	var kd: Dictionary = M.city.karakol_door()
	var kp: Vector2 = kd["p"]
	M.spawn_car(vh, Vector3(kp.x, 0.6, C.S + 3.5), PI / 2)
	M.player.global_position = Vector3(kp.x, 0.4, kp.y - 1.5)
	M.set_outfit(Color(_outfit_col()))
	M.set_mode("car")
	M.cam_yaw = -PI / 2
	M.city.update_stream(pp(), 0.0, true)
	_set_weather()
	M.set_siren(false)
	M.lights_on = night_f() > 0.3
	cam_on = true
	state = "play"
	M.ui.set_hud_visible(true)
	for i in 8:
		M.traffic.spawn_car("civ", 25, 90)
	for i in 6:
		M.traffic.spawn_walker()
	radio("Merkez: %s, %s devriye bölgen. İyi vardiyalar." % [G["name"], dist_n(rg)], "good")
	bc("Vardiya başladı: %s / %s" % [dist_n(rg), C.VEH[vh]["n"]])
	if G["story"]["path"] == "corrupt" and G["story"]["kuzgun_pay"]:
		G["money"] += 500
		SH["dirty"] += 500
		G["suspicion"] = clampf(float(G["suspicion"]) + 4, 0, 100)
		radio("Hesabına isimsiz 500$ yatırıldı...", "warn")

func _outfit_col() -> String:
	for o in C.OUTFITS:
		if o["id"] == G["outfit"]:
			return o["col"]
	return "#1a2c5c"

func _set_weather() -> void:
	var r := randf()
	M.weather = "clear" if r < 0.5 else ("overcast" if r < 0.7 else ("rain" if r < 0.87 else "fog"))

const WNAME := {"clear": "Açık", "overcast": "Bulutlu", "rain": "Yağmurlu", "fog": "Sisli"}
var weather_t := 120.0

func end_shift(early: bool) -> void:
	if SH == null:
		return
	M.set_siren(false)
	if mission != null:
		cleanup(mission)
		mission = null
	offer = null
	M.ui.hide_offer()
	state = "hub"
	M.ui.set_hud_visible(false)
	var sal: int = C.RANKS[int(G["rank"])]["sal"]
	var lines: Array = []
	var cut := 0
	var dmg_cost = roundi(M.dmg * 2.5)
	var perf = clampi(roundi(50 + SH["done"] * 12 - SH["fail"] * 14 - SH["declined"] * 4 + SH["stops"] * 3 + minf(20, float(SH["patrol"]) * 0.15) - SH["disc"] * 5 - (20 if early else 0) - SH["peds"] * 15), 0, 100)
	var grade := "A" if perf >= 85 else ("B" if perf >= 70 else ("C" if perf >= 55 else ("D" if perf >= 40 else "F")))
	var team_lines: Array = []
	var team_xp := 0
	for t in G["team"]:
		if t["assign"] != "takviye":
			var solved = maxi(0, roundi(float(t["skill"]) / 100.0 * float(C.DIST[t["assign"]]["crime"]) * 3.0 * (float(t["morale"]) / 80.0) + randf() - 0.3))
			team_xp += solved * 15
			t["cases"] += solved
			team_lines.append("%s: %d olay çözdü" % [t["name"], solved])
			if randf() < (100.0 - float(t["integ"])) / 450.0:
				team_lines.append("[color=#ff6b6b]%s hakkında şikâyet (denetim eksikliği)[/color]" % t["name"])
				SH["disc"] += 1
		t["morale"] = clampi(int(t["morale"]) + (2 if grade in ["A", "B"] else -2), 10, 100)
	if team_xp > 0:
		G["xp"] += team_xp
		SH["xp"] += team_xp
	var comp: Array = G["complaints"].duplicate()
	G["complaints"] = []
	var comp_lines: Array = []
	for c in comp:
		if c["just"] and c["cam"]:
			comp_lines.append("✔ \"%s\" — kamera kaydı incelendi, şikâyet reddedildi." % c["text"])
			G["integrity"] = clampi(int(G["integrity"]) + 1, 0, 100)
		elif c["just"]:
			comp_lines.append("△ \"%s\" — kayıt yok, kısmen haklı bulundu." % c["text"])
			SH["disc"] += 1
		elif c["cam"]:
			comp_lines.append("✖ \"%s\" — kamera kaydı aleyhine delil oldu!" % c["text"])
			SH["disc"] += int(c["d"])
			cut += roundi(sal * 0.15)
		else:
			comp_lines.append("✖ \"%s\" — kamera kapalıydı, iç denetim şüpheli buldu." % c["text"])
			SH["disc"] += int(c["d"])
			G["suspicion"] = clampf(float(G["suspicion"]) + 6, 0, 100)
	if float(SH["cam_off"]) > 90:
		SH["disc"] += 1
		comp_lines.append("⚠ Vücut kamerası uzun süre kapalı kaldı (prosedür ihlali).")
	G["disc"] += SH["disc"]
	if int(SH["disc"]) == 0 and int(SH["corrupt_n"]) == 0:
		G["disc"] = maxi(0, int(G["disc"]) - 2)
	var over := ""
	var sanc: Array = []
	if int(SH["disc"]) >= 4:
		cut += roundi(sal * 0.2)
		sanc.append("Maaş kesintisi (%20)")
	if int(G["disc"]) >= 50:
		over = "expel"
	elif int(G["disc"]) >= 35:
		G["suspended"] = true
		G["disc"] -= 6
		sanc.append("GÖREVDEN UZAKLAŞTIRMA (1 vardiya)")
	elif int(G["disc"]) >= 25 and int(SH["disc"]) > 0:
		if int(G["rank"]) > 0:
			G["rank"] -= 1
			sanc.append("RÜTBE DÜŞÜRME → " + C.RANKS[int(G["rank"])]["n"])
		G["disc"] -= 8
	var ia := "Herhangi bir inceleme yok."
	if int(G["corrupt"]) > 0:
		var ch = clampf(float(G["suspicion"]) / 140.0 + SH["detections"].size() * 0.35 + (0.15 if G["investigation"] else 0.0), 0, 0.95)
		var roll = randf() < ch if (int(SH["corrupt_n"]) > 0 or SH["detections"].size() > 0) else randf() < ch * 0.3
		if roll:
			var sev := 0
			for dd in SH["detections"]:
				sev += int(dd["sev"])
			sev = maxi(sev, 1)
			var lvl = (1 if G["investigation"] else 0) + mini(3, ceili(sev / 2.0)) + (1 if int(G["corrupt"]) >= 8 else 0)
			G["money"] -= SH["dirty"]
			if lvl >= 5:
				over = "arrest"
			elif lvl == 4:
				over = "expel"
			elif lvl == 3:
				G["suspended"] = true
				if int(G["rank"]) > 0:
					G["rank"] -= 1
				ia = "İç denetim usulsüzlük tespit etti: rütbe düşürme + görevden uzaklaştırma. Yasa dışı kazanca el konuldu."
			elif lvl == 2:
				if int(G["rank"]) > 0:
					G["rank"] -= 1
				cut += roundi(sal * 0.4)
				ia = "İç denetim: rütbe düşürme ve %40 maaş kesintisi."
			else:
				cut += roundi(sal * 0.4)
				ia = "İç denetim: %40 maaş kesintisi. Hakkında GİZLİ SORUŞTURMA açıldı."
			G["investigation"] = true
		elif float(G["suspicion"]) > 40:
			ia = "Söylentiler dolaşıyor... İç denetim dosyanı inceliyor olabilir."
	if int(SH["corrupt_n"]) == 0:
		G["suspicion"] = maxf(0.0, float(G["suspicion"]) - 3)
	var honor = int(SH["honest"]) * 40
	if int(G["integrity"]) >= 85 and not G["medal"] and int(G["stats"]["done"]) >= 8:
		G["medal"] = true
		honor += 500
		G["xp"] += 200
		lines.append("[color=#5ee08a]🎖 ÜSTÜN HİZMET MADALYASI! +$500, +200 XP[/color]")
	var cr := 5 if grade == "A" else (2 if grade == "B" else 0)
	G["credits"] += cr
	var net = sal + int(SH["earn"]) + honor - int(SH["fines"]) - dmg_cost - cut
	G["money"] += net
	var promo := ""
	if over == "" and int(G["rank"]) < 7 and int(G["xp"]) >= int(C.RANKS[int(G["rank"]) + 1]["xp"]) and int(G["disc"]) < 20 and grade != "F":
		G["rank"] += 1
		promo = "[color=#5ee08a][b]⭐ TERFİ! Yeni rütbe: %s[/b][/color]\n\n" % C.RANKS[int(G["rank"])]["n"]
	G["time"] = 960.0 if G["shift_type"] == "day" else 240.0
	if G["shift_type"] == "night":
		G["day"] += 1
	var t = "%sGün %d • %s vardiyası • %s\n\n" % [promo, G["day"], "Gündüz" if G["shift_type"] == "day" else "Gece", dist_n(G["region"])]
	t += "Tamamlanan / başarısız görev: %d / %d\nProaktif trafik kontrolü: %d\nBölgede devriye: %d dk\nReddedilen anons: %d\n\n" % [SH["done"], SH["fail"], SH["stops"], roundi(SH["patrol"]), SH["declined"]]
	t += "Maaş (%s): +$%d\nGörev primleri: +$%d\n" % [C.RANKS[int(G["rank"])]["n"], sal, SH["earn"]]
	if honor > 0:
		t += "[color=#5ee08a]Dürüstlük primi: +$%d[/color]\n" % honor
	t += "[color=#ff6b6b]Görev cezaları: -$%d\nAraç hasar kesintisi: -$%d[/color]\n" % [SH["fines"], dmg_cost]
	if cut > 0:
		t += "[color=#ff6b6b]Disiplin kesintisi: -$%d[/color]\n" % cut
	t += "[b]NET: %s$%d[/b]\n\nXP: +%d\n[b]Performans notu: %s[/b] (%d/100)%s\nDisiplin puanı: %d (bu vardiya +%d)\nDürüstlük: %d/100\n" % ["+" if net >= 0 else "-", absi(net), SH["xp"], grade, perf, (" • +%d kredi" % cr) if cr > 0 else "", G["disc"], SH["disc"], G["integrity"]]
	if lines.size() > 0:
		t += "\n" + "\n".join(lines) + "\n"
	if team_lines.size() > 0:
		t += "\n[b]Ekip raporu[/b]\n" + "\n".join(team_lines) + "\n"
	t += "\n[b]Şikâyet incelemeleri[/b]\n" + ("\n".join(comp_lines) if comp_lines.size() > 0 else "Şikâyet yok.")
	if sanc.size() > 0:
		t += "\n[color=#ffbf47]Disiplin kurulu: " + ", ".join(sanc) + "[/color]"
	t += "\n\n[b]İç denetim[/b]\n" + ia
	SH = null
	M.traffic.clear_all()
	modal("📊 Vardiya Sonu Değerlendirmesi", t, [B("Devam" if over != "" else "🏠 Eve dön", func():
		if over != "":
			ending(over)
		else:
			save()
			open_hub(), "pri")])

# ---------------------------------------------------------------- kişisel yaşam
func comfort() -> int:
	var c: int = C.HOMES[int(G["home"])]["com"]
	for f in G["furn"]:
		for it in C.FURN:
			if it["id"] == f:
				c += int(it["com"])
	return mini(100, c)

func open_hub(tab: String = "") -> void:
	state = "hub"
	if tab != "":
		hub_tab = tab
	M.ui.set_hud_visible(false)
	var tabs = [["durum", "Durum"], ["ev", "Ev"], ["market", "Market"], ["sosyal", "Sosyal"], ["garaj", "Garaj"], ["kiyafet", "Kıyafet"], ["ekip", "Ekip"], ["premium", "★ Mağaza"], ["kayit", "Kayıtlar"]]
	var body := ""
	var rows: Array = []
	var row := func(l: String, bt: String, fn: Callable, dis: bool = false):
		rows.append([l, bt, fn, dis])
	var re := func(): open_hub()
	match hub_tab:
		"durum":
			body = "👮 [b]%s[/b] — %s\n📅 Gün %d • 🕒 %s\n💵 Banka: [b]$%d[/b] • ★ Kredi: %d\nXP: %d%s\n\n😴 Yorgunluk: %d • 🍽 Açlık: %d • 😣 Stres: %d • ❤ Sağlık: %d\n🏠 %s (konfor %d)\n\n⚖ Disiplin puanı: %d • 🤝 Dürüstlük: %d/100%s%s\n\n[i]Yorgunluk vardiyada dikkat ve tepki süresini düşürür. Uyumadan çalışma![/i]" % [G["name"], C.RANKS[int(G["rank"])]["n"], G["day"], C.fmt_clock(G["time"]), G["money"], G["credits"], G["xp"], (" / %d (sonraki rütbe)" % C.RANKS[int(G["rank"]) + 1]["xp"]) if int(G["rank"]) < 7 else "", roundi(G["fatigue"]), roundi(G["hunger"]), roundi(G["stress"]), roundi(G["hp"]), C.HOMES[int(G["home"])]["n"], comfort(), G["disc"], G["integrity"], "\n[color=#ff6b6b]Hakkında gizli soruşturma var.[/color]" if G["investigation"] else "", "\n[color=#ffbf47]İç denetim seni izliyor olabilir.[/color]" if float(G["suspicion"]) > 40 else ""]
		"ev":
			body = "[b]Konut[/b]"
			for i in C.HOMES.size():
				var h: Dictionary = C.HOMES[i]
				var own: bool = G["own_homes"].has(i)
				var ii := i
				row.call("%s — konfor %d%s%s" % [h["n"], h["com"], (" • kira $%d/gün" % h["rent"]) if int(h["rent"]) > 0 else "", (" • $%d" % h["price"]) if int(h["price"]) > 0 else ""], "Oturuyorsun" if i == int(G["home"]) else ("Taşın" if (own or int(h["rent"]) > 0 or i == 0) else "Satın al"), func():
					if not own and int(h["price"]) > 0:
						if int(G["money"]) < int(h["price"]):
							radio("Yetersiz bakiye.", "bad")
							return
						G["money"] -= h["price"]
						G["own_homes"].append(ii)
						if int(G["corrupt"]) > 0 and int(h["price"]) > int(C.RANKS[int(G["rank"])]["sal"]) * 40:
							G["suspicion"] = clampf(float(G["suspicion"]) + 12, 0, 100)
					G["home"] = ii
					re.call(), i == int(G["home"]))
			for f in C.FURN:
				var fd: Dictionary = f
				var has: bool = G["furn"].has(f["id"])
				row.call("🛋 %s (+%d konfor)" % [f["n"], f["com"]], "Var" if has else (("%d kredi" % f["cr"]) if f.has("cr") else ("$%d" % f["p"])), func():
					if fd.has("cr"):
						if int(G["credits"]) < int(fd["cr"]):
							radio("Kredi yetersiz.", "bad")
							return
						G["credits"] -= fd["cr"]
					else:
						if int(G["money"]) < int(fd["p"]):
							radio("Yetersiz bakiye.", "bad")
							return
						G["money"] -= fd["p"]
					G["furn"].append(fd["id"])
					re.call(), has)
		"market":
			body = "Açlık: %d • Dolapta öğün: %d" % [roundi(G["hunger"]), G["meals"]]
			row.call("Sandviç ($8) — açlık -30", "Al", func(): _buy(8, func(): G["hunger"] = maxf(0, G["hunger"] - 30)); re.call())
			row.call("Haftalık market alışverişi ($45) — 4 öğün", "Al", func(): _buy(45, func(): G["meals"] += 4); re.call())
			row.call("Evde yemek yap (1 öğün) — açlık -45, stres -5", "Pişir", func(): G["meals"] -= 1; G["hunger"] = maxf(0, G["hunger"] - 45); G["stress"] = maxf(0, G["stress"] - 5); re.call(), int(G["meals"]) < 1)
			row.call("Enerji içeceği ($6) — yorgunluk -12, stres +3", "Al", func(): _buy(6, func(): G["fatigue"] = maxf(0, G["fatigue"] - 12); G["stress"] += 3); re.call())
			if G["furn"].has("coffee"):
				row.call("Kahve makinesi — yorgunluk -8", "İç", func(): G["fatigue"] = maxf(0, G["fatigue"] - 8); G["coffee_day"] = G["day"]; re.call(), int(G["coffee_day"]) == int(G["day"]))
		"sosyal":
			body = "Stres: %d • İlişki (Deniz): %d/100" % [roundi(G["stress"]), G["rel"]]
			row.call("Aileni ara — stres -8", "Ara", func(): G["stress"] = maxf(0, G["stress"] - 8); radio("Annen: \"Kendine dikkat et.\""); re.call())
			row.call("Arkadaşlarla akşam yemeği ($40) — stres -25", "Git", func(): _buy(40, func(): G["stress"] = maxf(0, G["stress"] - 25); G["hunger"] = maxf(0, G["hunger"] - 40)); re.call())
			row.call("Deniz ile buluş ($60) — stres -30, ilişki +8", "Buluş", func():
				_buy(60, func(): G["stress"] = maxf(0, G["stress"] - 30); G["rel"] = mini(100, int(G["rel"]) + 8))
				if int(G["rel"]) >= 80 and not G["engaged"]:
					G["engaged"] = true
					modal("💍", "Deniz ile ilişkiniz ciddileşti! Stres artık daha yavaş birikiyor.", [B("Harika", re)])
				else:
					re.call())
			row.call("Spor salonu ($15) — stres -15, sağlık +10", "Git", func(): _buy(15, func(): G["stress"] = maxf(0, G["stress"] - 15); G["hp"] = minf(100, G["hp"] + 10); G["fatigue"] += 5); re.call())
		"garaj":
			body = "[b]Teşkilat araçları (rütbeyle açılır)[/b]\n"
			for k in C.VEH.keys():
				var v: Dictionary = C.VEH[k]
				body += "• %s — %s\n" % [v["n"], "Mağaza" if v.has("premium") else ("[color=#5ee08a]Açık[/color]" if int(v["rank"]) <= int(G["rank"]) else C.RANKS[int(v["rank"])]["n"])]
			body += "\n[b]Kişisel araç[/b]\n[i]Gelirine göre aşırı lüks harcama iç denetimin dikkatini çekebilir.[/i]"
			for i in C.PCARS.size():
				var pc: Dictionary = C.PCARS[i]
				var ii2 := i
				row.call("%s — $%d" % [pc["n"], pc["p"]], "Senin" if int(G["pcar"]) == i else "Satın al", func():
					if int(G["money"]) < int(pc["p"]):
						radio("Yetersiz bakiye.", "bad")
						return
					G["money"] -= pc["p"]
					G["pcar"] = ii2
					if int(pc["p"]) > int(C.RANKS[int(G["rank"])]["sal"]) * 30 and int(G["corrupt"]) > 0:
						G["suspicion"] = clampf(float(G["suspicion"]) + 10, 0, 100)
					G["stress"] = maxf(0, G["stress"] - 10)
					re.call(), int(G["pcar"]) == i)
		"kiyafet":
			for o in C.OUTFITS:
				var od: Dictionary = o
				var has2: bool = G["outfits"].has(o["id"])
				row.call(o["n"], "Giyili" if G["outfit"] == o["id"] else ("Giy" if has2 else (("%d kredi" % o["cr"]) if o.has("cr") else ("$%d" % o["p"]))), func():
					if not has2:
						if od.has("cr"):
							if int(G["credits"]) < int(od["cr"]):
								radio("Kredi yetersiz.", "bad")
								return
							G["credits"] -= od["cr"]
						else:
							if int(G["money"]) < int(od["p"]):
								radio("Yetersiz bakiye.", "bad")
								return
							G["money"] -= od["p"]
						G["outfits"].append(od["id"])
					G["outfit"] = od["id"]
					M.set_outfit(Color(od["col"]))
					re.call(), G["outfit"] == o["id"])
		"ekip":
			var slots = maxi(0, int(G["rank"]) - 2)
			if int(G["rank"]) < 3:
				body = "Ekip yönetimi [b]Komiser Yardımcısı[/b] rütbesinde açılır."
			else:
				body = "Kadro: %d/%d • Atama: bölge devriyesi (vardiya sonu olay çözer) veya takviye (takip/müzakerede yardım)\n" % [G["team"].size(), slots]
				var assigns: Array = ["takviye"] + C.DIST.keys()
				for ti in G["team"].size():
					var t: Dictionary = G["team"][ti]
					body += "\n• [b]%s[/b] — %s • Yetenek %d • Moral %d • Çözülen %d • Atama: %s" % [t["name"], t["trait"], t["skill"], t["morale"], t["cases"], "Takviye" if t["assign"] == "takviye" else C.DIST[t["assign"]]["n"]]
					row.call("%s: atamayı değiştir" % t["name"], "Değiştir", func(): t["assign"] = assigns[(assigns.find(t["assign"]) + 1) % assigns.size()]; re.call())
					row.call("%s: övgü / eğitim ($150) / uyarı" % t["name"], "Övgü", func(): t["morale"] = mini(100, int(t["morale"]) + 8); re.call())
					row.call("%s: eğitim (+6 yetenek)" % t["name"], "$150", func():
						if int(G["money"]) < 150:
							radio("Yetersiz bakiye.", "bad")
							return
						G["money"] -= 150
						t["skill"] = mini(99, int(t["skill"]) + 6)
						re.call())
					row.call("%s: uyarı ver / görevden al" % t["name"], "Görevden al", func(): G["team"].erase(t); re.call())
				if G["recruits"].is_empty() or int(G["recruit_day"]) != int(G["day"]):
					G["recruit_day"] = G["day"]
					G["recruits"] = []
					for i in 3:
						G["recruits"].append({"name": C.rname(), "trait": C.pick(C.TRAITS), "skill": randi_range(35, 85), "morale": randi_range(55, 85), "integ": randi_range(30, 95), "cases": 0, "assign": "takviye"})
				for r in G["recruits"]:
					var rd: Dictionary = r
					row.call("Başvuru: %s — %s, yetenek %d, sicil: %s" % [r["name"], r["trait"], r["skill"], "temiz" if int(r["integ"]) > 70 else ("orta" if int(r["integ"]) > 45 else "şüpheli")], "İşe al", func(): G["team"].append(rd); G["recruits"].erase(rd); re.call(), G["team"].size() >= slots)
		"premium":
			body = "★ Premium kredi: [b]%d[/b]\n[i]Kredi; A/B performans notu ve başarımlarla kazanılır. Gerçek parayla satın alma Google Play Faturalandırma ile bağlanacak; bu sürümde ödeme alınmaz.[/i]" % G["credits"]
			for l in C.LIVERIES:
				var ld: Dictionary = l
				var has3: bool = G["liveries"].has(l["id"])
				row.call("Araç görünümü: " + l["n"], "Aktif" if G["livery"] == l["id"] else ("Kullan" if has3 else "%d kredi" % l["cr"]), func():
					if not has3:
						if int(G["credits"]) < int(ld["cr"]):
							radio("Kredi yetersiz.", "bad")
							return
						G["credits"] -= ld["cr"]
						G["liveries"].append(ld["id"])
					G["livery"] = ld["id"]
					re.call(), G["livery"] == l["id"])
			row.call("Interceptor GT ★ (en hızlı devriye aracı)", "Sahipsin" if G["premium"].get("gt", false) else "150 kredi", func():
				if int(G["credits"]) < 150:
					radio("Kredi yetersiz.", "bad")
					return
				G["credits"] -= 150
				G["premium"]["gt"] = true
				re.call(), G["premium"].get("gt", false))
			row.call("Hikâye genişletmesi: \"Liman Dosyası\"", "Yakında", Callable(), true)
			row.call("TEST: +100 kredi (geliştirici modu)", "Ekle", func(): G["credits"] += 100; re.call())
		"kayit":
			body = "Toplam görev: %d başarılı / %d başarısız • Gözaltı: %d • Trafik kontrolü: %d\nHikâye aşaması: %d/4%s" % [G["stats"]["done"], G["stats"]["fail"], G["stats"]["arrests"], G["stats"]["stops"], G["story"]["stage"], (" • Son: " + ENDINGS[G["story"]["ending"]][0]) if G["story"]["ending"] != "" else ""]
			row.call("Vücut kamerası kayıtları", "Aç", func(): show_bodycam(re))
			row.call("Oyunu kaydet", "Kaydet", func(): save(); radio("Kaydedildi.", "good"); re.call())
			row.call("Grafik kalitesi: " + M.quality_name(), "Değiştir", func(): M.cycle_quality(); re.call())
			row.call("Ana menü", "Çık", func(): save(); title_screen())
	var extra := func(box: VBoxContainer):
		var tb := HFlowContainer.new()
		for t in tabs:
			var b: Button = M.ui.mk_button(t[1], "pri" if t[0] == hub_tab else "")
			b.custom_minimum_size = Vector2(0, 46)
			var key: String = t[0]
			b.pressed.connect(func(): open_hub(key))
			tb.add_child(b)
		box.add_child(tb)
		box.move_child(tb, 1)
		for r in rows:
			var h := HBoxContainer.new()
			var l: Label = M.ui.mk_label(r[0], 20)
			l.size_flags_horizontal = Control.SIZE_EXPAND_FILL
			h.add_child(l)
			var b2: Button = M.ui.mk_button(r[1])
			b2.custom_minimum_size = Vector2(170, 48)
			b2.disabled = r[3]
			var fn: Callable = r[2]
			if fn.is_valid():
				b2.pressed.connect(func(): fn.call())
			h.add_child(b2)
			box.add_child(h)
	modal("🏠 Kişisel Yaşam", body, [B("😴 Uyu (8 saat)", sleep), B("🚓 Vardiyaya git", briefing, "pri")], extra)

func sleep() -> void:
	var c := comfort()
	G["fatigue"] = maxf(0, float(G["fatigue"]) - c)
	G["hunger"] = minf(100, float(G["hunger"]) + 18)
	G["stress"] = maxf(0, float(G["stress"]) - (15 if G["engaged"] else 8))
	G["hp"] = minf(100, float(G["hp"]) + 20)
	var rent: int = C.HOMES[int(G["home"])]["rent"]
	var msg = "Konfor %d: yorgunluk -%d." % [c, c]
	if rent > 0:
		if int(G["money"]) >= rent:
			G["money"] -= rent
			msg += " Kira -$%d." % rent
		else:
			G["home"] = 0
			msg += " Kirayı ödeyemedin, lojmana taşındın!"
	G["day"] += 1
	G["time"] = 420.0
	if float(G["hunger"]) > 80:
		G["hp"] = maxf(30, float(G["hp"]) - 10)
		msg += " Aç yattın, sağlığın düştü."
	save()
	modal("😴 Uyudun", msg, [B("Devam", func(): open_hub())])

func show_bodycam(back: Callable) -> void:
	var t := ""
	var bl: Array = G["bodycam"].slice(-50)
	bl.reverse()
	for e in bl:
		t += ("[color=#ff6b6b]" if e["f"] else "") + "G%d %s — %s" % [e["d"], e["t"], e["x"]] + ("[/color]" if e["f"] else "") + "\n"
	modal("📹 Vücut Kamerası Kayıtları", t if t != "" else "Kayıt yok", [B("Geri", back)])

func radio_menu() -> void:
	modal("📻 Telsiz", "Merkeze çağrı yap.", [
		B("Görev iste", func(): next_offer = 1.0; radio("Merkez: Anlaşıldı, ilk olayda yönlendireceğiz."), "", mission != null or offer != null),
		B("Aktif görevi iptal et (ceza)", func(): fail(mission, "Görev iptal edildi"), "dark", mission == null),
		B("Durum raporu", open_file),
		B("Kapat")])

func pause_menu() -> void:
	if M.ui.modal_open or not playing():
		return
	modal("⏸ Duraklatıldı", status_text() + "\n\n[font_size=17]Klavye: WASD/oklar sürüş • Boşluk el freni • E etkileşim • F in/bin • Q siren • C kamera • B takviye • I dosya • V görüş[/font_size]", [
		B("Devam", Callable(), "pri"),
		B("🗂 Dosya", open_file),
		B("📹 Kamera kayıtları", func(): show_bodycam(Callable())),
		B("🎨 Grafik: " + M.quality_name(), func(): M.cycle_quality(); pause_menu()),
		B("Vardiyayı bitir (erken ise ceza)", func(): end_shift(float(SH["end_abs"]) - now_abs() > 0), "dark", mission != null or state == "interior")])

# ---------------------------------------------------------------- başlangıç
func title_screen() -> void:
	state = "title"
	G = {}
	SH = null
	M.ui.set_hud_visible(false)
	var sv := load_save()
	var name_edit = [null]
	var extra := func(box: VBoxContainer):
		box.add_child(M.ui.mk_label("Memur adı:", 20))
		var le := LineEdit.new()
		le.text = sv.get("name", "Kerem Aydın")
		le.max_length = 24
		le.add_theme_font_size_override("font_size", 22)
		le.custom_minimum_size = Vector2(0, 50)
		box.add_child(le)
		name_edit[0] = le
	var bt: Array = []
	if not sv.is_empty():
		bt.append(B("▶ Devam et (Gün %d, %s)" % [sv["day"], C.RANKS[int(sv["rank"])]["n"]], func():
			G = sv
			for k in ["outfits", "liveries", "own_homes", "furn", "team", "recruits", "bodycam", "complaints"]:
				if not G.has(k):
					G[k] = []
			M.set_outfit(Color(_outfit_col()))
			open_hub("durum"), "pri"))
	bt.append(B("Yeni kariyer", func():
		var n: String = (name_edit[0] as LineEdit).text.strip_edges() if name_edit[0] != null else "Memur"
		new_game(n if n != "" else "Memur")
		how_to(func(): open_hub("durum")), "" if not sv.is_empty() else "pri"))
	bt.append(B("Nasıl oynanır?", func(): how_to(title_screen)))
	modal("🚓 DEVRİYE — Şehrin Nabzı", "[i]3D polis simülasyonu[/i]\n\nEn alt rütbeden başla. Anonslara yetiş, delil topla, şüphelileri yakala. Her kararın vücut kamerasına kaydediliyor. Dürüst mü kalacaksın, yoksa Kuzgun'un teklifini mi alacaksın?", bt, extra)

func how_to(next: Callable) -> void:
	modal("📘 Nasıl Oynanır", "• [b]Sürüş:[/b] ◀ ▶ direksiyon, GAZ, FREN (durunca geri), EL FRENİ. Ekranı sürükleyerek etrafa bak. GÖRÜŞ: 3. şahıs / 1. şahıs (araç içi) / yukarıdan.\n• [b]Yaya:[/b] sol alttaki joystick ile yürü. İN/BİN ile araçtan in.\n• [b]Anons:[/b] Merkezden gelen görevi KABUL et. Işık sütunu, sarı ok ve harita hedefi gösterir. Süre dolarsa ceza.\n• [b]Durdurma:[/b] SİREN aç ve bir aracın arkasında kal → sağa çeker. Araçtan in, ETKİLEŞİM ile kontrol et. Gerekçesiz arama/gözaltı şikâyete yol açar.\n• [b]Delil:[/b] Olay yerinde delil topla, DOSYA'dan şüphelileri karşılaştır, doğru kişiyi suçla.\n• [b]Kamera:[/b] Vücut kamerası her şeyi kaydeder. Şikâyetlerde kayıt seni korur ya da aleyhine delil olur.\n• [b]Yolsuzluk:[/b] Rüşvet tekliflerinde risk yüzdesi görünür (tanık, kamera, yakındaki polis). Yakalanırsan kesinti, rütbe düşürme, ihraç, tutuklama.\n• [b]Yaşam:[/b] Vardiya arası uyu, ye, ev ve mobilya al. Yorgunluk sürüşü bozar.\n• [b]Kariyer:[/b] XP ile terfi; Komiser Yardımcısı'nda ekip kurarsın. Ana hikâyede Kuzgun örgütünün peşine düşersin — birden fazla son var.", [B("Anladım", next, "pri")])

# ---------------------------------------------------------------- ana döngü
func _process(delta: float) -> void:
	if not playing() or SH == null:
		return
	G["time"] = float(G["time"]) + delta * C.TS
	if float(G["time"]) >= 1440.0:
		G["time"] = float(G["time"]) - 1440.0
		G["day"] += 1
	var hr := delta * C.TS / 60.0
	G["fatigue"] = clampf(float(G["fatigue"]) + hr * 9.0 * (1.4 if float(G["hunger"]) > 80 else 1.0) * (1.2 if float(G["stress"]) > 70 else 1.0), 0, 100)
	G["hunger"] = clampf(float(G["hunger"]) + hr * 6.0, 0, 100)
	if not cam_on:
		SH["cam_off"] += delta
	backup_cd -= delta
	weather_t -= delta
	if weather_t <= 0:
		weather_t = 100 + randf() * 120
		var o: String = M.weather
		_set_weather()
		if o != M.weather:
			radio("Hava durumu: " + WNAME[M.weather])
	if state == "interior":
		upd_mission(delta)
		interact = find_interact()
		_hud(delta)
		return
	M.traffic.update(delta)
	var p := pp()
	if M.mode == "car" and M.siren and stop_target == null and not (mission != null and mission.get("car") != null):
		var tgt = null
		var fw: Vector2 = M.car_fwd()
		for c in M.traffic.cars:
			if c.kind != "civ" or c.keep or c.flee:
				continue
			var d: float = c.pos.distance_to(p)
			if d < 13.0 and (c.pos - p).dot(fw) > 0:
				tgt = c
				break
		if tgt != null:
			stop_timer += delta
			if stop_timer > 1.6:
				stop_target = tgt
				tgt.stopping = true
				tgt.keep = true
				tgt.drv = gen_driver(tgt)
				radio("Araç sağa çekiyor (trafik kontrolü). Araçtan inip konuş.")
				bc("Araç durduruldu: " + tgt.plate)
				stop_timer = 0.0
		else:
			stop_timer = maxf(0.0, stop_timer - delta)
	if stop_target != null and stop_target.mission == null and stop_target.pos.distance_to(p) > 75.0:
		stop_target.stopped = false
		stop_target.stopping = false
		stop_target.keep = false
		stop_target = null
		radio("Durdurulan araç bırakıldı.")
	if M.mode == "car":
		var rr: float = 18.0 * float(C.VEH[M.car_type].get("radar", 1.0))
		for c in M.traffic.cars:
			if c.speeder and not c.radared and c.pos.distance_to(p) < rr:
				c.radared = true
				radio("📡 Radar: %s — %d km/h (limit 120). SİREN aç ve takip et." % [c.plate, roundi(c.base * C.KMH)], "warn")
	if C.district_p(p) == G["region"] and (M.mode == "foot" or M.car_speed() > 2.5):
		SH["patrol"] += delta * C.TS
	upd_mission(delta)
	upd_offers(delta)
	if mission == null and offer == null and randf() < delta * 0.02:
		for c in M.traffic.cars:
			var dd: float = c.pos.distance_to(p)
			if c.kind == "civ" and not c.keep and dd > 40.0 and dd < 100.0:
				var cp: Vector2 = c.pos
				var cdir: int = c.dir
				M.traffic.remove_car(c)
				var am := make_mission("accident", {"given": true, "p": cp, "dir": cdir})
				am["limit"] = 70.0
				am["t"] = 70.0
				radio("Yakınında bir trafik kazası oldu!", "warn")
				show_offer(am)
				break
	chatter_t -= delta
	if chatter_t <= 0:
		chatter_t = 20 + randf() * 25
		var dn: String = C.DIST[C.pick(C.DIST.keys())]["n"]
		radio(C.pick(["Ekip %d: %s bölgesinde şüpheli araç kontrolü." % [randi_range(2, 40), dn], "Merkez: %s bölgesinde kaza ihbarı, ekip yönlendirildi." % dn, "Ekip %d: Olay yerindeyiz, durum kontrol altında." % randi_range(2, 40), "Merkez: Tüm ekiplere, %s bir araç aranıyor." % (C.pick(C.COLORS)[0] as String).to_lower(), "Ekip %d: Mola veriyoruz, 10 dakika." % randi_range(2, 40)]))
	if now_abs() >= float(SH["end_abs"]) and mission == null and not SH["end_warned"]:
		SH["end_warned"] = true
		modal("🕓 Vardiya bitti", "Mesai sona erdi. Karakola dönebilir veya şimdi bitirebilirsin.", [B("Vardiyayı bitir", func(): end_shift(false), "pri"), B("Karakola dönerim")])
		return
	if float(G["hp"]) <= 5:
		radio("Ağır yaralandın! Hastaneye kaldırılıyorsun.", "bad")
		G["hp"] = 50.0
		if mission != null:
			fail(mission, "Memur yaralandı")
		end_shift(true)
		return
	if M.mode == "car" and M.fuel <= 0 and not SH.get("fuel_warned", false):
		SH["fuel_warned"] = true
		radio("Yakıt bitti! Benzinliğe veya karakola git (araçtan inip yürüyebilirsin).", "bad")
	interact = find_interact()
	_hud(delta)

func _hud(delta: float) -> void:
	hud_t -= delta
	if hud_t > 0:
		return
	hud_t = 0.1
	var p := pp()
	var info = "%s\n$%d • ★%d\n%s • Gün %d • %s\n%s%s" % [C.RANKS[int(G["rank"])]["n"], G["money"], G["credits"], C.fmt_clock(G["time"]), G["day"], WNAME[M.weather], dist_n(C.district_p(p)), " ✓" if C.district_p(p) == G["region"] else ""]
	var mt := ""
	if mission != null:
		mt = "#%d %s\n%s\n%s%.0f m" % [mission["id"], mission["n"], obj_text(mission), "" if mission["no_timer"] else ("⏱ " + C.fmt_t(mission["t"]) + " • "), p.distance_to(mission["p"])]
	elif stop_target != null:
		mt = "Trafik kontrolü\nAraçtan in ve sürücüyle konuş"
	elif now_abs() >= float(SH["end_abs"]):
		mt = "Vardiya bitti\nKarakola dön (mavi nokta)"
	M.ui.update_hud(info, mt, cam_on, interact["label"] if interact != null else "")
