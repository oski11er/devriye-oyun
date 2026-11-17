extends Node3D
## Trafik ve yaya yapay zekası (şerit tabanlı araçlar, yürüyen yayalar, kaçan şüpheliler)

var M
var cars: Array = []
var peds: Array = []
var spawn_t = 0.0
var geo_cache = {}
var mat_cache = {}
var human_scene: PackedScene

class NCar:
	var node: AnimatableBody3D
	var vis: Node3D
	var pos = Vector2.ZERO
	var dir = 0
	var v = 0.0
	var base = 10.0
	var sbase = 0.0
	var ni = 0.0
	var hl = false
	var kind = "civ"
	var col = Color.WHITE
	var plate = ""
	var speeder = false
	var flee = false
	var backup = false
	var stopping = false
	var stopped = false
	var wreck = false
	var hp = 100.0
	var mission = null
	var keep = false
	var drv = null
	var angry = 0.0
	var pull = 0.0
	var yaw = 0.0
	var radared = false
	var skill = 50.0
	var big = false
	var lb: Array = []

class NPed:
	var node: Node3D
	var anim: AnimationPlayer
	var pos = Vector2.ZERO
	var face = Vector2(0, 1)
	var kind = "civ"
	var ai = "idle"
	var bx = 0
	var bz = 0
	var s = 0.0
	var per = 1.0
	var ds = 1.0
	var v = 1.4
	var down = false
	var down_t = 0.0
	var keep = false
	var mission = null
	var stam = 14.0
	var wa = 0.0
	var armed = false
	var state = ""
	var bribe = -1
	var warned = false
	var col = Color.GRAY
	var center = Vector2.ZERO
	var side = 0.0
	var moving = false
	var marker: Node3D

func setup(main) -> void:
	M = main

# ---------------------------------------------------------------- araç modeli (düşük poligon, PBR boya)
func _mat(key: String, c: Color, rough: float, metal: float, clear: bool = false, emis: float = 0.0) -> StandardMaterial3D:
	if mat_cache.has(key):
		return mat_cache[key]
	var m = StandardMaterial3D.new()
	m.albedo_color = c
	m.roughness = rough
	m.metallic = metal
	if clear:
		m.clearcoat_enabled = true
		m.clearcoat = 1.0
		m.clearcoat_roughness = 0.05
	if emis > 0.0:
		m.emission_enabled = true
		m.emission = c
		m.emission_energy_multiplier = emis
	mat_cache[key] = m
	return m

func _tri(st: SurfaceTool, a: Vector3, b: Vector3, c: Vector3, n: Vector3, col: Color) -> void:
	# Godot ön yüz: saat yönü → (b-a)x(c-a) normalin tersi olmalı
	if (b - a).cross(c - a).dot(n) > 0.0:
		var t = b
		b = c
		c = t
	for v in [a, b, c]:
		st.set_color(col)
		st.set_normal(n)
		st.set_uv(Vector2(v.x, v.y))
		st.add_vertex(v)

func _quad(st: SurfaceTool, a: Vector3, b: Vector3, c: Vector3, d: Vector3, out: Vector3, col: Color = Color.WHITE) -> void:
	var n = (b - a).cross(c - a).normalized()
	if n.dot(out) < 0:
		n = -n
	_tri(st, a, b, c, n, col)
	_tri(st, a, c, d, n, col)

func _vbox(st: SurfaceTool, c: Vector3, sz: Vector3, col: Color) -> void:
	var h = sz * 0.5
	var P = [c + Vector3(-h.x, -h.y, -h.z), c + Vector3(h.x, -h.y, -h.z), c + Vector3(h.x, h.y, -h.z), c + Vector3(-h.x, h.y, -h.z), c + Vector3(-h.x, -h.y, h.z), c + Vector3(h.x, -h.y, h.z), c + Vector3(h.x, h.y, h.z), c + Vector3(-h.x, h.y, h.z)]
	_quad(st, P[0], P[1], P[2], P[3], Vector3(0, 0, -1), col)
	_quad(st, P[4], P[5], P[6], P[7], Vector3(0, 0, 1), col)
	_quad(st, P[0], P[4], P[7], P[3], Vector3(-1, 0, 0), col)
	_quad(st, P[1], P[5], P[6], P[2], Vector3(1, 0, 0), col)
	_quad(st, P[3], P[2], P[6], P[7], Vector3(0, 1, 0), col)
	_quad(st, P[0], P[1], P[5], P[4], Vector3(0, -1, 0), col)

func _vwheel(st: SurfaceTool, c: Vector3, r: float, w: float, col: Color, hub: Color) -> void:
	var seg = 12
	for i in seg:
		var a0 = TAU * i / seg
		var a1 = TAU * (i + 1) / seg
		var p0 = Vector3(0, sin(a0) * r, cos(a0) * r)
		var p1 = Vector3(0, sin(a1) * r, cos(a1) * r)
		var n = Vector3(0, sin((a0 + a1) * 0.5), cos((a0 + a1) * 0.5))
		var hw = Vector3(w * 0.5, 0, 0)
		_quad(st, c + p0 - hw, c + p1 - hw, c + p1 + hw, c + p0 + hw, n, col)
		for sx in [-1.0, 1.0]:
			var side = Vector3(sx * w * 0.5, 0, 0)
			_tri(st, c + side, c + side + p0 * (0.62 if true else 1.0), c + side + p1 * 0.62, Vector3(sx, 0, 0), hub)
			_quad(st, c + side + p0 * 0.62, c + side + p1 * 0.62, c + side + p1, c + side + p0, Vector3(sx, 0, 0), col)

func car_mesh(col: Color, big: bool, police: bool, taxi: bool) -> ArrayMesh:
	var key = col.to_html() + str(big) + str(police) + str(taxi)
	if geo_cache.has(key):
		return geo_cache[key]
	var L = 4.5 if big else 4.2
	var Wd = 1.95 if big else 1.82
	var y0 = 0.32
	var hb = 0.85 if big else 0.62
	var top = y0 + hb
	var ch = 0.72 if big else 0.58
	var body = SurfaceTool.new()
	body.begin(Mesh.PRIMITIVE_TRIANGLES)
	var gl = SurfaceTool.new()
	gl.begin(Mesh.PRIMITIVE_TRIANGLES)
	var lt = SurfaceTool.new()
	lt.begin(Mesh.PRIMITIVE_TRIANGLES)
	var dark = Color(0.08, 0.08, 0.09)
	_vbox(body, Vector3(0, y0 + hb * 0.5, 0), Vector3(Wd, hb, L), col)
	_vbox(body, Vector3(0, y0 + 0.05, L * 0.5 + 0.04), Vector3(Wd - 0.1, 0.28, 0.12), dark)
	_vbox(body, Vector3(0, y0 + 0.05, -L * 0.5 - 0.04), Vector3(Wd - 0.1, 0.28, 0.12), dark)
	_vbox(body, Vector3(0, y0 - 0.02, 0), Vector3(Wd + 0.02, 0.14, L - 0.5), dark)
	var z0 = -L * (0.42 if big else 0.30)
	var z1 = L * (0.22 if big else 0.16)
	var bx = Wd * 0.5 - 0.07
	var tx = Wd * 0.5 - 0.2
	var tz0 = z0 + (0.1 if big else 0.25)
	var tz1 = z1 - (0.2 if big else 0.45)
	var y1 = top + ch
	var B0 = Vector3(-bx, top, z0)
	var B1 = Vector3(bx, top, z0)
	var B2 = Vector3(bx, top, z1)
	var B3 = Vector3(-bx, top, z1)
	var T0 = Vector3(-tx, y1, tz0)
	var T1 = Vector3(tx, y1, tz0)
	var T2 = Vector3(tx, y1, tz1)
	var T3 = Vector3(-tx, y1, tz1)
	_quad(gl, B3, B2, T2, T3, Vector3(0, 0.5, 1))
	_quad(gl, B0, B1, T1, T0, Vector3(0, 0.5, -1))
	_quad(gl, B1, B2, T2, T1, Vector3(1, 0.3, 0))
	_quad(gl, B0, B3, T3, T0, Vector3(-1, 0.3, 0))
	_quad(body, T0, T1, T2, T3, Vector3.UP, col)
	var mz = (tz0 + tz1) * 0.5
	_quad(body, Vector3(bx + 0.005, top, mz - 0.06), Vector3(bx + 0.005, top, mz + 0.06), Vector3(tx + 0.005, y1, mz + 0.06), Vector3(tx + 0.005, y1, mz - 0.06), Vector3(1, 0.3, 0), col)
	_quad(body, Vector3(-bx - 0.005, top, mz - 0.06), Vector3(-bx - 0.005, top, mz + 0.06), Vector3(-tx - 0.005, y1, mz + 0.06), Vector3(-tx - 0.005, y1, mz - 0.06), Vector3(-1, 0.3, 0), col)
	for sx in [-1.0, 1.0]:
		for sz in [-1.0, 1.0]:
			_vwheel(body, Vector3(sx * (Wd * 0.5 - 0.06), 0.33, sz * L * 0.31), 0.33, 0.24, Color(0.05, 0.05, 0.05), Color(0.7, 0.72, 0.75))
		_vbox(lt, Vector3(sx * (Wd * 0.5 - 0.3), y0 + 0.4, L * 0.5 + 0.02), Vector3(0.38, 0.15, 0.05), Color(1, 0.96, 0.85))
		_vbox(lt, Vector3(sx * (Wd * 0.5 - 0.28), y0 + 0.45, -L * 0.5 - 0.02), Vector3(0.36, 0.14, 0.05), Color(1, 0.1, 0.08))
	if police:
		_vbox(body, Vector3(0, y0 + 0.36, -0.05), Vector3(Wd + 0.02, 0.32, L * 0.42), Color(0.07, 0.17, 0.45))
		_vbox(body, Vector3(0, y1 + 0.05, mz), Vector3(1.1, 0.1, 0.3), dark)
	if taxi:
		_vbox(lt, Vector3(0, y1 + 0.13, mz), Vector3(0.6, 0.22, 0.32), Color(1, 0.85, 0.2))
	var am = ArrayMesh.new()
	body.commit(am)
	am.surface_set_material(0, _mat("cbody", Color.WHITE, 0.32, 0.45, true))
	(am.surface_get_material(0) as StandardMaterial3D).vertex_color_use_as_albedo = true
	gl.commit(am)
	am.surface_set_material(1, _mat("glass", Color(0.04, 0.05, 0.07), 0.04, 0.6))
	lt.commit(am)
	var lm = _mat("clights", Color.WHITE, 0.3, 0.0)
	lm.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	lm.vertex_color_use_as_albedo = true
	am.surface_set_material(2, lm)
	geo_cache[key] = am
	return am

func _lightbar(parent: Node3D, y: float, z: float) -> Array:
	var out: Array = []
	for side in [-1.0, 1.0]:
		var lm = MeshInstance3D.new()
		var lb = BoxMesh.new()
		lb.size = Vector3(0.48, 0.12, 0.26)
		lm.mesh = lb
		lm.material_override = M.lb_off_r if side < 0 else M.lb_off_b
		lm.position = Vector3(side * 0.28, y, z)
		parent.add_child(lm)
		out.append(lm)
	return out

# ---------------------------------------------------------------- araçlar
func make_car(kind: String, p: Vector2, dir: int, col: Color = Color.WHITE) -> NCar:
	var c = NCar.new()
	c.kind = kind
	c.pos = p
	c.dir = dir
	c.plate = C.plate()
	c.big = kind == "civ" and randf() < 0.15
	if kind == "cop":
		c.col = Color(0.95, 0.95, 0.96)
	elif col == Color.WHITE:
		c.col = Color(C.pick(C.COLORS)[1])
	else:
		c.col = col
	c.hl = absf(p.y - roundf(p.y / C.S) * C.S) > 6.0
	var body = AnimatableBody3D.new()
	body.sync_to_physics = false
	var cs = CollisionShape3D.new()
	var bs = BoxShape3D.new()
	bs.size = Vector3(1.85, 1.3, 4.3)
	cs.shape = bs
	cs.position.y = 0.75
	body.add_child(cs)
	var mi = MeshInstance3D.new()
	mi.mesh = car_mesh(c.col, c.big, kind == "cop", c.col.r > 0.8 and c.col.g > 0.6 and c.col.b < 0.3)
	body.add_child(mi)
	if kind == "cop":
		c.lb = _lightbar(body, 1.62, -0.3)
	body.set_meta("ncar", c)
	add_child(body)
	c.node = body
	c.vis = mi
	c.yaw = C.dir_yaw(dir)
	_upd_base(c)
	c.v = c.base * 0.8
	_set_ni(c)
	_place(c)
	cars.append(c)
	return c

func on_hwy(c: NCar) -> bool:
	return c.dir % 2 == 0 and absf(c.pos.y) < C.HW * 0.5

func _upd_base(c: NCar) -> void:
	if c.wreck:
		return
	if c.flee:
		c.base = 40.0 if on_hwy(c) else 24.0
	elif c.backup:
		c.base = 40.0 if on_hwy(c) else 26.0
	elif c.speeder and on_hwy(c):
		c.base = c.sbase if c.sbase > 0 else 44.0
	else:
		c.base = (randf_range(25, 31) if on_hwy(c) else randf_range(9, 13)) * (0.86 if M.weather == "rain" else 1.0)

func _lane_off(k: int, c: NCar) -> float:
	return (9.0 if c.hl else 3.2) if k == 0 else 3.5

func _set_ni(c: NCar) -> void:
	var p = c.pos.x if c.dir % 2 == 0 else c.pos.y
	c.ni = (floorf(p / C.S) + 1) * C.S if c.dir < 2 else (ceilf(p / C.S) - 1) * C.S

func _valid(c: NCar, d: int) -> bool:
	var q: Vector2 = c.pos + C.DV[d] * C.S * 0.9
	return absf(q.x) < C.LIM - 2 and absf(q.y) < C.LIM - 2

func _target(c: NCar):
	var m = M.game.mission
	if m == null:
		return null
	if m.get("car") != null and (m["car"] as NCar).flee:
		return (m["car"] as NCar).pos
	if m.get("sus") != null:
		return (m["sus"] as NPed).pos
	return m["p"]

func _turn(c: NCar) -> void:
	var mk = roundi(c.ni / C.S)
	var opts: Array = [c.dir, (c.dir + 1) % 4, (c.dir + 3) % 4].filter(func(d): return _valid(c, d))
	if opts.is_empty():
		opts = [(c.dir + 2) % 4]
	var nd = c.dir
	if c.flee or c.backup:
		var tg = M.player_pos() if c.flee else _target(c)
		var best = -1e9
		for d in opts:
			var dv: Vector2 = C.DV[d]
			var sc = 0.0
			if tg != null:
				sc = (c.pos - (tg as Vector2)).dot(dv)
			if c.backup:
				sc = -sc
			sc += randf() * (30.0 if c.flee else 10.0)
			if sc > best:
				best = sc
				nd = d
	else:
		nd = c.dir if (randf() < 0.58 and opts.has(c.dir)) else C.pick(opts)
	if nd != c.dir:
		if nd == (c.dir + 2) % 4:
			if nd % 2 == 0:
				var k = roundi(c.pos.y / C.S)
				c.pos.y = k * C.S + (_lane_off(k, c) if nd == 0 else -_lane_off(k, c))
			else:
				var k2 = roundi(c.pos.x / C.S)
				c.pos.x = k2 * C.S + (-3.5 if nd == 1 else 3.5)
		elif nd % 2 == 1:
			c.pos.x = mk * C.S + (-3.5 if nd == 1 else 3.5)
		else:
			c.hl = randf() < 0.5
			var off = _lane_off(mk, c)
			c.pos.y = mk * C.S + (off if nd == 0 else -off)
		c.dir = nd
		_upd_base(c)
	_set_ni(c)

func _place(c: NCar) -> void:
	var dv: Vector2 = C.DV[c.dir]
	var rt = Vector2(-dv.y, dv.x)
	var pp = c.pos + rt * c.pull * 0.1
	c.node.global_position = Vector3(pp.x, 0.0, pp.y)
	c.node.rotation = Vector3(0.04 if c.wreck else 0.0, c.yaw, -0.03 if c.wreck else 0.0)

func upd_car(c: NCar, dt: float) -> void:
	if c.wreck:
		_place(c)
		return
	var dv: Vector2 = C.DV[c.dir]
	var tv = c.base
	if c.stopping or c.stopped:
		tv = 0.0
	if c.angry > 0:
		c.angry -= dt
		tv = 0.0
	var pp: Vector2 = M.player_pos()
	if not c.flee and not c.backup:
		var ahead = 1e9
		for o in cars:
			if o == c:
				continue
			var r: Vector2 = o.pos - c.pos
			var f = r.dot(dv)
			if f <= 0 or f > ahead:
				continue
			if absf(r.x * dv.y - r.y * dv.x) < 1.6:
				ahead = f
		var pc: Vector2 = M.car_pos()
		var r2 = pc - c.pos
		var f2 = r2.dot(dv)
		if f2 > 0 and absf(r2.x * dv.y - r2.y * dv.x) < 2.0 and f2 < ahead:
			ahead = f2
		if M.mode == "foot":
			var r3 = pp - c.pos
			var f3 = r3.dot(dv)
			if f3 > 0 and absf(r3.x * dv.y - r3.y * dv.x) < 1.4 and f3 < ahead:
				ahead = f3
		if ahead < 5.5:
			tv = 0.0
		elif ahead < 12.0:
			tv = minf(tv, (ahead - 5.5) * 2.4)
		if c.speeder and on_hwy(c) and ahead < 16.0:
			c.hl = not c.hl
			c.pos.y = _lane_off(0, c) if c.dir == 0 else -_lane_off(0, c)
		if M.siren and c.kind != "cop" and c.pos.distance_to(pp) < 30.0:
			tv *= 0.3
			c.pull = minf(14.0, c.pull + dt * 22.0)
		else:
			c.pull = maxf(0.0, c.pull - dt * 10.0)
	if c.backup:
		var tg = _target(c)
		var m = M.game.mission
		if m != null and m.get("car") != null and (m["car"] as NCar).flee and c.pos.distance_to((m["car"] as NCar).pos) < 14.0:
			(m["car"] as NCar).hp -= dt * 12.0 * (c.skill / 60.0)
	c.v = lerpf(c.v, tv, 1.0 - exp(-(4.5 if tv < c.v else 1.6) * dt))
	if c.stopping and c.v < 0.4:
		c.stopping = false
		c.stopped = true
		c.v = 0.0
	var step = c.v * dt
	c.pos += dv * step
	var p1 = c.pos.x if c.dir % 2 == 0 else c.pos.y
	if (p1 - c.ni) * (1.0 if c.dir < 2 else -1.0) >= 0:
		_turn(c)
	c.yaw = lerp_angle(c.yaw, C.dir_yaw(c.dir), 1.0 - exp(-10.0 * dt))
	_place(c)
	if c.lb.size() == 2:
		var on = c.backup
		c.lb[0].material_override = M.lb_on_r if on else M.lb_off_r
		c.lb[1].material_override = M.lb_on_b if on else M.lb_off_b

func remove_car(c: NCar) -> void:
	cars.erase(c)
	if is_instance_valid(c.node):
		c.node.queue_free()

func spawn_car(kind: String, dmin: float = 60.0, dmax: float = 120.0) -> NCar:
	var pp: Vector2 = M.player_pos()
	for i in 12:
		var ang = randf() * TAU
		var d = randf_range(dmin, dmax)
		var q = pp + Vector2(cos(ang), sin(ang)) * d
		var kx = roundi(q.x / C.S)
		var kz = roundi(q.y / C.S)
		if absi(kx) > C.WB or absi(kz) > C.WB:
			continue
		var s: Vector2
		var dir: int
		if absf(q.x - kx * C.S) < absf(q.y - kz * C.S):
			dir = 1 if randf() < 0.5 else 3
			s = Vector2(kx * C.S + (-3.5 if dir == 1 else 3.5), q.y)
		else:
			dir = 0 if randf() < 0.5 else 2
			var off = (3.2 if randf() < 0.5 else 9.0) if kz == 0 else 3.5
			s = Vector2(q.x, kz * C.S + (off if dir == 0 else -off))
		if absf(s.x) > C.LIM - 6 or absf(s.y) > C.LIM - 6:
			continue
		var clash = false
		for o in cars:
			if (o as NCar).pos.distance_to(s) < 7.0:
				clash = true
		if clash:
			continue
		return make_car(kind, s, dir)
	return null

# ---------------------------------------------------------------- yayalar
func _human() -> Node3D:
	if human_scene == null:
		human_scene = load("res://assets/models/human.glb")
	return human_scene.instantiate()

func make_ped(kind: String, p: Vector2, col: Color = Color(0, 0, 0, 0)) -> NPed:
	var e = NPed.new()
	e.kind = kind
	e.pos = p
	e.col = col if col.a > 0 else Color.from_hsv(randf(), randf_range(0.3, 0.7), randf_range(0.35, 0.8))
	var holder = Node3D.new()
	var inst = _human()
	holder.add_child(inst)
	var a: AABB = M.aabb_of(inst, holder)
	var sc = randf_range(1.68, 1.85) / maxf(a.size.y, 0.001)
	inst.scale = Vector3.ONE * sc
	inst.position.y = -a.position.y * sc
	var mat = StandardMaterial3D.new()
	mat.albedo_color = e.col
	mat.roughness = 0.75
	for mi in inst.find_children("*", "MeshInstance3D", true, false):
		(mi as MeshInstance3D).material_override = mat
	e.anim = inst.find_child("AnimationPlayer", true, false) as AnimationPlayer
	if e.anim:
		for n in ["idle", "run", "fight_idle"]:
			if e.anim.has_animation(n):
				e.anim.get_animation(n).loop_mode = Animation.LOOP_LINEAR
		e.anim.play("idle")
		e.anim.seek(randf() * 1.5)
	if kind == "suspect":
		var mk = MeshInstance3D.new()
		var cm = CylinderMesh.new()
		cm.top_radius = 0.0
		cm.bottom_radius = 0.22
		cm.height = 0.45
		cm.radial_segments = 4
		mk.mesh = cm
		mk.rotation.x = PI
		mk.position.y = 2.35
		var mm = StandardMaterial3D.new()
		mm.albedo_color = Color(1, 0.15, 0.1)
		mm.emission_enabled = true
		mm.emission = Color(1, 0.1, 0.05)
		mm.emission_energy_multiplier = 2.0
		mk.material_override = mm
		holder.add_child(mk)
		e.marker = mk
	add_child(holder)
	e.node = holder
	peds.append(e)
	_place_ped(e)
	return e

func _place_ped(e: NPed) -> void:
	var y = 0.0 if M.city.is_road(e.pos) else 0.18
	e.node.global_position = Vector3(e.pos.x, y + (0.25 if e.down else 0.0), e.pos.y)
	e.node.rotation = Vector3(0, C.yaw_of(e.face), PI / 2 if e.down else 0.0)

func _walker_pos(e: NPed) -> Vector2:
	var r = C.block_rect(e.bx, e.bz).grow(-1.4)
	var w = r.size.x
	var h = r.size.y
	var s = fposmod(e.s, e.per)
	if s < w:
		return Vector2(r.position.x + s, r.position.y)
	elif s < w + h:
		return Vector2(r.end.x, r.position.y + (s - w))
	elif s < 2 * w + h:
		return Vector2(r.end.x - (s - w - h), r.end.y)
	return Vector2(r.position.x, r.end.y - (s - 2 * w - h))

func spawn_walker() -> void:
	var pp: Vector2 = M.player_pos()
	for i in 10:
		var ang = randf() * TAU
		var d = randf_range(30, 90)
		var q = pp + Vector2(cos(ang), sin(ang)) * d
		var bx = floori(q.x / C.S)
		var bz = floori(q.y / C.S)
		if bx < -C.WB or bx >= C.WB or bz < -C.WB or bz >= C.WB:
			continue
		var e = make_ped("civ", q)
		e.ai = "walk"
		e.bx = bx
		e.bz = bz
		var r = C.block_rect(bx, bz).grow(-1.4)
		e.per = 2 * (r.size.x + r.size.y)
		e.s = randf() * e.per
		e.ds = 1.0 if randf() < 0.5 else -1.0
		e.v = randf_range(1.1, 1.7)
		e.pos = _walker_pos(e)
		return

func set_anim(e: NPed, n: String, blend: float = 0.2) -> void:
	if e.anim and e.anim.has_animation(n) and e.anim.current_animation != n:
		e.anim.play(n, blend)
		if n == "run":
			e.anim.speed_scale = 1.0 if e.ai == "flee" else 0.55
		else:
			e.anim.speed_scale = 1.0

func upd_ped(e: NPed, dt: float) -> void:
	if e.down:
		e.down_t -= dt
		_place_ped(e)
		return
	var old = e.pos
	match e.ai:
		"walk":
			e.s += e.ds * e.v * dt
			e.pos = _walker_pos(e)
		"fight":
			e.pos = e.center + Vector2(e.side + sin(Time.get_ticks_msec() / 180.0 + e.side) * 0.5, cos(Time.get_ticks_msec() / 230.0) * 0.3)
			e.face = Vector2(-signf(e.side), 0)
			set_anim(e, "fight_idle")
		"flee":
			var pp: Vector2 = M.player_pos()
			var away = e.pos - pp
			var dl = maxf(away.length(), 0.01)
			e.stam -= dt
			var spd = 5.6 if e.stam > 0 else 3.6
			e.wa += (randf() - 0.5) * dt * 3.0
			var mv = away / dl + Vector2(cos(e.wa), sin(e.wa)) * 0.55
			e.pos += mv.normalized() * spd * dt
			var res: Array = M.city.collide_circle(e.pos, 0.4)
			e.pos = res[0]
			if res[1]:
				e.wa += 1.6 * (1.0 if randf() < 0.5 else -1.0)
	var mvd = e.pos - old
	if mvd.length() > 0.001 and e.ai != "fight":
		e.face = mvd.normalized()
		set_anim(e, "run")
	elif e.ai != "fight":
		set_anim(e, "idle")
	# oyuncu aracı çarpması
	if M.mode == "car" and e.ai != "held":
		var cp: Vector2 = M.car_pos()
		var d = e.pos.distance_to(cp)
		var spd2: float = M.car_speed()
		if d < 1.6 and spd2 > 6.0:
			M.game.hit_ped(e)
		elif d < 5.0 and spd2 > 5.0 and e.ai == "walk":
			e.pos += (e.pos - cp).normalized() * 3.0 * dt
	if e.marker:
		e.marker.position.y = 2.35 + sin(Time.get_ticks_msec() / 200.0) * 0.12
		e.marker.rotation.y += dt * 3.0
	_place_ped(e)

func remove_ped(e: NPed) -> void:
	peds.erase(e)
	if is_instance_valid(e.node):
		e.node.queue_free()

# ---------------------------------------------------------------- yönetim
func clear_all() -> void:
	for c in cars.duplicate():
		remove_car(c)
	for e in peds.duplicate():
		remove_ped(e)

func traffic_mul() -> float:
	var h: float = M.game.G["time"] / 60.0
	var m = 1.0
	if (h >= 7 and h < 9.5) or (h >= 17 and h < 19.5):
		m = 1.7
	elif h >= 22 or h < 6:
		m = 0.45
	return m * (0.85 if M.weather == "rain" else 1.0)

func ped_mul() -> float:
	var h: float = M.game.G["time"] / 60.0
	var m = 0.4 if (h >= 22 or h < 6) else (1.2 if (h >= 11 and h < 20) else 0.9)
	if M.weather == "rain":
		m *= 0.5
	return m

func update(dt: float) -> void:
	var pp: Vector2 = M.player_pos()
	spawn_t -= dt
	if spawn_t <= 0:
		spawn_t = 0.4
		for c in cars.duplicate():
			if not c.keep and c.pos.distance_to(pp) > 170.0:
				remove_car(c)
		for e in peds.duplicate():
			if not e.keep and (e.pos.distance_to(pp) > 120.0 or (e.down and e.down_t <= 0)):
				remove_ped(e)
		var civ = 0
		var cops = 0
		for c in cars:
			if c.kind == "civ" and not c.keep:
				civ += 1
			if c.kind == "cop" and not c.backup:
				cops += 1
		if civ < roundi(14 * traffic_mul()):
			spawn_car("civ")
		if cops < 2:
			spawn_car("cop", 90, 150)
		var np = 0
		for e in peds:
			if e.kind == "civ" and not e.keep:
				np += 1
		if np < roundi(10 * ped_mul()):
			spawn_walker()
		if absf(pp.y) < 90 and randf() < 0.05:
			var spd = 0
			for c in cars:
				if c.speeder:
					spd += 1
			if spd < 2:
				var dir = 0 if randf() < 0.5 else 2
				var x = clampf(pp.x + (-1.0 if randf() < 0.5 else 1.0) * randf_range(80, 130), -C.LIM + 20, C.LIM - 20)
				var c2 = make_car("civ", Vector2(x, 9.0 if dir == 0 else -9.0), dir)
				c2.speeder = true
				c2.sbase = randf_range(39, 48)
				_upd_base(c2)
				c2.v = c2.base
	for c in cars:
		upd_car(c, dt)
	for e in peds:
		upd_ped(e, dt)
