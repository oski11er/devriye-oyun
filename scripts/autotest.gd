extends Node
## CI otomatik oynanış testi: tüm sistemleri dener, ekran görüntüsü alır, sonucu loglar.

var M
var g
var dir := ""
var ok_n := 0

func run(main) -> void:
	M = main
	g = M.game
	process_mode = Node.PROCESS_MODE_ALWAYS
	dir = ProjectSettings.globalize_path("res://shots")
	DirAccess.make_dir_recursive_absolute(dir)
	_go()

func w(s: float) -> void:
	await get_tree().create_timer(s, true).timeout

func shot(n: String) -> void:
	if DisplayServer.get_name() == "headless":
		print("SHOT ", n, " (headless, atlandı) blocks=", M.city.loaded_count(), " cars=", M.traffic.cars.size())
		return
	await RenderingServer.frame_post_draw
	await RenderingServer.frame_post_draw
	get_viewport().get_texture().get_image().save_png(dir + "/" + n + ".png")
	print("SHOT ", n, " draw_calls=", Performance.get_monitor(Performance.RENDER_TOTAL_DRAW_CALLS_IN_FRAME), " fps=", Engine.get_frames_per_second(), " blocks=", M.city.loaded_count(), " cars=", M.traffic.cars.size(), " peds=", M.traffic.peds.size())

func step(n: String) -> void:
	ok_n += 1
	print("TEST_OK ", ok_n, " ", n)

func click(sub: String) -> bool:
	for b in _buttons(M.ui.modal_box):
		if (b as Button).text.findn(sub) >= 0 and not (b as Button).disabled:
			(b as Button).pressed.emit()
			return true
	print("TEST_WARN buton yok: ", sub, " | mevcut: ", _btn_names())
	return false

func _buttons(n: Node) -> Array:
	var out: Array = []
	for c in n.get_children():
		if c is Button and not (c is OptionButton) and not (c is CheckBox):
			out.append(c)
		out.append_array(_buttons(c))
	return out

func _btn_names() -> String:
	var s := ""
	for b in _buttons(M.ui.modal_box):
		s += (b as Button).text.substr(0, 24) + " | "
	return s

func title() -> String:
	if not M.ui.modal_open:
		return ""
	for c in M.ui.modal_box.get_children():
		if c is Label:
			return (c as Label).text
	return ""

func tp(p: Vector2) -> void:
	if M.mode == "car":
		M.car.global_transform = Transform3D(Basis(Vector3.UP, M.car.rotation.y), Vector3(p.x, 0.6, p.y))
		M.car.linear_velocity = Vector3.ZERO
	else:
		M.player.global_position = Vector3(p.x, 0.4, p.y)
	M.city.update_stream(p, 0.0, true)

func foot() -> void:
	if M.mode == "car":
		M.car.linear_velocity = Vector3.ZERO
		M.toggle_enter()

func car_mode() -> void:
	if M.mode != "car":
		M.player.global_position = M.car.global_position + Vector3(1.5, 0.3, 0)
		M.set_mode("car")

func new_mission(t: String, o: Dictionary = {}) -> Dictionary:
	if M.ui.modal_open:
		M.ui.close_modal()
	if g.mission != null:
		g.cleanup(g.mission)
		g.mission = null
	var m: Dictionary = g.make_mission(t, o)
	g.mission = m
	return m

func _go() -> void:
	await w(1.5)
	await shot("00_baslik")
	# yeni oyun
	g.new_game("Test Memur")
	g.open_hub("durum")
	await w(0.5)
	await shot("01_yasam_durum")
	for t in ["ev", "market", "sosyal", "garaj", "kiyafet", "ekip", "premium", "kayit"]:
		g.open_hub(t)
		await w(0.1)
	step("hub sekmeleri")
	g.G["rank"] = 4
	g.G["team"].append({"name": "Ece Kaya", "trait": "Taktik", "skill": 80, "morale": 70, "integ": 80, "cases": 0, "assign": "takviye"})
	g.G["team"].append({"name": "Can Demir", "trait": "Dedektif", "skill": 60, "morale": 70, "integ": 50, "cases": 0, "assign": "merkez"})
	g.open_hub("ekip")
	await w(0.2)
	g.briefing()
	await w(0.3)
	await shot("02_brifing")
	click("Devriyeye")
	await w(2.0)
	M.weather = "clear"
	g.G["time"] = 13 * 60.0
	step("vardiya başladı")
	await w(1.5)
	await shot("03_surus_3sahis")
	# sürüş testi
	var p0: Vector3 = M.car.global_position
	Input.action_press("gas")
	await w(2.5)
	Input.action_release("gas")
	print("DRIVE move=", M.car.global_position - p0, " kmh=", M.car_speed() * 3.6)
	Input.action_press("steer_left")
	Input.action_press("gas")
	await w(1.0)
	Input.action_release("steer_left")
	Input.action_release("gas")
	Input.action_press("brake")
	await w(1.5)
	Input.action_release("brake")
	step("sürüş")
	M.view = "fps"
	await w(0.6)
	await shot("04_surus_1sahis")
	M.view = "tps"
	# trafik kontrolü (proaktif)
	await w(0.5)
	var civ = null
	for c in M.traffic.cars:
		if c.kind == "civ":
			civ = c
			break
	if civ != null:
		civ.stopped = true
		civ.keep = true
		g.stop_target = civ
		g.traffic_stop(civ, null)
		await w(0.3)
		await shot("05_trafik_kontrolu")
		click("Ehliyet")
		await w(0.1)
		click("Alkol")
		await w(0.1)
		click("Ceza yaz")
		await w(0.3)
		step("trafik kontrolü")
	# hız ihlali
	var m := new_mission("speed")
	tp((m["p"] as Vector2) + Vector2(-25, 3.2))
	await w(1.0)
	if m.get("car") != null:
		var c = m["car"]
		M.set_siren(true)
		tp(c.pos - Vector2(8, 0))
		m["pull"] = 2.0
		await w(1.5)
		if m["stage"] == "chase":
			c.hp = 0
			await w(0.5)
		print("speed stage=", m["stage"])
		if m["stage"] == "stop":
			c.stopped = true
			g.traffic_stop(c, m)
			click("Ehliyet")
			await w(0.1)
			click("Ceza yaz")
			await w(0.2)
			if title().begins_with("📝"):
				await shot("06_rapor")
				click("Raporu gönder")
				await w(0.2)
				click("Devriyeye")
	M.set_siren(false)
	step("hız ihlali")
	# kaza
	m = new_mission("accident")
	tp((m["p"] as Vector2) + Vector2(10, 0))
	await w(1.0)
	foot()
	M.player.global_position = Vector3(m["p"].x + 3, 0.4, m["p"].y + 3)
	await w(0.8)
	await shot("07_kaza_yeri")
	g.accident_dialog(m)
	for s in ["Alanı", "Sürücü A", "Sürücü B", "fotoğrafla", "kamerası"]:
		click(s)
		await w(0.1)
	click("Kusurlu: Sürücü " + ["A", "B"][m["guilty"]])
	await w(0.1)
	click("Çekici")
	await w(0.2)
	click("Raporu gönder")
	await w(0.1)
	click("Devriyeye")
	step("kaza")
	# kavga
	m = new_mission("fight")
	M.player.global_position = Vector3(m["p"].x + 4, 0.4, m["p"].y + 2)
	M.city.update_stream(m["p"], 0.0, true)
	await w(1.2)
	await shot("08_kavga")
	g.fight_dialog(m)
	click("Biber")
	await w(0.1)
	click("Tanıkları")
	await w(0.1)
	click("Gözaltı: " + m["cl"][m["inst"]][0])
	await w(0.2)
	click("Raporu gönder")
	await w(0.1)
	click("Devriyeye")
	step("kavga")
	# hırsızlık
	m = new_mission("burglary")
	M.player.global_position = Vector3(m["door"]["p"].x, 0.4, m["door"]["p"].y + float(m["door"]["out"]) * 1.5)
	M.city.update_stream(m["door"]["p"], 0.0, true)
	await w(0.8)
	g.enter_scene(m)
	await w(1.0)
	await shot("09_olay_yeri_ic")
	for sp in g.int_spots:
		g._use_spot(sp)
	g.exit_interior()
	await w(0.3)
	g.open_file()
	await w(0.2)
	await shot("10_dosya")
	click("Suçla: " + m["g"]["name"])
	await w(0.3)
	print("burglary stage=", m["stage"])
	M.player.global_position = Vector3(m["p"].x, 0.4, m["p"].y + float(m["door"]["out"]) * 2.0)
	M.city.update_stream(m["p"], 0.0, true)
	await w(1.0)
	if m.get("sus") != null:
		m["sus"].ai = "idle"
		m["sus"].warned = true
		M.player.global_position = Vector3(m["sus"].pos.x + 1.0, 0.4, m["sus"].pos.y)
		await w(0.6)
		print("arrest modal=", title())
		await shot("11_yakalama")
		for i in 6:
			if title().begins_with("📝") or not M.ui.modal_open:
				break
			if not click("Yere yat"):
				if not click("Ekip otosuna"):
					click("Kelepçele")
			await w(0.15)
		if title().begins_with("📝"):
			click("Raporu gönder")
			await w(0.1)
			click("Devriyeye")
	step("hırsızlık")
	# soygun + takip + takviye
	car_mode()
	m = new_mission("robbery")
	tp((m["p"] as Vector2) + Vector2(-30, 0))
	await w(1.5)
	print("robbery stage=", m["stage"])
	g.call_backup()
	await w(1.5)
	await shot("12_takip_takviye")
	if m.get("car") != null:
		m["car"].hp = 0.0
		await w(0.5)
	print("robbery stage2=", m["stage"])
	if m.get("sus") != null:
		foot()
		m["sus"].ai = "idle"
		M.player.global_position = Vector3(m["sus"].pos.x + 1.0, 0.4, m["sus"].pos.y)
		await w(0.6)
		click("Elektroşok")
		await w(0.1)
		click("Kelepçele")
		await w(0.1)
		click("Ekip otosuna")
		await w(0.2)
		click("Raporu gönder")
		await w(0.1)
		click("Devriyeye")
	step("soygun")
	# gizli operasyon
	car_mode()
	m = new_mission("undercover")
	tp((m["p"] as Vector2) + Vector2(15, 0))
	await w(1.0)
	for i in 3:
		m["cd"] = 0.0
		g._photo(m)
	await w(0.2)
	click("Raporu gönder")
	await w(0.1)
	click("Devriyeye")
	step("gizli operasyon")
	# rehine
	m = new_mission("hostage")
	g._start_neg(m)
	for i in 4:
		if not click("Empati"):
			click("baskını")
		await w(0.1)
	click("Raporu gönder")
	await w(0.1)
	click("Devriyeye")
	step("rehine")
	# kaçan şüpheli + devriye + muhbir
	m = new_mission("patrol")
	tp(m["cps"][0])
	await w(0.5)
	tp(m["cps"][1])
	await w(0.5)
	tp(m["cps"][2])
	await w(0.5)
	click("Raporu gönder")
	await w(0.1)
	click("Devriyeye")
	step("devriye")
	m = new_mission("meet", {"story": "meet"})
	g.meet_dialog(m)
	click("Resmi muhbir")
	await w(0.2)
	click("Raporu gönder")
	await w(0.1)
	click("Devriyeye")
	await w(0.1)
	click("Devam")
	step("muhbir")
	# yolsuzluk denemesi
	g.corrupt_act("Test rüşvet", 100, 0.9, 2)
	step("yolsuzluk")
	# gece + yağmur + yaya görünümleri
	if M.ui.modal_open:
		M.ui.close_modal()
	car_mode()
	tp(Vector2(20, 67.5))
	M.car.rotation.y = PI / 2
	M.cam_free = 0.0
	g.G["time"] = 21.5 * 60.0
	M.set_siren(true)
	await w(2.0)
	await shot("13_gece_siren")
	g.G["time"] = 17.5 * 60.0
	M.weather = "rain"
	M.set_siren(false)
	await w(2.0)
	await shot("14_yagmur")
	M.weather = "clear"
	g.G["time"] = 11 * 60.0
	foot()
	M.cam_yaw = PI * 0.2
	await w(1.0)
	await shot("15_yaya_3sahis")
	M.view = "fps"
	await w(0.5)
	await shot("16_yaya_1sahis")
	M.view = "top"
	await w(1.0)
	await shot("17_yukaridan")
	M.view = "tps"
	car_mode()
	# vardiya sonu
	if g.mission != null:
		g.cleanup(g.mission)
		g.mission = null
	g.end_shift(false)
	await w(0.4)
	await shot("18_vardiya_sonu")
	click("Eve dön")
	if title() == "":
		click("Devam")
	await w(0.3)
	g.sleep()
	await w(0.2)
	click("Devam")
	step("vardiya sonu + uyku")
	# hikâye: ihanet + plan + son
	g.G["story"]["stage"] = 3
	g.G["rank"] = 3
	g.briefing()
	await w(0.2)
	click("Reddet")
	await w(0.1)
	click("Devam")
	await w(0.1)
	click("Şafak")
	await w(0.2)
	print("story planned=", g.G["story"]["planned"], " title=", title())
	g.ending("hero")
	await w(0.3)
	await shot("19_son")
	step("hikâye")
	g.save()
	print("TESTS_DONE ok=", ok_n)
	get_tree().quit()
