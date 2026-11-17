extends Node3D
## Akışlı açık dünya şehri: oyuncunun çevresindeki bloklar yüklenir, uzaktakiler silinir.

var M
var meta = {}           # Vector2i -> Dictionary (deterministik plan, düğüm yok)
var built = {}          # Vector2i -> Node3D
var mat_cache = {}
var glass: StandardMaterial3D
var glass_lit: StandardMaterial3D
var lamp_head: StandardMaterial3D
var stream_t = 0.0
const RAD := 2

func setup(main) -> void:
	M = main
	glass = _plain(Color(0.06, 0.08, 0.1), 0.05, 0.9)
	glass_lit = _plain(Color(0.08, 0.08, 0.08), 0.08, 0.6)
	glass_lit.emission_enabled = true
	glass_lit.emission = Color(1.0, 0.8, 0.52)
	glass_lit.emission_energy_multiplier = 0.0
	lamp_head = _plain(Color(0.9, 0.9, 0.85), 0.3)
	lamp_head.emission_enabled = true
	lamp_head.emission = Color(1.0, 0.86, 0.6)
	lamp_head.emission_energy_multiplier = 0.2
	var size = C.LIM * 2.0 + 600.0
	var g = MeshInstance3D.new()
	var pm = PlaneMesh.new()
	pm.size = Vector2(size, size)
	g.mesh = pm
	g.material_override = pbr("asphalt", 0.18, Color(0.85, 0.85, 0.85), Color(0.2, 0.2, 0.21))
	add_child(g)
	var sb = StaticBody3D.new()
	var cs = CollisionShape3D.new()
	cs.shape = WorldBoundaryShape3D.new()
	sb.add_child(cs)
	add_child(sb)
	for i in 4:
		var w = StaticBody3D.new()
		var c2 = CollisionShape3D.new()
		var bs = BoxShape3D.new()
		var horiz = i < 2
		bs.size = Vector3(C.LIM * 2 + 40, 8, 2) if horiz else Vector3(2, 8, C.LIM * 2 + 40)
		c2.shape = bs
		w.add_child(c2)
		var s = -1.0 if i % 2 == 0 else 1.0
		w.position = Vector3(0, 4, s * (C.LIM + 1)) if horiz else Vector3(s * (C.LIM + 1), 4, 0)
		add_child(w)
	# şehir dışı: deniz
	var wm = MeshInstance3D.new()
	var wp = PlaneMesh.new()
	wp.size = Vector2(4000, 4000)
	wm.mesh = wp
	var wmat = _plain(Color(0.08, 0.22, 0.32), 0.08, 0.2)
	wm.material_override = wmat
	wm.position.y = -0.3
	add_child(wm)

# ---------------------------------------------------------------- malzemeler
func tex(n: String) -> Texture2D:
	var p = "res://assets/tex/" + n
	if ResourceLoader.exists(p):
		return load(p)
	return null

func pbr(role: String, scale: float, tint: Color = Color.WHITE, fallback: Color = Color(0.5, 0.5, 0.5)) -> StandardMaterial3D:
	var key = role + str(scale) + tint.to_html()
	if mat_cache.has(key):
		return mat_cache[key]
	var m = StandardMaterial3D.new()
	var d = tex(role + "_diff.jpg")
	if d:
		m.albedo_texture = d
		m.albedo_color = tint
	else:
		m.albedo_color = fallback * tint
	var n = tex(role + "_nor.jpg")
	if n:
		m.normal_enabled = true
		m.normal_texture = n
	var r = tex(role + "_rough.jpg")
	if r:
		m.roughness_texture = r
		m.roughness = 1.0
	else:
		m.roughness = 0.85
	m.uv1_triplanar = true
	m.uv1_world_triplanar = true
	m.uv1_scale = Vector3.ONE * scale
	m.uv1_triplanar_sharpness = 4.0
	m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_LINEAR_WITH_MIPMAPS_ANISOTROPIC
	mat_cache[key] = m
	return m

func _plain(c: Color, rough: float, metal: float = 0.0) -> StandardMaterial3D:
	var m = StandardMaterial3D.new()
	m.albedo_color = c
	m.roughness = rough
	m.metallic = metal
	return m

func plain(c: Color, rough: float, metal: float = 0.0) -> StandardMaterial3D:
	var key = "p" + c.to_html() + str(rough) + str(metal)
	if not mat_cache.has(key):
		mat_cache[key] = _plain(c, rough, metal)
	return mat_cache[key]

var surf_mats := {}
func city_mat(key: String) -> Material:
	if surf_mats.has(key):
		return surf_mats[key]
	var m: StandardMaterial3D
	match key:
		"pave": m = pbr("pavement", 0.35, Color.WHITE, Color(0.62, 0.61, 0.58)).duplicate()
		"brick": m = pbr("brick", 0.4, Color.WHITE, Color(0.55, 0.3, 0.24)).duplicate()
		"plaster": m = pbr("plaster", 0.3, Color.WHITE, Color(0.85, 0.82, 0.75)).duplicate()
		"concrete": m = pbr("concrete", 0.3, Color.WHITE, Color(0.6, 0.6, 0.6)).duplicate()
		"grass": m = pbr("grass", 0.25, Color.WHITE, Color(0.25, 0.42, 0.18)).duplicate()
		"lot": m = pbr("asphalt", 0.2, Color.WHITE, Color(0.18, 0.18, 0.19)).duplicate()
		"store": m = glass
		"glass_lit": m = glass_lit
		"lamp": m = lamp_head
		"bark": m = _plain(Color(0.3, 0.22, 0.15), 0.9)
		"leaf": m = _plain(Color(0.2, 0.36, 0.14), 0.85)
		_:
			m = _plain(Color.WHITE, 0.7, 0.15)
	if key in ["pave", "brick", "plaster", "concrete", "grass", "lot", "misc"]:
		m.vertex_color_use_as_albedo = true
	surf_mats[key] = m
	return m

# ---------------------------------------------------------------- plan (deterministik)
func block_meta(c: Vector2i) -> Dictionary:
	if meta.has(c):
		return meta[c]
	var rng = RandomNumberGenerator.new()
	rng.seed = hash(c) ^ 0x51ED
	var d = C.district_at(c.x, c.y)
	var r = C.block_rect(c.x, c.y)
	var md = {"d": d, "r": r, "lots": [], "b": [], "doors": [], "gas": null, "trees": [], "conts": []}
	var inner = Rect2(r.position + Vector2(3.5, 3.5), r.size - Vector2(7, 7))
	var hx = inner.size.x * 0.5
	var hz = inner.size.y * 0.5
	var station = c == Vector2i(0, 1)
	var gas_lot = 3 if (hash(c) % 5 == 0 and not station) else -1
	var park = not station and rng.randf() < (0.2 if d == "konut" else 0.1)
	for i in 4:
		var ix = i % 2
		var iz = i / 2
		var lot = Rect2(inner.position + Vector2(ix * hx, iz * hz), Vector2(hx, hz))
		var kind = "bld"
		if station and i == 0:
			kind = "karakol"
		elif station and i == 1:
			kind = "lot"
		elif i == gas_lot:
			kind = "gas"
		elif park:
			kind = "park"
		elif (d == "liman" or d == "sanayi") and rng.randf() < 0.3:
			kind = "yard"
		var lt = {"r": lot, "kind": kind, "iz": iz}
		if kind == "bld" or kind == "karakol":
			var ins = rng.randf_range(0.8, 2.2) if d != "konut" else rng.randf_range(1.8, 3.5)
			var br = lot.grow(-ins)
			var h: float
			match d:
				"merkez": h = rng.randf_range(14.0, 42.0)
				"konut": h = rng.randf_range(7.0, 13.0)
				"sanayi", "liman": h = rng.randf_range(7.0, 11.0)
				_: h = rng.randf_range(9.0, 16.0)
			var mk: String
			match d:
				"merkez": mk = C.pick(["plaster", "concrete", "brick", "plaster_warm"])
				"konut": mk = C.pick(["brick", "plaster_warm", "plaster"])
				"eski": mk = C.pick(["brick", "brick_dark", "plaster_warm"])
				_: mk = C.pick(["concrete", "concrete_dark", "tin"])
			if kind == "karakol":
				br = lot.grow(-0.5)
				h = 12.0
				mk = "plaster_blue"
			lt["b"] = br
			lt["h"] = h
			lt["mk"] = mk
			lt["depo"] = (d == "sanayi" or d == "liman")
			md["b"].append(br)
			var fz = br.position.y - 0.9 if iz == 0 else br.end.y + 0.9
			if kind == "karakol":
				md["doors"].append({"p": Vector2(br.get_center().x, fz), "kind": "karakol", "out": -1 if iz == 0 else 1})
			elif rng.randf() < 0.45:
				var dk = "house" if d == "konut" else ("shop" if rng.randf() < 0.45 else "house")
				md["doors"].append({"p": Vector2(br.get_center().x, fz), "kind": dk, "out": -1 if iz == 0 else 1})
		elif kind == "gas":
			md["gas"] = lot
		elif kind == "park":
			for k in 5:
				md["trees"].append(Vector2(rng.randf_range(lot.position.x + 2, lot.end.x - 2), rng.randf_range(lot.position.y + 2, lot.end.y - 2)))
		elif kind == "yard":
			for k in rng.randi_range(3, 7):
				var cp = Vector2(rng.randf_range(lot.position.x + 3, lot.end.x - 6), rng.randf_range(lot.position.y + 2, lot.end.y - 3))
				var cr = Rect2(cp, Vector2(6.1, 2.45))
				md["conts"].append({"r": cr, "lv": rng.randi_range(1, 2), "c": C.pick(["cont_r", "cont_b", "cont_g", "cont_y"])})
				md["b"].append(cr)
		md["lots"].append(lt)
	if d == "konut" or d == "merkez":
		for k in 4:
			var side = k
			var along = rng.randf_range(0.15, 0.85)
			var tp: Vector2
			match side:
				0: tp = Vector2(r.position.x + r.size.x * along, r.position.y + 1.6)
				1: tp = Vector2(r.position.x + r.size.x * along, r.end.y - 1.6)
				2: tp = Vector2(r.position.x + 1.6, r.position.y + r.size.y * along)
				_: tp = Vector2(r.end.x - 1.6, r.position.y + r.size.y * along)
			md["trees"].append(tp)
	meta[c] = md
	return md

# ---------------------------------------------------------------- akış
func update_stream(p: Vector2, delta: float, force: bool = false) -> void:
	stream_t -= delta
	if stream_t > 0.0 and not force:
		return
	stream_t = 0.25
	var pc = Vector2i(floori(p.x / C.S), floori(p.y / C.S))
	var n = 0
	for dz in range(-RAD, RAD + 1):
		for dx in range(-RAD, RAD + 1):
			var c = pc + Vector2i(dx, dz)
			if c.x < -C.WB or c.x >= C.WB or c.y < -C.WB or c.y >= C.WB:
				continue
			if built.has(c):
				continue
			if n >= (99 if force else 1):
				break
			built[c] = _build_block(c)
			n += 1
	for c in built.keys():
		if absi(c.x - pc.x) > RAD + 1 or absi(c.y - pc.y) > RAD + 1:
			(built[c] as Node3D).queue_free()
			built.erase(c)

func loaded_count() -> int:
	return built.size()

# ---------------------------------------------------------------- inşa
var _sts = {}
func _st(key: String) -> SurfaceTool:
	if not _sts.has(key):
		var s = SurfaceTool.new()
		s.begin(Mesh.PRIMITIVE_TRIANGLES)
		_sts[key] = s
	return _sts[key]

const KMAP := {
	"trim": ["misc", Color(0.78, 0.77, 0.74)], "metal": ["misc", Color(0.6, 0.62, 0.64)], "tin": ["misc", Color(0.5, 0.53, 0.56)],
	"dark": ["misc", Color(0.12, 0.13, 0.14)], "white": ["misc", Color(0.92, 0.92, 0.9)], "red": ["misc", Color(0.7, 0.1, 0.08)],
	"navy": ["misc", Color(0.08, 0.16, 0.38)], "rail": ["misc", Color(0.75, 0.77, 0.8)], "wood": ["misc", Color(0.45, 0.32, 0.2)],
	"cont_r": ["misc", Color(0.62, 0.16, 0.1)], "cont_b": ["misc", Color(0.1, 0.3, 0.55)], "cont_g": ["misc", Color(0.18, 0.45, 0.25)], "cont_y": ["misc", Color(0.8, 0.6, 0.12)],
	"yellow": ["misc", Color(0.9, 0.7, 0.12)], "frame": ["misc", Color(0.2, 0.2, 0.21)], "pole": ["misc", Color(0.18, 0.19, 0.2)],
	"pave": ["pave", Color(0.95, 0.95, 0.93)], "brick": ["brick", Color(1, 1, 1)], "brick_dark": ["brick", Color(0.72, 0.66, 0.62)],
	"plaster": ["plaster", Color(0.96, 0.93, 0.86)], "plaster_warm": ["plaster", Color(1.0, 0.86, 0.7)], "plaster_blue": ["plaster", Color(0.75, 0.84, 1.0)],
	"concrete": ["concrete", Color(0.92, 0.92, 0.92)], "concrete_dark": ["concrete", Color(0.62, 0.63, 0.64)], "roof": ["concrete", Color(0.55, 0.55, 0.57)],
	"grass": ["grass", Color(0.85, 1.0, 0.8)], "lot": ["lot", Color(0.7, 0.7, 0.7)], "store": ["store", Color(1, 1, 1)], "glass_lit": ["glass_lit", Color(1, 1, 1)], "lamp": ["lamp", Color(1, 1, 1)],
}
const BIG := ["brick", "plaster", "concrete"]
const FACES := [[Vector3(1, 0, 0), Vector3(0, 0, 1), Vector3(0, 1, 0)], [Vector3(-1, 0, 0), Vector3(0, 0, -1), Vector3(0, 1, 0)], [Vector3(0, 1, 0), Vector3(1, 0, 0), Vector3(0, 0, 1)], [Vector3(0, -1, 0), Vector3(1, 0, 0), Vector3(0, 0, -1)], [Vector3(0, 0, 1), Vector3(-1, 0, 0), Vector3(0, 1, 0)], [Vector3(0, 0, -1), Vector3(1, 0, 0), Vector3(0, 1, 0)]]

func _box(key: String, center: Vector3, size: Vector3, xf: Basis = Basis()) -> void:
	var km: Array = KMAP.get(key, ["misc", Color.MAGENTA])
	var st: SurfaceTool = _st(km[0])
	var col: Color = km[1]
	var h := size * 0.5
	for f in FACES:
		var n: Vector3 = f[0]
		var u: Vector3 = f[1]
		var v: Vector3 = f[2]
		var c := n * h.abs() * 1.0
		c = Vector3(n.x * h.x, n.y * h.y, n.z * h.z)
		var du := Vector3(u.x * h.x, u.y * h.y, u.z * h.z)
		var dv := Vector3(v.x * h.x, v.y * h.y, v.z * h.z)
		var q := [c - du - dv, c + du - dv, c + du + dv, c - du + dv]
		var wn := (xf * n).normalized()
		# Godot: ön yüz saat yönünde → (b-a)x(c-a) normalin tersini göstermeli
		var a: Vector3 = xf * q[0]
		var b: Vector3 = xf * q[1]
		var cc: Vector3 = xf * q[2]
		var order := [0, 1, 2, 0, 2, 3]
		if (b - a).cross(cc - a).dot(wn) > 0.0:
			order = [0, 2, 1, 0, 3, 2]
		for i in order:
			st.set_color(col)
			st.set_normal(wn)
			var wp: Vector3 = center + xf * q[i]
			st.set_uv(Vector2(wp.x + wp.y, wp.z + wp.y) * 0.25)
			st.add_vertex(wp)

func _mesh(key: String, mesh: Mesh, xf: Transform3D) -> void:
	_st(key).append_from(mesh, 0, xf)

func _collider(parent: Node3D, center: Vector3, size: Vector3) -> void:
	var sb = StaticBody3D.new()
	sb.position = center
	var cs = CollisionShape3D.new()
	var bs = BoxShape3D.new()
	bs.size = size
	cs.shape = bs
	sb.add_child(cs)
	parent.add_child(sb)

func _flush(parent: Node3D) -> void:
	var big := ArrayMesh.new()
	var det := ArrayMesh.new()
	for key in _sts.keys():
		var am: ArrayMesh = big if key in BIG else det
		(_sts[key] as SurfaceTool).commit(am)
		am.surface_set_material(am.get_surface_count() - 1, city_mat(key))
	_sts.clear()
	var mb := MeshInstance3D.new()
	mb.mesh = big
	parent.add_child(mb)
	var md := MeshInstance3D.new()
	md.mesh = det
	md.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	md.visibility_range_end = 150.0
	md.visibility_range_end_margin = 10.0
	parent.add_child(md)

func _mm(parent: Node3D, mesh: Mesh, xfs: Array, mat: Material, cast: bool) -> void:
	if xfs.is_empty():
		return
	var mm = MultiMesh.new()
	mm.transform_format = MultiMesh.TRANSFORM_3D
	mm.mesh = mesh
	mm.instance_count = xfs.size()
	for i in xfs.size():
		mm.set_instance_transform(i, xfs[i])
	var mmi = MultiMeshInstance3D.new()
	mmi.multimesh = mm
	mmi.material_override = mat
	mmi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_ON if cast else GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	parent.add_child(mmi)

var _wm: BoxMesh
var _fm: BoxMesh
func _build_block(c: Vector2i) -> Node3D:
	var md = block_meta(c)
	var node = Node3D.new()
	add_child(node)
	var rng = RandomNumberGenerator.new()
	rng.seed = hash(c) ^ 0xBEEF
	var r: Rect2 = md["r"]
	var cx = r.get_center().x
	var cz = r.get_center().y
	_box("pave", Vector3(cx, 0.09, cz), Vector3(r.size.x, 0.18, r.size.y))
	_collider(node, Vector3(cx, 0.09, cz), Vector3(r.size.x, 0.18, r.size.y))
	for s in [-1.0, 1.0]:
		_box("trim", Vector3(cx, 0.1, cz + s * (r.size.y * 0.5 - 0.15)), Vector3(r.size.x, 0.2, 0.3))
		_box("trim", Vector3(cx + s * (r.size.x * 0.5 - 0.15), 0.1, cz), Vector3(0.3, 0.2, r.size.y))
	var win_u: Array = []
	var win_l: Array = []
	var win_f: Array = []
	for lt in md["lots"]:
		var lr: Rect2 = lt["r"]
		match lt["kind"]:
			"bld", "karakol":
				_building(node, lt, rng, win_u, win_l, win_f)
			"lot":
				_box("lot", Vector3(lr.get_center().x, 0.185, lr.get_center().y), Vector3(lr.size.x, 0.01, lr.size.y))
				for i in 6:
					_box("white", Vector3(lr.position.x + 2 + i * 3.2, 0.192, lr.get_center().y - 1), Vector3(0.12, 0.01, 5))
			"gas":
				_gas(node, lr)
			"park":
				_box("grass", Vector3(lr.get_center().x, 0.19, lr.get_center().y), Vector3(lr.size.x, 0.02, lr.size.y))
				_box("wood", Vector3(lr.get_center().x, 0.45, lr.get_center().y), Vector3(1.8, 0.1, 0.5))
			"yard":
				_box("concrete_dark", Vector3(lr.get_center().x, 0.185, lr.get_center().y), Vector3(lr.size.x, 0.01, lr.size.y))
	for ct in md["conts"]:
		var cr: Rect2 = ct["r"]
		for lv in ct["lv"]:
			var ctr = Vector3(cr.get_center().x, 0.18 + 1.3 + lv * 2.6, cr.get_center().y)
			_box(ct["c"], ctr, Vector3(cr.size.x, 2.59, cr.size.y))
			for k in 6:
				_box("dark", ctr + Vector3(-2.6 + k * 1.04, 0, cr.size.y * 0.5 + 0.01), Vector3(0.06, 2.3, 0.02))
		_collider(node, Vector3(cr.get_center().x, 2.8, cr.get_center().y), Vector3(cr.size.x, 5.2, cr.size.y))
	for tp in md["trees"]:
		_tree(Vector3(tp.x, 0.18, tp.y), rng)
	for door in md["doors"]:
		_door(node, door)
	_roads(node, c, r)
	for x in win_f:
		_box("frame", (x as Transform3D).origin, Vector3(1.6, 2.05, 0.08), (x as Transform3D).basis)
	for x in win_u:
		_box("store", (x as Transform3D).origin, Vector3(1.35, 1.8, 0.1), (x as Transform3D).basis)
	for x in win_l:
		_box("glass_lit", (x as Transform3D).origin, Vector3(1.35, 1.8, 0.1), (x as Transform3D).basis)
	_lamps(node, r)
	_flush(node)
	return node

func _building(node: Node3D, lt: Dictionary, rng: RandomNumberGenerator, wu: Array, wl: Array, wf: Array) -> void:
	var br: Rect2 = lt["b"]
	var h: float = lt["h"]
	var mk: String = lt["mk"]
	var base = 0.18
	var pos = Vector3(br.get_center().x, 0, br.get_center().y)
	var size = Vector3(br.size.x, h, br.size.y)
	_box(mk, pos + Vector3(0, base + h * 0.5, 0), size)
	_collider(node, pos + Vector3(0, base + h * 0.5, 0), size)
	var depo: bool = lt["depo"]
	if not depo:
		_box("store", pos + Vector3(0, base + 1.7, 0), Vector3(size.x + 0.1, 3.3, size.z + 0.1))
		_box("trim", pos + Vector3(0, base + 3.55, 0), Vector3(size.x + 0.35, 0.4, size.z + 0.35))
	var top = base + h
	_box("roof", pos + Vector3(0, top + 0.02, 0), Vector3(size.x - 0.1, 0.04, size.z - 0.1))
	for s in [-1.0, 1.0]:
		_box("trim", pos + Vector3(0, top + 0.45, s * (size.z * 0.5 - 0.15)), Vector3(size.x, 0.9, 0.3))
		_box("trim", pos + Vector3(s * (size.x * 0.5 - 0.15), top + 0.45, 0), Vector3(0.3, 0.9, size.z))
	for i in rng.randi_range(1, 3):
		_box("metal", pos + Vector3(rng.randf_range(-size.x * 0.3, size.x * 0.3), top + 0.65, rng.randf_range(-size.z * 0.3, size.z * 0.3)), Vector3(1.8, 1.3, 1.3))
	if depo:
		var fz = -1.0 if lt["iz"] == 0 else 1.0
		_box("dark", pos + Vector3(0, base + 2.2, fz * (size.z * 0.5 + 0.03)), Vector3(minf(6.0, size.x - 2), 4.4, 0.08))
		for k in 5:
			_box("metal", pos + Vector3(0, base + 0.4 + k * 0.88, fz * (size.z * 0.5 + 0.07)), Vector3(minf(6.0, size.x - 2), 0.05, 0.04))
	if lt["kind"] == "karakol":
		_box("navy", pos + Vector3(0, base + 4.2, 0), Vector3(size.x + 0.4, 0.9, size.z + 0.4))
		var lbl = Label3D.new()
		lbl.text = "POLİS"
		lbl.font_size = 220
		lbl.pixel_size = 0.012
		lbl.outline_size = 24
		lbl.outline_modulate = Color(0.05, 0.15, 0.45)
		lbl.position = pos + Vector3(0, base + 6.0, -size.z * 0.5 - 0.3)
		lbl.rotation.y = PI
		node.add_child(lbl)
		_box("dark", pos + Vector3(size.x * 0.3, top + 3, -size.z * 0.3), Vector3(0.12, 6, 0.12))
		_box("red", pos + Vector3(size.x * 0.3 + 0.9, top + 5.2, -size.z * 0.3), Vector3(1.8, 1.1, 0.04))
	_windows(pos, size, base, rng, wu, wl, wf, depo)

func _windows(pos: Vector3, size: Vector3, base: float, rng: RandomNumberGenerator, wu: Array, wl: Array, wf: Array, depo: bool) -> void:
	var rows = int((size.y - 5.0) / 3.4)
	if depo:
		rows = 1
	var faces = [[Vector3(0, 0, 1), size.x, size.z], [Vector3(0, 0, -1), size.x, size.z], [Vector3(1, 0, 0), size.z, size.x], [Vector3(-1, 0, 0), size.z, size.x]]
	for f in faces:
		var nrm: Vector3 = f[0]
		var flen: float = f[1]
		var depth: float = f[2]
		var cols = int((flen - 1.5) / 3.1)
		if depo:
			cols = int(cols / 2)
		if cols < 1:
			continue
		var bas = Basis(Vector3.UP, atan2(nrm.x, nrm.z))
		var tangent = bas.x
		var step = 3.1 if not depo else 6.2
		for rr in rows:
			for cc in cols:
				var u = (cc - (cols - 1) * 0.5) * step
				var y = base + (5.2 if not depo else 6.0) + rr * 3.4
				var p = pos + nrm * (depth * 0.5 + 0.02) + tangent * u + Vector3(0, y, 0)
				wf.append(Transform3D(bas, p - nrm * 0.01))
				if rng.randf() < 0.38:
					wl.append(Transform3D(bas, p))
				else:
					wu.append(Transform3D(bas, p))

func _door(node: Node3D, door: Dictionary) -> void:
	var p: Vector2 = door["p"]
	var o: float = door["out"]
	var fz = p.y - o * 0.9
	var dc = "navy" if door["kind"] == "karakol" else ("store" if door["kind"] == "shop" else "wood")
	_box("dark", Vector3(p.x, 1.4, fz + o * 0.05), Vector3(2.2, 2.6, 0.12))
	_box(dc, Vector3(p.x, 1.32, fz + o * 0.09), Vector3(1.9, 2.3, 0.08))
	if door["kind"] == "shop":
		_box("red", Vector3(p.x, 2.85, fz + o * 0.7), Vector3(4.4, 0.15, 1.4))
		var lbl = Label3D.new()
		lbl.text = "MARKET"
		lbl.font_size = 96
		lbl.pixel_size = 0.008
		lbl.modulate = Color(1, 0.75, 0.2)
		lbl.outline_size = 12
		lbl.position = Vector3(p.x, 3.5, fz + o * 0.12)
		lbl.rotation.y = 0.0 if o > 0 else PI
		node.add_child(lbl)
	elif door["kind"] == "karakol":
		_box("concrete", Vector3(p.x, 0.25, fz + o * 1.5), Vector3(6, 0.15, 3))

func _gas(node: Node3D, lr: Rect2) -> void:
	var c = Vector3(lr.get_center().x, 0, lr.get_center().y)
	_box("concrete", c + Vector3(0, 0.2, 0), Vector3(lr.size.x, 0.05, lr.size.y))
	_box("red", c + Vector3(0, 4.9, 0), Vector3(lr.size.x - 4, 0.6, 9))
	_box("white", c + Vector3(0, 5.25, 0), Vector3(lr.size.x - 4, 0.12, 9))
	for sx in [-1.0, 1.0]:
		for sz in [-1.0, 1.0]:
			_box("white", c + Vector3(sx * (lr.size.x * 0.5 - 3), 2.5, sz * 3.6), Vector3(0.4, 4.8, 0.4))
	for i in 3:
		_box("white", c + Vector3(-5 + i * 5, 0.95, 0), Vector3(1.1, 1.5, 0.9))
		_box("red", c + Vector3(-5 + i * 5, 1.4, 0), Vector3(1.12, 0.35, 0.92))
		_collider(node, c + Vector3(-5 + i * 5, 0.95, 0), Vector3(1.1, 1.5, 0.9))
	var lbl = Label3D.new()
	lbl.text = "AKARYAKIT"
	lbl.font_size = 128
	lbl.pixel_size = 0.01
	lbl.outline_size = 16
	lbl.position = c + Vector3(0, 6.0, -4.6)
	lbl.rotation.y = PI
	node.add_child(lbl)

func _tree(p: Vector3, rng: RandomNumberGenerator) -> void:
	var h = rng.randf_range(3.0, 4.5)
	var trunk = CylinderMesh.new()
	trunk.top_radius = 0.12
	trunk.bottom_radius = 0.18
	trunk.height = h
	trunk.radial_segments = 8
	_mesh("bark", trunk, Transform3D(Basis(), p + Vector3(0, h * 0.5, 0)))
	for i in 3:
		var s = SphereMesh.new()
		var rr = rng.randf_range(1.2, 1.8)
		s.radius = rr
		s.height = rr * 1.7
		s.radial_segments = 10
		s.rings = 6
		_mesh("leaf", s, Transform3D(Basis(), p + Vector3(rng.randf_range(-0.6, 0.6), h + rng.randf_range(-0.3, 0.8), rng.randf_range(-0.6, 0.6))))

func _roads(node: Node3D, c: Vector2i, r: Rect2) -> void:
	var vx = c.x * C.S
	var t = r.position.y + 6.0
	while t < r.end.y - 6.0:
		_box("white", Vector3(vx, 0.012, t), Vector3(0.15, 0.02, 3.0))
		t += 7.0
	var hz = c.y * C.S
	t = r.position.x + 6.0
	while t < r.end.x - 6.0:
		if c.y == 0:
			for off in [-6.1, 6.1]:
				_box("white", Vector3(t, 0.012, hz + off), Vector3(3.0, 0.02, 0.15))
		else:
			_box("white", Vector3(t, 0.012, hz), Vector3(3.0, 0.02, 0.15))
		t += 7.0
	for i in 8:
		var o = (i - 3.5) * 1.5
		_box("white", Vector3(vx + o, 0.013, r.position.y + 2.0), Vector3(0.55, 0.02, 3.0))
		_box("white", Vector3(vx + o, 0.013, r.end.y - 2.0), Vector3(0.55, 0.02, 3.0))
		if c.y != 0:
			_box("white", Vector3(r.position.x + 2.0, 0.013, hz + o), Vector3(3.0, 0.02, 0.55))
			_box("white", Vector3(r.end.x - 2.0, 0.013, hz + o), Vector3(3.0, 0.02, 0.55))
	if c.y == 0:
		_box("yellow", Vector3(r.get_center().x, 0.012, hz - 0.2), Vector3(r.size.x, 0.02, 0.12))
		_box("yellow", Vector3(r.get_center().x, 0.012, hz + 0.2), Vector3(r.size.x, 0.02, 0.12))
		for zz in [hz - C.HW * 0.5 - 0.6, hz + C.HW * 0.5 + 0.6]:
			_box("rail", Vector3(r.get_center().x, 0.75, zz), Vector3(r.size.x, 0.35, 0.12))
		if posmod(c.x, 2) == 0:
			var gx = r.position.x + 18.0
			for zz in [hz - C.HW * 0.5 - 1.2, hz + C.HW * 0.5 + 1.2]:
				_box("dark", Vector3(gx, 3.9, zz), Vector3(0.4, 7.8, 0.4))
			_box("dark", Vector3(gx, 7.4, hz), Vector3(0.4, 0.4, C.HW + 2.4))
			for sgn in [-1.0, 1.0]:
				_box("cont_g", Vector3(gx, 6.0, hz + sgn * 5.5), Vector3(0.1, 2.4, 6.5))
				var sg = Label3D.new()
				sg.text = "O-1  LİMAN →\n120"
				sg.font_size = 110
				sg.pixel_size = 0.012
				sg.outline_size = 0
				sg.position = Vector3(gx - 0.07 if sgn < 0 else gx + 0.07, 6.0, hz + sgn * 5.5)
				sg.rotation.y = -PI / 2 if sgn < 0 else PI / 2
				node.add_child(sg)

func _lamps(node: Node3D, r: Rect2) -> void:
	var pts: Array = []
	for cc in [[r.position.x + 0.8, r.position.y + 0.8, -1.0], [r.end.x - 0.8, r.end.y - 0.8, 1.0], [r.get_center().x, r.position.y + 0.8, -1.0], [r.get_center().x, r.end.y - 0.8, 1.0]]:
		var p = Vector3(cc[0], 0.18, cc[1])
		var dz: float = cc[2]
		_box("pole", p + Vector3(0, 3.5, 0), Vector3(0.16, 7.0, 0.16))
		_box("pole", p + Vector3(0, 6.9, dz * 0.9), Vector3(0.1, 0.1, 1.8))
		var hp = p + Vector3(0, 6.85, dz * 1.75)
		_box("lamp", hp, Vector3(0.45, 0.14, 0.7))
		pts.append(hp)
	node.set_meta("lamps", pts)

func lamps_near(p: Vector2, maxn: int) -> Array:
	var all: Array = []
	for n in built.values():
		if (n as Node3D).has_meta("lamps"):
			for hp in n.get_meta("lamps"):
				all.append(hp)
	all.sort_custom(func(a, b): return Vector2(a.x, a.z).distance_squared_to(p) < Vector2(b.x, b.z).distance_squared_to(p))
	return all.slice(0, maxn)

# ---------------------------------------------------------------- mantık sorguları
func rects_near(p: Vector2) -> Array:
	var out: Array = []
	var pc = Vector2i(floori(p.x / C.S), floori(p.y / C.S))
	for dz in range(-1, 2):
		for dx in range(-1, 2):
			var c = pc + Vector2i(dx, dz)
			if c.x < -C.WB or c.x >= C.WB or c.y < -C.WB or c.y >= C.WB:
				continue
			out.append_array(block_meta(c)["b"])
	return out

## Daireyi binalardan dışarı iter. [yeni_konum, çarptı_mı]
func collide_circle(p: Vector2, r: float) -> Array:
	var hit = false
	for b in rects_near(p):
		var rc: Rect2 = b
		if p.x < rc.position.x - r or p.x > rc.end.x + r or p.y < rc.position.y - r or p.y > rc.end.y + r:
			continue
		var n = Vector2(clampf(p.x, rc.position.x, rc.end.x), clampf(p.y, rc.position.y, rc.end.y))
		var d = p - n
		var l = d.length()
		if l < 0.0001:
			var e = [p.x - rc.position.x, rc.end.x - p.x, p.y - rc.position.y, rc.end.y - p.y]
			var m: float = e.min()
			var ix = e.find(m)
			if ix == 0: p.x = rc.position.x - r
			elif ix == 1: p.x = rc.end.x + r
			elif ix == 2: p.y = rc.position.y - r
			else: p.y = rc.end.y + r
			hit = true
		elif l < r:
			p = n + d / l * r
			hit = true
	p.x = clampf(p.x, -C.LIM + r, C.LIM - r)
	p.y = clampf(p.y, -C.LIM + r, C.LIM - r)
	return [p, hit]

func inside_building(p: Vector2, m: float = 0.5) -> bool:
	for b in rects_near(p):
		if (b as Rect2).grow(m).has_point(p):
			return true
	return false

func is_road(p: Vector2) -> bool:
	var kx = roundi(p.x / C.S)
	var kz = roundi(p.y / C.S)
	return absf(p.x - kx * C.S) < C.RW * 0.5 or absf(p.y - kz * C.S) < C.hh(kz)

func doors_near(p: Vector2, rad: float) -> Array:
	var out: Array = []
	var pc = Vector2i(floori(p.x / C.S), floori(p.y / C.S))
	for dz in range(-1, 2):
		for dx in range(-1, 2):
			var c = pc + Vector2i(dx, dz)
			if c.x < -C.WB or c.x >= C.WB or c.y < -C.WB or c.y >= C.WB:
				continue
			for d in block_meta(c)["doors"]:
				if (d["p"] as Vector2).distance_to(p) < rad:
					out.append(d)
	return out

func gas_near(p: Vector2) -> bool:
	var c = Vector2i(floori(p.x / C.S), floori(p.y / C.S))
	if c.x < -C.WB or c.x >= C.WB or c.y < -C.WB or c.y >= C.WB:
		return false
	var g = block_meta(c)["gas"]
	return g != null and (g as Rect2).grow(3.0).has_point(p)

func karakol_door() -> Dictionary:
	for d in block_meta(Vector2i(0, 1))["doors"]:
		if d["kind"] == "karakol":
			return d
	return {"p": Vector2(16, 70), "kind": "karakol", "out": -1}

func find_door(kind: String, region: String, from: Vector2, min_d: float = 60.0, max_d: float = 420.0) -> Dictionary:
	for i in 120:
		var c = Vector2i(randi_range(-C.WB, C.WB - 1), randi_range(-C.WB, C.WB - 1))
		if region != "" and C.district_at(c.x, c.y) != region and i < 80:
			continue
		var ds: Array = block_meta(c)["doors"].filter(func(d): return d["kind"] == kind)
		if ds.is_empty():
			continue
		var d: Dictionary = C.pick(ds)
		var dd = (d["p"] as Vector2).distance_to(from)
		if dd < min_d or dd > max_d:
			continue
		return d
	return {}

func road_point(from: Vector2, region: String = "", min_d: float = 0.0, max_d: float = 99999.0) -> Dictionary:
	for i in 80:
		var cx: int
		var cz: int
		if region != "" and randf() < 0.75:
			cx = randi_range(-C.WB, C.WB - 1)
			cz = randi_range(-C.WB, C.WB - 1)
			if C.district_at(cx, cz) != region:
				continue
		else:
			cx = clampi(floori(from.x / C.S) + randi_range(-3, 3), -C.WB, C.WB - 1)
			cz = clampi(floori(from.y / C.S) + randi_range(-3, 3), -C.WB + 1, C.WB - 1)
		var p: Vector2
		var dir: int
		if randf() < 0.5:
			dir = 0 if randf() < 0.5 else 2
			var off = 3.5
			if cz == 0:
				off = 3.2 if randf() < 0.5 else 9.0
			p = Vector2(cx * C.S + C.RW * 0.5 + 6 + randf() * (C.S - C.RW - 12), cz * C.S + (off if dir == 0 else -off))
		else:
			dir = 1 if randf() < 0.5 else 3
			p = Vector2(cx * C.S + (-3.5 if dir == 1 else 3.5), cz * C.S + C.hh(cz) + 6 + randf() * (C.S - C.hh(cz) - C.hh(cz + 1) - 12))
		var d = p.distance_to(from)
		if d < min_d or d > max_d:
			continue
		if absf(p.x) > C.LIM - 8 or absf(p.y) > C.LIM - 8:
			continue
		return {"p": p, "dir": dir}
	return {"p": from + Vector2(30, 0), "dir": 0}

func side_point(from: Vector2, region: String = "", min_d: float = 40.0, max_d: float = 340.0) -> Vector2:
	for i in 80:
		var cx = randi_range(-C.WB, C.WB - 1) if region != "" and randf() < 0.7 else clampi(floori(from.x / C.S) + randi_range(-3, 3), -C.WB, C.WB - 1)
		var cz = randi_range(-C.WB, C.WB - 1) if region != "" else clampi(floori(from.y / C.S) + randi_range(-3, 3), -C.WB, C.WB - 1)
		if region != "" and C.district_at(cx, cz) != region and i < 50:
			continue
		var r = C.block_rect(cx, cz).grow(-1.5)
		var p: Vector2
		match randi() % 4:
			0: p = Vector2(lerpf(r.position.x, r.end.x, randf()), r.position.y)
			1: p = Vector2(r.end.x, lerpf(r.position.y, r.end.y, randf()))
			2: p = Vector2(lerpf(r.position.x, r.end.x, randf()), r.end.y)
			_: p = Vector2(r.position.x, lerpf(r.position.y, r.end.y, randf()))
		var d = p.distance_to(from)
		if d < min_d or d > max_d:
			continue
		if inside_building(p, 0.6):
			continue
		return p
	return from + Vector2(20, 20)

func hw_point(from: Vector2) -> Vector2:
	var x = clampf(from.x + (-1.0 if randf() < 0.5 else 1.0) * randf_range(60, 160), -C.LIM + 30, C.LIM - 30)
	return Vector2(x, 9.0 if randf() < 0.5 else -9.0)
