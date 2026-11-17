extends CanvasLayer
## Arayüz: HUD, mini harita, butonlar (çoklu dokunma), joystick, pencereler, anons kartı

var M
var root: Control
var hud: Control
var btn_root: Node2D
var btns := {}
var info: Label
var speed_l: Label
var rec_l: Label
var mcard: PanelContainer
var mcard_l: Label
var toasts: VBoxContainer
var offer: PanelContainer
var offer_rt: RichTextLabel
var offer_acc: Button
var minimap: Control
var bars := {}
var vign: TextureRect
var siren_l: ColorRect
var siren_r: ColorRect
var act_label: Label
var modal_layer: Control
var modal_box: VBoxContainer
var modal_open := false
var joy_id := -1
var joy_origin := Vector2.ZERO
var look_id := -1
var look_last := Vector2.ZERO
var joy_ctrl: Control
var fs := 26

func setup(main) -> void:
	M = main
	layer = 10
	process_mode = Node.PROCESS_MODE_ALWAYS
	root = Control.new()
	root.set_anchors_preset(Control.PRESET_FULL_RECT)
	root.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(root)
	_build_hud()
	_build_buttons()
	_build_modal()
	get_viewport().size_changed.connect(_layout)
	_layout()
	set_hud_visible(false)

func vs() -> Vector2:
	return get_viewport().get_visible_rect().size

func _panel_style(col: Color = Color(0.03, 0.06, 0.12, 0.78), rad: int = 12, border: Color = Color(0.45, 0.6, 0.9, 0.35)) -> StyleBoxFlat:
	var sb := StyleBoxFlat.new()
	sb.bg_color = col
	sb.set_corner_radius_all(rad)
	sb.set_border_width_all(2)
	sb.border_color = border
	sb.content_margin_left = 14
	sb.content_margin_right = 14
	sb.content_margin_top = 10
	sb.content_margin_bottom = 10
	return sb

func _label(sz: int, col: Color = Color.WHITE) -> Label:
	var l := Label.new()
	l.add_theme_font_size_override("font_size", sz)
	l.add_theme_color_override("font_color", col)
	l.add_theme_color_override("font_outline_color", Color(0, 0, 0))
	l.add_theme_constant_override("outline_size", 6)
	l.mouse_filter = Control.MOUSE_FILTER_IGNORE
	return l

# ---------------------------------------------------------------- HUD
func _build_hud() -> void:
	hud = Control.new()
	hud.set_anchors_preset(Control.PRESET_FULL_RECT)
	hud.mouse_filter = Control.MOUSE_FILTER_IGNORE
	root.add_child(hud)
	vign = TextureRect.new()
	var gt := GradientTexture2D.new()
	var gr := Gradient.new()
	gr.set_color(0, Color(0, 0, 0, 0))
	gr.set_color(1, Color(0, 0, 0, 1))
	gr.add_point(0.55, Color(0, 0, 0, 0))
	gt.gradient = gr
	gt.fill = GradientTexture2D.FILL_RADIAL
	gt.fill_from = Vector2(0.5, 0.5)
	gt.fill_to = Vector2(1.0, 0.5)
	vign.texture = gt
	vign.stretch_mode = TextureRect.STRETCH_SCALE
	vign.set_anchors_preset(Control.PRESET_FULL_RECT)
	vign.mouse_filter = Control.MOUSE_FILTER_IGNORE
	vign.modulate.a = 0.0
	hud.add_child(vign)
	siren_l = ColorRect.new()
	siren_r = ColorRect.new()
	for s in [siren_l, siren_r]:
		s.mouse_filter = Control.MOUSE_FILTER_IGNORE
		s.color = Color(1, 0, 0, 0)
		hud.add_child(s)
	var ip := PanelContainer.new()
	ip.add_theme_stylebox_override("panel", _panel_style())
	ip.position = Vector2(16, 14)
	ip.mouse_filter = Control.MOUSE_FILTER_IGNORE
	info = _label(22)
	ip.add_child(info)
	hud.add_child(ip)
	var bb := VBoxContainer.new()
	bb.position = Vector2(18, 160)
	bb.mouse_filter = Control.MOUSE_FILTER_IGNORE
	for k in ["YAKIT", "HASAR", "LASTİK", "YORGUNLUK", "SAĞLIK"]:
		var h := HBoxContainer.new()
		h.mouse_filter = Control.MOUSE_FILTER_IGNORE
		var l := _label(15)
		l.text = k
		l.custom_minimum_size = Vector2(110, 0)
		h.add_child(l)
		var bg := ColorRect.new()
		bg.color = Color(0, 0, 0, 0.45)
		bg.custom_minimum_size = Vector2(120, 10)
		bg.mouse_filter = Control.MOUSE_FILTER_IGNORE
		var fg := ColorRect.new()
		fg.color = Color(0.3, 0.8, 0.4)
		fg.size = Vector2(120, 10)
		fg.mouse_filter = Control.MOUSE_FILTER_IGNORE
		bg.add_child(fg)
		h.add_child(bg)
		bb.add_child(h)
		bars[k] = fg
	hud.add_child(bb)
	speed_l = _label(40)
	speed_l.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	hud.add_child(speed_l)
	rec_l = _label(20, Color(1, 0.3, 0.3))
	hud.add_child(rec_l)
	mcard = PanelContainer.new()
	mcard.add_theme_stylebox_override("panel", _panel_style())
	mcard.mouse_filter = Control.MOUSE_FILTER_IGNORE
	mcard_l = _label(21)
	mcard_l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	mcard.add_child(mcard_l)
	hud.add_child(mcard)
	toasts = VBoxContainer.new()
	toasts.mouse_filter = Control.MOUSE_FILTER_IGNORE
	toasts.add_theme_constant_override("separation", 6)
	hud.add_child(toasts)
	minimap = Control.new()
	minimap.custom_minimum_size = Vector2(210, 210)
	minimap.size = Vector2(210, 210)
	minimap.mouse_filter = Control.MOUSE_FILTER_IGNORE
	minimap.draw.connect(_draw_mini)
	hud.add_child(minimap)
	act_label = _label(24, Color(0.5, 1, 0.7))
	act_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	hud.add_child(act_label)
	joy_ctrl = Control.new()
	joy_ctrl.set_anchors_preset(Control.PRESET_FULL_RECT)
	joy_ctrl.mouse_filter = Control.MOUSE_FILTER_IGNORE
	joy_ctrl.draw.connect(_draw_joy)
	hud.add_child(joy_ctrl)
	offer = PanelContainer.new()
	offer.add_theme_stylebox_override("panel", _panel_style(Color(0.04, 0.07, 0.14, 0.95), 14, Color(0.4, 0.55, 0.9, 0.8)))
	var ov := VBoxContainer.new()
	offer_rt = RichTextLabel.new()
	offer_rt.bbcode_enabled = true
	offer_rt.fit_content = true
	offer_rt.scroll_active = false
	offer_rt.custom_minimum_size = Vector2(560, 0)
	offer_rt.add_theme_font_size_override("normal_font_size", 20)
	offer_rt.add_theme_font_size_override("bold_font_size", 22)
	ov.add_child(offer_rt)
	var oh := HBoxContainer.new()
	offer_acc = _mk_button("KABUL", "good")
	offer_acc.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	offer_acc.pressed.connect(func(): M.game.accept_offer())
	var rej := _mk_button("REDDET", "dark")
	rej.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	rej.pressed.connect(func(): M.game.decline_offer(false))
	oh.add_child(offer_acc)
	oh.add_child(rej)
	ov.add_child(oh)
	offer.add_child(ov)
	offer.visible = false
	hud.add_child(offer)

func _draw_mini() -> void:
	if M.game == null or M.game.G.size() == 0:
		return
	var sz := minimap.size
	var c := sz * 0.5
	var r := sz.x * 0.5
	minimap.draw_circle(c, r, Color(0.04, 0.07, 0.12, 0.85))
	var pp: Vector2 = M.player_pos()
	var sc := r / 230.0
	var rot: float = -float(M.player_yaw()) + PI
	var tf := func(w: Vector2) -> Vector2:
		var d := (w - pp) * sc
		return c + d.rotated(rot)
	var k0 := floori((pp.x - 260) / C.S)
	var k1 := ceili((pp.x + 260) / C.S)
	var j0 := floori((pp.y - 260) / C.S)
	var j1 := ceili((pp.y + 260) / C.S)
	for k in range(maxi(k0, -C.WB), mini(k1, C.WB) + 1):
		var a: Vector2 = tf.call(Vector2(k * C.S, maxf(pp.y - 260, -C.LIM)))
		var b: Vector2 = tf.call(Vector2(k * C.S, minf(pp.y + 260, C.LIM)))
		minimap.draw_line(a, b, Color(0.55, 0.6, 0.68), C.RW * sc)
	for j in range(maxi(j0, -C.WB), mini(j1, C.WB) + 1):
		var a2: Vector2 = tf.call(Vector2(maxf(pp.x - 260, -C.LIM), j * C.S))
		var b2: Vector2 = tf.call(Vector2(minf(pp.x + 260, C.LIM), j * C.S))
		minimap.draw_line(a2, b2, Color(0.85, 0.7, 0.3) if j == 0 else Color(0.55, 0.6, 0.68), C.hh(j) * 2.0 * sc)
	var kd: Dictionary = M.city.karakol_door()
	var kp: Vector2 = tf.call(kd["p"])
	if kp.distance_to(c) < r:
		minimap.draw_circle(kp, 6, Color(0.3, 0.6, 1))
	for car in M.traffic.cars:
		var q: Vector2 = tf.call(car.pos)
		if q.distance_to(c) > r:
			continue
		if car.kind == "cop":
			minimap.draw_circle(q, 3.5, Color(0.3, 0.6, 1))
		elif car.flee:
			minimap.draw_circle(q, 6, Color(1, 0.2, 0.2))
	var m = M.game.mission
	if m != null:
		var mp: Vector2 = tf.call(m["p"])
		var dv := mp - c
		if dv.length() > r - 8:
			mp = c + dv.normalized() * (r - 8)
		var blink := int(Time.get_ticks_msec() / 300) % 2 == 0
		minimap.draw_circle(mp, 8, Color(1, 0.82, 0.2) if blink else Color(1, 0.45, 0.1))
	minimap.draw_colored_polygon(PackedVector2Array([c + Vector2(0, -10), c + Vector2(-7, 8), c + Vector2(7, 8)]), Color.WHITE)
	minimap.draw_arc(c, r, 0, TAU, 64, Color(0.5, 0.65, 0.95, 0.7), 3)

func _draw_joy() -> void:
	if M.mode == "car" or not hud.visible:
		return
	var v := vs()
	var c := joy_origin if joy_id >= 0 else Vector2(150, v.y - 150)
	joy_ctrl.draw_circle(c, 85, Color(1, 1, 1, 0.12))
	joy_ctrl.draw_arc(c, 85, 0, TAU, 48, Color(1, 1, 1, 0.4), 3)
	joy_ctrl.draw_circle(c + M.joy_vec * 60, 36, Color(0.5, 0.7, 1, 0.55))

# ---------------------------------------------------------------- butonlar
func _btn_tex(size: Vector2i, col: Color) -> ImageTexture:
	var img := Image.create(size.x, size.y, false, Image.FORMAT_RGBA8)
	var r := 18.0
	for y in size.y:
		for x in size.x:
			var dx := maxf(maxf(r - x, x - (size.x - 1 - r)), 0.0)
			var dy := maxf(maxf(r - y, y - (size.y - 1 - r)), 0.0)
			var d := sqrt(dx * dx + dy * dy)
			if d <= r:
				img.set_pixel(x, y, Color(1, 1, 1, 0.55) if r - d < 3.0 else col)
	return ImageTexture.create_from_image(img)

func _mk_tbtn(id: String, label: String, action: String, size: Vector2i, col: Color, fsz: int) -> TouchScreenButton:
	var b := TouchScreenButton.new()
	b.texture_normal = _btn_tex(size, col)
	b.texture_pressed = _btn_tex(size, col.lightened(0.35))
	if action != "":
		b.action = action
	var l := Label.new()
	l.text = label
	l.size = Vector2(size)
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	l.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	l.add_theme_font_size_override("font_size", fsz)
	l.add_theme_color_override("font_outline_color", Color(0, 0, 0))
	l.add_theme_constant_override("outline_size", 6)
	l.mouse_filter = Control.MOUSE_FILTER_IGNORE
	b.add_child(l)
	b.set_meta("size", Vector2(size))
	btn_root.add_child(b)
	btns[id] = b
	return b

func _build_buttons() -> void:
	btn_root = Node2D.new()
	add_child(btn_root)
	var dark := Color(0.06, 0.1, 0.18, 0.62)
	_mk_tbtn("left", "◀", "steer_left", Vector2i(150, 150), dark, 64)
	_mk_tbtn("right", "▶", "steer_right", Vector2i(150, 150), dark, 64)
	_mk_tbtn("gas", "GAZ\n▲", "gas", Vector2i(150, 200), Color(0.08, 0.38, 0.16, 0.7), 32)
	_mk_tbtn("brake", "FREN\n▼", "brake", Vector2i(150, 140), Color(0.45, 0.08, 0.08, 0.7), 28)
	_mk_tbtn("hand", "EL\nFRENİ", "hand", Vector2i(110, 90), dark, 20)
	_mk_tbtn("act", "ETKİLEŞİM", "", Vector2i(250, 84), Color(0.1, 0.42, 0.22, 0.8), 24).pressed.connect(func(): M.game.tap("act"))
	_mk_tbtn("enter", "İN", "", Vector2i(120, 72), dark, 24).pressed.connect(func(): M.game.tap("enter"))
	var small = [["siren", "SİREN"], ["horn", "KORNA"], ["lights", "FAR"], ["cam", "KAMERA"], ["backup", "TAKVİYE"], ["file", "DOSYA"], ["radio", "TELSİZ"], ["view", "GÖRÜŞ"]]
	for s in small:
		var id: String = s[0]
		var b = _mk_tbtn(id, s[1], "horn" if id == "horn" else "", Vector2i(104, 54), dark, 18)
		if id != "horn":
			b.pressed.connect(func(): M.game.tap(id))
	_mk_tbtn("pause", "II", "", Vector2i(64, 54), dark, 24).pressed.connect(func(): M.game.pause_menu())

func _layout() -> void:
	var v := vs()
	var m := 22.0
	btns["left"].position = Vector2(m, v.y - 150 - m)
	btns["right"].position = Vector2(m + 170, v.y - 150 - m)
	btns["gas"].position = Vector2(v.x - 150 - m, v.y - 200 - m)
	btns["brake"].position = Vector2(v.x - 320 - m, v.y - 140 - m)
	btns["hand"].position = Vector2(v.x - 300 - m, v.y - 260 - m)
	btns["act"].position = Vector2(v.x * 0.5 - 125, v.y - 84 - m)
	btns["enter"].position = Vector2(v.x * 0.5 + 140, v.y - 76 - m)
	var ids = ["siren", "horn", "lights", "cam", "backup", "file", "radio", "view"]
	for i in ids.size():
		var col := i % 2
		var row := i / 2
		btns[ids[i]].position = Vector2(v.x - 224 - m + col * 112, 250 + row * 62)
	btns["pause"].position = Vector2(v.x - 300 - m, m)
	minimap.position = Vector2(v.x - 210 - m, m)
	speed_l.position = Vector2(v.x - 420 - m, 200)
	speed_l.size = Vector2(170, 50)
	rec_l.position = Vector2(v.x - 420 - m, m + 6)
	mcard.position = Vector2(v.x * 0.5 - 300, m)
	mcard.custom_minimum_size = Vector2(600, 0)
	toasts.position = Vector2(m, v.y * 0.42)
	toasts.custom_minimum_size = Vector2(minf(560, v.x * 0.42), 0)
	offer.position = Vector2(v.x * 0.5 - 300, 110)
	act_label.position = Vector2(v.x * 0.5 - 300, v.y - 150)
	act_label.size = Vector2(600, 40)
	siren_l.position = Vector2.ZERO
	siren_l.size = Vector2(v.x * 0.12, v.y)
	siren_r.position = Vector2(v.x * 0.88, 0)
	siren_r.size = Vector2(v.x * 0.12, v.y)

func on_mode() -> void:
	var in_car: bool = M.mode == "car"
	for id in ["left", "right", "gas", "brake", "hand", "siren", "horn", "lights"]:
		btns[id].visible = in_car
	(btns["enter"].get_child(0) as Label).text = "İN" if in_car else "BİN"
	btns["enter"].visible = M.mode != "interior"
	joy_ctrl.queue_redraw()

func set_hud_visible(on: bool) -> void:
	hud.visible = on
	btn_root.visible = on and not modal_open
	if on:
		on_mode()

func _btn_hit(p: Vector2) -> bool:
	if not btn_root.visible:
		return false
	for b in btns.values():
		if not b.visible:
			continue
		if Rect2(b.position, b.get_meta("size")).has_point(p):
			return true
	return false

func _input(e: InputEvent) -> void:
	if modal_open or not hud.visible:
		return
	if e is InputEventScreenTouch:
		var t := e as InputEventScreenTouch
		if t.pressed:
			if _btn_hit(t.position) or (offer.visible and offer.get_global_rect().has_point(t.position)):
				return
			if M.mode != "car" and t.position.x < vs().x * 0.4 and joy_id < 0:
				joy_id = t.index
				joy_origin = t.position
				M.joy_vec = Vector2.ZERO
			elif look_id < 0:
				look_id = t.index
				look_last = t.position
		else:
			if t.index == joy_id:
				joy_id = -1
				M.joy_vec = Vector2.ZERO
			if t.index == look_id:
				look_id = -1
	elif e is InputEventScreenDrag:
		var d := e as InputEventScreenDrag
		if d.index == joy_id:
			M.joy_vec = ((d.position - joy_origin) / 70.0).limit_length(1.0)
		elif d.index == look_id:
			var dl = d.position - look_last
			look_last = d.position
			M.cam_yaw -= dl.x * 0.006
			M.cam_pitch = clampf(M.cam_pitch - dl.y * 0.004, -1.2, 0.6)
			M.cam_free = 2.5
	elif e is InputEventKey and e.pressed and not e.echo:
		match (e as InputEventKey).physical_keycode:
			KEY_E: M.game.tap("act")
			KEY_F: M.game.tap("enter")
			KEY_Q: M.game.tap("siren")
			KEY_L: M.game.tap("lights")
			KEY_C: M.game.tap("cam")
			KEY_B: M.game.tap("backup")
			KEY_I: M.game.tap("file")
			KEY_R: M.game.tap("radio")
			KEY_V: M.game.tap("view")
			KEY_ESCAPE: M.game.pause_menu()

# ---------------------------------------------------------------- HUD güncelle
func update_hud(text_info: String, mission_text: String, rec: bool, interact: String) -> void:
	info.text = text_info
	mcard.visible = mission_text != ""
	mcard_l.text = mission_text
	rec_l.text = ("● REC" if int(Time.get_ticks_msec() / 600) % 2 == 0 else "○ REC") if rec else "KAMERA KAPALI"
	rec_l.add_theme_color_override("font_color", Color(1, 0.3, 0.3) if rec else Color(0.6, 0.6, 0.6))
	speed_l.text = ("%d km/h" % roundi(M.car_speed() * C.KMH)) if M.mode == "car" else ""
	var G: Dictionary = M.game.G
	_bar("YAKIT", M.fuel, false)
	_bar("HASAR", M.dmg, true)
	_bar("LASTİK", M.tire, false)
	_bar("YORGUNLUK", G["fatigue"], true)
	_bar("SAĞLIK", G["hp"], false)
	act_label.text = ("▶ " + interact) if interact != "" else ""
	(btns["act"].get_child(0) as Label).text = interact.substr(0, 20) if interact != "" else "ETKİLEŞİM"
	btns["act"].modulate.a = 1.0 if interact != "" else 0.5
	var f: float = G["fatigue"]
	vign.modulate.a = clampf((f - 60.0) / 40.0, 0.0, 1.0) * (0.6 + sin(Time.get_ticks_msec() / 900.0) * 0.1)
	var fl := int(Time.get_ticks_msec() / 140) % 2 == 0
	siren_l.color = Color(1, 0.1, 0.1, 0.16 if (M.siren and fl) else 0.0)
	siren_r.color = Color(0.2, 0.35, 1, 0.16 if (M.siren and not fl) else 0.0)
	minimap.queue_redraw()
	if M.mode != "car":
		joy_ctrl.queue_redraw()

func _bar(k: String, v: float, inv: bool) -> void:
	var fg: ColorRect = bars[k]
	fg.size.x = 120.0 * clampf(v, 0, 100) / 100.0
	var g := (100.0 - v) if inv else v
	fg.color = Color(0.3, 0.8, 0.4) if g > 60 else (Color(1, 0.7, 0.15) if g > 30 else Color(1, 0.3, 0.3))

func toast(msg: String, kind: String = "") -> void:
	var p := PanelContainer.new()
	var col := Color(0.5, 0.7, 1)
	match kind:
		"bad": col = Color(1, 0.35, 0.35)
		"good": col = Color(0.35, 0.9, 0.5)
		"warn": col = Color(1, 0.7, 0.2)
	var sb := _panel_style(Color(0.03, 0.06, 0.12, 0.88), 8, col)
	sb.border_width_left = 5
	p.add_theme_stylebox_override("panel", sb)
	p.mouse_filter = Control.MOUSE_FILTER_IGNORE
	var l := _label(19)
	l.text = msg
	l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	l.custom_minimum_size = Vector2(minf(540, vs().x * 0.4), 0)
	p.add_child(l)
	toasts.add_child(p)
	while toasts.get_child_count() > 4:
		toasts.get_child(0).free()
	get_tree().create_timer(6.5, true).timeout.connect(_kill.bind(p))

func _kill(n: Node) -> void:
	if is_instance_valid(n):
		n.queue_free()

func show_offer(bb: String) -> void:
	offer_rt.text = bb
	offer.visible = true
	M.beep()

func set_offer_timer(s: float) -> void:
	offer_acc.text = "KABUL (%d)" % ceili(s)

func hide_offer() -> void:
	offer.visible = false

# ---------------------------------------------------------------- pencere (modal)
func _mk_button(text: String, kind: String = "") -> Button:
	var b := Button.new()
	b.text = text
	b.add_theme_font_size_override("font_size", 22)
	b.custom_minimum_size = Vector2(0, 58)
	b.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	var col := Color(0.1, 0.17, 0.3)
	var brd := Color(0.25, 0.38, 0.6)
	match kind:
		"good":
			col = Color(0.1, 0.38, 0.2)
			brd = Color(0.2, 0.6, 0.35)
		"dark":
			col = Color(0.3, 0.07, 0.1)
			brd = Color(0.55, 0.15, 0.22)
		"pri":
			col = Color(0.16, 0.36, 0.84)
			brd = Color(0.3, 0.5, 1)
	for st in ["normal", "hover", "pressed", "focus", "disabled"]:
		var sb := StyleBoxFlat.new()
		sb.bg_color = col.lightened(0.15) if st == "pressed" else (col.darkened(0.4) if st == "disabled" else col)
		sb.set_corner_radius_all(10)
		sb.set_border_width_all(2)
		sb.border_color = brd
		sb.content_margin_left = 10
		sb.content_margin_right = 10
		b.add_theme_stylebox_override(st, sb)
	return b

func _build_modal() -> void:
	modal_layer = Control.new()
	modal_layer.set_anchors_preset(Control.PRESET_FULL_RECT)
	modal_layer.mouse_filter = Control.MOUSE_FILTER_STOP
	modal_layer.visible = false
	root.add_child(modal_layer)
	var dim := ColorRect.new()
	dim.color = Color(0.01, 0.02, 0.05, 0.62)
	dim.set_anchors_preset(Control.PRESET_FULL_RECT)
	modal_layer.add_child(dim)
	var center := CenterContainer.new()
	center.set_anchors_preset(Control.PRESET_FULL_RECT)
	modal_layer.add_child(center)
	var panel := PanelContainer.new()
	panel.add_theme_stylebox_override("panel", _panel_style(Color(0.05, 0.08, 0.15, 0.97), 16, Color(0.3, 0.45, 0.75)))
	center.add_child(panel)
	var scroll := ScrollContainer.new()
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	scroll.name = "scroll"
	panel.add_child(scroll)
	modal_box = VBoxContainer.new()
	modal_box.add_theme_constant_override("separation", 10)
	modal_box.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	scroll.add_child(modal_box)
	panel.name = "panel"

## buttons: [{"t": metin, "f": Callable, "c": "good|dark|pri", "dis": bool, "keep": bool}]
func modal(title: String, body: String, buttons: Array, extra: Callable = Callable()) -> void:
	for ch in modal_box.get_children():
		ch.free()
	var v := vs()
	var w := minf(980.0, v.x * 0.92)
	var scroll: ScrollContainer = modal_box.get_parent()
	scroll.custom_minimum_size = Vector2(w, minf(v.y * 0.86, 640))
	modal_box.custom_minimum_size = Vector2(w - 30, 0)
	if title != "":
		var t := _label(30, Color(0.62, 0.78, 1))
		t.text = title
		t.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
		modal_box.add_child(t)
	if body != "":
		var rt := RichTextLabel.new()
		rt.bbcode_enabled = true
		rt.fit_content = true
		rt.scroll_active = false
		rt.text = body
		rt.add_theme_font_size_override("normal_font_size", 21)
		rt.add_theme_font_size_override("bold_font_size", 22)
		rt.mouse_filter = Control.MOUSE_FILTER_PASS
		modal_box.add_child(rt)
	if extra.is_valid():
		extra.call(modal_box)
	var grid := GridContainer.new()
	grid.columns = 2 if w > 600 else 1
	grid.add_theme_constant_override("h_separation", 10)
	grid.add_theme_constant_override("v_separation", 10)
	for bd in buttons:
		if bd == null:
			continue
		var b = _mk_button(bd["t"], bd.get("c", ""))
		b.custom_minimum_size.x = (w - 50) / grid.columns
		b.disabled = bd.get("dis", false)
		var f: Callable = bd.get("f", Callable())
		var keep: bool = bd.get("keep", false)
		b.pressed.connect(func():
			if not keep:
				close_modal()
			if f.is_valid():
				f.call())
		grid.add_child(b)
	modal_box.add_child(grid)
	modal_layer.visible = true
	modal_open = true
	btn_root.visible = false
	scroll.scroll_vertical = 0
	get_tree().paused = true

func close_modal() -> void:
	modal_layer.visible = false
	modal_open = false
	btn_root.visible = hud.visible
	get_tree().paused = false
	joy_id = -1
	look_id = -1
	M.joy_vec = Vector2.ZERO

func mk_button(text: String, kind: String = "") -> Button:
	return _mk_button(text, kind)

func mk_label(text: String, sz: int = 21) -> Label:
	var l := _label(sz)
	l.text = text
	l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	return l
