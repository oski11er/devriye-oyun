extends Node3D
## Devriye: Şehrin Nabzı — native 3D prototip (Godot 4.3)
## Fotogerçekçi PBR şehir, fizikli polis aracı, animasyonlu memur, 1./3. şahıs kamera, dokunmatik yön tuşları.

const T := "res://assets/tex/"
const BLK := 64.0
const ROAD := 14.0
const NB := 3

var env: Environment
var sun: DirectionalLight3D
var sky_day: Sky
var sky_night: Sky
var car: VehicleBody3D
var car_scene: PackedScene
var human_scene: PackedScene
var lb_r: OmniLight3D
var lb_b: OmniLight3D
var lb_mat_r := StandardMaterial3D.new()
var lb_mat_b := StandardMaterial3D.new()
var head_l: SpotLight3D
var head_r: SpotLight3D
var player: CharacterBody3D
var pvis: Node3D
var panim: AnimationPlayer
var cam: Camera3D
var mode := "car"
var view := "tps"
var siren := false
var night := false
var steer := 0.0
var cam_yaw := 0.0
var cam_pitch := -0.15
var cam_free := 0.0
var joy_vec := Vector2.ZERO
var joy_id := -1
var joy_origin := Vector2.ZERO
var look_id := -1
var look_last := Vector2.ZERO
var ui_layer: CanvasLayer
var hud: Label
var btns := {}
var joy_ctrl: Control
var mat_cache := {}
var glass: StandardMaterial3D
var glass_lit: StandardMaterial3D
var lamp_head: StandardMaterial3D
var lamp_lights: Array = []
var shots := false
var t_acc := 0.0
var cam_override := false
var _sts := {}
var win_unlit: Array = []
var win_lit: Array = []
var win_frames: Array = []
var driver_seat := Vector3(0.36, 1.12, -0.15)

# ---------------------------------------------------------------- yardımcılar
func tex(n: String) -> Texture2D:
	var p := T + n
	if ResourceLoader.exists(p):
		return load(p)
	return null

func pbr(role: String, scale: float, tint: Color = Color.WHITE, fallback: Color = Color(0.5, 0.5, 0.5)) -> StandardMaterial3D:
	var key := role + str(scale) + tint.to_html()
	if mat_cache.has(key):
		return mat_cache[key]
	var m := StandardMaterial3D.new()
	var d := tex(role + "_diff.jpg")
	if d:
		m.albedo_texture = d
		m.albedo_color = tint
	else:
		m.albedo_color = fallback * tint
	var n := tex(role + "_nor.jpg")
	if n:
		m.normal_enabled = true
		m.normal_texture = n
	var r := tex(role + "_rough.jpg")
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

func plain(c: Color, rough: float, metal: float = 0.0) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = c
	m.roughness = rough
	m.metallic = metal
	return m

func _xform_to(n: Node3D, anc: Node3D) -> Transform3D:
	var xf := Transform3D()
	var cur: Node = n
	while cur != null and cur != anc:
		if cur is Node3D:
			xf = (cur as Node3D).transform * xf
		cur = cur.get_parent()
	return xf

func _aabb(n: Node, anc: Node3D, filter: String = "") -> AABB:
	var out := AABB()
	var first := true
	for mi in n.find_children("*", "MeshInstance3D", true, false):
		var m := mi as MeshInstance3D
		if m.mesh == null:
			continue
		if filter != "" and not _has_mat(m, filter):
			continue
		var a: AABB = _xform_to(m, anc) * m.get_aabb()
		if first:
			out = a
			first = false
		else:
			out = out.merge(a)
	return out

func _has_mat(m: MeshInstance3D, filter: String) -> bool:
	for i in m.mesh.get_surface_count():
		var mat := m.mesh.surface_get_material(i)
		if mat and mat.resource_name.findn(filter) >= 0:
			return true
	return false

# ---------------------------------------------------------------- kurulum
func _ready() -> void:
	shots = "--shots" in OS.get_cmdline_user_args()
	if shots:
		DisplayServer.window_set_size(Vector2i(1280, 720))
	_setup_input()
	_setup_env()
	_setup_materials()
	_build_city()
	_spawn_car(Vector3(BLK * 0.5, 0.6, ROAD * 0.25 + 0.2), PI / 2)
	_spawn_player(Vector3(BLK * 0.5 - 6, 0.5, ROAD * 0.5 + 2.5))
	_spawn_extras()
	cam = Camera3D.new()
	cam.fov = 68.0
	cam.far = 900.0
	add_child(cam)
	cam.current = true
	_build_ui()
	_set_mode("car")
	if shots:
		_run_shots()

func _setup_input() -> void:
	var keys := {"steer_left": [KEY_LEFT, KEY_A], "steer_right": [KEY_RIGHT, KEY_D], "gas": [KEY_UP, KEY_W], "brake": [KEY_DOWN, KEY_S], "hand": [KEY_SPACE]}
	for a in keys.keys():
		if not InputMap.has_action(a):
			InputMap.add_action(a)
		for k in keys[a]:
			var ev := InputEventKey.new()
			ev.physical_keycode = k
			InputMap.action_add_event(a, ev)

func _make_sky(hdr_name: String, is_night: bool) -> Sky:
	var s := Sky.new()
	var h := tex(hdr_name)
	if h:
		var pm := PanoramaSkyMaterial.new()
		pm.panorama = h
		if is_night:
			pm.set("energy_multiplier", 0.5)
		s.sky_material = pm
	else:
		var ps := ProceduralSkyMaterial.new()
		if is_night:
			ps.sky_top_color = Color(0.01, 0.015, 0.04)
			ps.sky_horizon_color = Color(0.05, 0.06, 0.1)
			ps.ground_horizon_color = Color(0.03, 0.03, 0.04)
			ps.ground_bottom_color = Color(0, 0, 0)
		s.sky_material = ps
	s.radiance_size = Sky.RADIANCE_SIZE_256
	return s

func _setup_env() -> void:
	sky_day = _make_sky("sky_day.hdr", false)
	sky_night = _make_sky("sky_night.hdr", true)
	env = Environment.new()
	env.background_mode = Environment.BG_SKY
	env.sky = sky_day
	env.ambient_light_source = Environment.AMBIENT_SOURCE_SKY
	env.reflected_light_source = Environment.REFLECTION_SOURCE_SKY
	env.tonemap_mode = Environment.TONE_MAPPER_ACES
	env.tonemap_white = 6.0
	env.glow_enabled = true
	env.glow_intensity = 0.45
	env.glow_hdr_threshold = 1.3
	env.fog_enabled = true
	env.fog_light_color = Color(0.74, 0.79, 0.86)
	env.fog_density = 0.0018
	env.fog_aerial_perspective = 0.75
	env.adjustment_enabled = true
	env.adjustment_contrast = 1.06
	env.adjustment_saturation = 1.07
	var we := WorldEnvironment.new()
	we.environment = env
	add_child(we)
	sun = DirectionalLight3D.new()
	sun.shadow_enabled = true
	sun.light_energy = 2.0
	sun.light_color = Color(1.0, 0.95, 0.86)
	sun.rotation_degrees = Vector3(-40, -32, 0)
	sun.directional_shadow_max_distance = 150.0
	sun.directional_shadow_mode = DirectionalLight3D.SHADOW_PARALLEL_4_SPLITS
	sun.shadow_blur = 1.4
	add_child(sun)

func _setup_materials() -> void:
	glass = plain(Color(0.06, 0.08, 0.1), 0.05, 0.9)
	glass_lit = plain(Color(0.08, 0.08, 0.08), 0.08, 0.6)
	glass_lit.emission_enabled = true
	glass_lit.emission = Color(1.0, 0.8, 0.52)
	glass_lit.emission_energy_multiplier = 0.0
	lamp_head = plain(Color(0.9, 0.9, 0.85), 0.3)
	lamp_head.emission_enabled = true
	lamp_head.emission = Color(1.0, 0.86, 0.6)
	lamp_head.emission_energy_multiplier = 0.2
	for m in [lb_mat_r, lb_mat_b]:
		m.emission_enabled = true
	lb_mat_r.albedo_color = Color(0.4, 0, 0)
	lb_mat_r.emission = Color(1, 0.05, 0.05)
	lb_mat_b.albedo_color = Color(0, 0, 0.4)
	lb_mat_b.emission = Color(0.1, 0.25, 1)

# ---------------------------------------------------------------- şehir
func _st(key: String) -> SurfaceTool:
	if not _sts.has(key):
		var s := SurfaceTool.new()
		s.begin(Mesh.PRIMITIVE_TRIANGLES)
		_sts[key] = s
	return _sts[key]

func add_box(key: String, center: Vector3, size: Vector3, collide: bool = false) -> void:
	var bm := BoxMesh.new()
	bm.size = size
	_st(key).append_from(bm, 0, Transform3D(Basis(), center))
	if collide:
		var sb := StaticBody3D.new()
		sb.position = center
		var cs := CollisionShape3D.new()
		var bs := BoxShape3D.new()
		bs.size = size
		cs.shape = bs
		sb.add_child(cs)
		add_child(sb)

func add_mesh(key: String, mesh: Mesh, xf: Transform3D) -> void:
	_st(key).append_from(mesh, 0, xf)

func _city_mat(key: String) -> Material:
	match key:
		"pave": return pbr("pavement", 0.35, Color(0.95, 0.95, 0.93), Color(0.62, 0.61, 0.58))
		"brick": return pbr("brick", 0.4, Color(1, 1, 1), Color(0.55, 0.3, 0.24))
		"plaster": return pbr("plaster", 0.3, Color(0.96, 0.93, 0.86), Color(0.85, 0.82, 0.75))
		"plaster_blue": return pbr("plaster", 0.3, Color(0.75, 0.84, 1.0), Color(0.7, 0.78, 0.9))
		"concrete": return pbr("concrete", 0.3, Color(0.92, 0.92, 0.92), Color(0.6, 0.6, 0.6))
		"grass": return pbr("grass", 0.25, Color(0.85, 1.0, 0.8), Color(0.25, 0.42, 0.18))
		"store": return glass
		"trim": return plain(Color(0.78, 0.77, 0.74), 0.7)
		"roof": return pbr("concrete", 0.3, Color(0.55, 0.55, 0.57), Color(0.35, 0.35, 0.36))
		"metal": return plain(Color(0.6, 0.62, 0.64), 0.4, 0.6)
		"dark": return plain(Color(0.12, 0.13, 0.14), 0.5, 0.3)
		"white": return plain(Color(0.92, 0.92, 0.9), 0.6)
		"yellow": return plain(Color(0.9, 0.7, 0.12), 0.6)
		"bark": return plain(Color(0.3, 0.22, 0.15), 0.9)
		"leaf": return plain(Color(0.2, 0.36, 0.14), 0.85)
		"navy": return plain(Color(0.08, 0.16, 0.38), 0.5)
	return plain(Color.MAGENTA, 0.5)

func _flush(cast: bool = true) -> void:
	var am := ArrayMesh.new()
	for key in _sts.keys():
		var s: SurfaceTool = _sts[key]
		s.commit(am)
		am.surface_set_material(am.get_surface_count() - 1, _city_mat(key))
	_sts.clear()
	var mi := MeshInstance3D.new()
	mi.mesh = am
	mi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_ON if cast else GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(mi)

func _build_city() -> void:
	var size := BLK * NB * 2 + 400.0
	var g := MeshInstance3D.new()
	var pm := PlaneMesh.new()
	pm.size = Vector2(size, size)
	g.mesh = pm
	g.material_override = pbr("asphalt", 0.18, Color(0.85, 0.85, 0.85), Color(0.2, 0.2, 0.21))
	add_child(g)
	var sb := StaticBody3D.new()
	var cs := CollisionShape3D.new()
	cs.shape = WorldBoundaryShape3D.new()
	sb.add_child(cs)
	add_child(sb)
	var rng := RandomNumberGenerator.new()
	rng.seed = 1337
	for bx in range(-NB, NB):
		for bz in range(-NB, NB):
			_build_block(bx, bz, rng)
	_build_windows()
	_build_markings()
	_build_lamps()

func _build_block(bx: int, bz: int, rng: RandomNumberGenerator) -> void:
	var x0 := bx * BLK + ROAD * 0.5
	var z0 := bz * BLK + ROAD * 0.5
	var w := BLK - ROAD
	var cx := x0 + w * 0.5
	var cz := z0 + w * 0.5
	add_box("pave", Vector3(cx, 0.09, cz), Vector3(w, 0.18, w), true)
	# bordür taşı
	for s in [-1.0, 1.0]:
		add_box("trim", Vector3(cx, 0.1, cz + s * (w * 0.5 - 0.15)), Vector3(w, 0.2, 0.3))
		add_box("trim", Vector3(cx + s * (w * 0.5 - 0.15), 0.1, cz), Vector3(0.3, 0.2, w))
	var station := bx == 0 and bz == 0
	if not station and rng.randf() < 0.14:
		add_box("grass", Vector3(cx, 0.19, cz), Vector3(w - 8, 0.03, w - 8))
		for i in 7:
			_tree(Vector3(cx + rng.randf_range(-18, 18), 0.2, cz + rng.randf_range(-18, 18)), rng)
		_flush()
		return
	var inner := w - 7.0
	var half := inner * 0.5
	for ix in 2:
		for iz in 2:
			var lc := Vector3(x0 + 3.5 + ix * half + half * 0.5, 0, z0 + 3.5 + iz * half + half * 0.5)
			var far := absi(bx) + absi(bz) > 2
			var h := rng.randf_range(9.0, 16.0) if far else rng.randf_range(13.0, 40.0)
			var bw := half - rng.randf_range(0.5, 2.5)
			var bd := half - rng.randf_range(0.5, 2.5)
			var mk: String = ["brick", "plaster", "concrete", "brick"][rng.randi() % 4]
			if station and ix == 0 and iz == 0:
				_building(lc, Vector3(half - 0.5, 12.0, half - 0.5), "plaster_blue", rng, true)
			else:
				_building(lc, Vector3(bw, h, bd), mk, rng, false)
	# kaldırım ağaçları
	for i in 4:
		var side := i % 4
		var p := Vector3(cx, 0.18, cz)
		var off := w * 0.5 - 1.6
		var along := rng.randf_range(-w * 0.4, w * 0.4)
		match side:
			0: p += Vector3(along, 0, -off)
			1: p += Vector3(along, 0, off)
			2: p += Vector3(-off, 0, along)
			3: p += Vector3(off, 0, along)
		_tree(p, rng)
	_flush()

func _building(pos: Vector3, size: Vector3, mk: String, rng: RandomNumberGenerator, station: bool) -> void:
	var base := 0.18
	add_box(mk, pos + Vector3(0, base + size.y * 0.5, 0), size, true)
	add_box("store", pos + Vector3(0, base + 1.7, 0), Vector3(size.x + 0.1, 3.3, size.z + 0.1))
	add_box("trim", pos + Vector3(0, base + 3.55, 0), Vector3(size.x + 0.35, 0.4, size.z + 0.35))
	var top := base + size.y
	add_box("roof", pos + Vector3(0, top + 0.02, 0), Vector3(size.x - 0.1, 0.04, size.z - 0.1))
	for s in [-1.0, 1.0]:
		add_box("trim", pos + Vector3(0, top + 0.45, s * (size.z * 0.5 - 0.15)), Vector3(size.x, 0.9, 0.3))
		add_box("trim", pos + Vector3(s * (size.x * 0.5 - 0.15), top + 0.45, 0), Vector3(0.3, 0.9, size.z))
	for i in rng.randi_range(1, 3):
		add_box("metal", pos + Vector3(rng.randf_range(-size.x * 0.3, size.x * 0.3), top + 0.65, rng.randf_range(-size.z * 0.3, size.z * 0.3)), Vector3(1.8, 1.3, 1.3))
	if station:
		add_box("navy", pos + Vector3(0, base + 4.2, 0), Vector3(size.x + 0.4, 0.9, size.z + 0.4))
		var lbl := Label3D.new()
		lbl.text = "POLİS"
		lbl.font_size = 220
		lbl.pixel_size = 0.012
		lbl.outline_size = 24
		lbl.modulate = Color(1, 1, 1)
		lbl.outline_modulate = Color(0.05, 0.15, 0.45)
		lbl.position = pos + Vector3(0, base + 6.0, -size.z * 0.5 - 0.3)
		lbl.rotation.y = PI
		add_child(lbl)
		var lbl2 := lbl.duplicate() as Label3D
		lbl2.position = pos + Vector3(-size.x * 0.5 - 0.3, base + 6.0, 0)
		lbl2.rotation.y = -PI / 2
		add_child(lbl2)
	_windows(pos, size, base, rng)

func _windows(pos: Vector3, size: Vector3, base: float, rng: RandomNumberGenerator) -> void:
	var rows := int((size.y - 5.0) / 3.4)
	var faces := [[Vector3(0, 0, 1), size.x, size.z], [Vector3(0, 0, -1), size.x, size.z], [Vector3(1, 0, 0), size.z, size.x], [Vector3(-1, 0, 0), size.z, size.x]]
	for f in faces:
		var nrm: Vector3 = f[0]
		var flen: float = f[1]
		var depth: float = f[2]
		var cols := int((flen - 1.5) / 3.1)
		if cols < 1:
			continue
		var bas := Basis(Vector3.UP, atan2(nrm.x, nrm.z))
		var tangent := bas.x
		for r in rows:
			for c in cols:
				var u := (c - (cols - 1) * 0.5) * 3.1
				var y := base + 5.2 + r * 3.4
				var p := pos + nrm * (depth * 0.5 + 0.02) + tangent * u + Vector3(0, y, 0)
				var xf := Transform3D(bas, p)
				win_frames.append(Transform3D(bas, p - nrm * 0.01))
				if rng.randf() < 0.38:
					win_lit.append(xf)
				else:
					win_unlit.append(xf)

func _mm(mesh: Mesh, xfs: Array, mat: Material, cast: bool) -> void:
	if xfs.is_empty():
		return
	var mm := MultiMesh.new()
	mm.transform_format = MultiMesh.TRANSFORM_3D
	mm.mesh = mesh
	mm.instance_count = xfs.size()
	for i in xfs.size():
		mm.set_instance_transform(i, xfs[i])
	var mmi := MultiMeshInstance3D.new()
	mmi.multimesh = mm
	mmi.material_override = mat
	mmi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_ON if cast else GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(mmi)

func _build_windows() -> void:
	var wm := BoxMesh.new()
	wm.size = Vector3(1.35, 1.8, 0.1)
	var fm := BoxMesh.new()
	fm.size = Vector3(1.6, 2.05, 0.08)
	_mm(fm, win_frames, plain(Color(0.2, 0.2, 0.21), 0.45, 0.5), false)
	_mm(wm, win_unlit, glass, false)
	_mm(wm, win_lit, glass_lit, false)

func _tree(p: Vector3, rng: RandomNumberGenerator) -> void:
	var h := rng.randf_range(3.0, 4.5)
	var trunk := CylinderMesh.new()
	trunk.top_radius = 0.12
	trunk.bottom_radius = 0.18
	trunk.height = h
	trunk.radial_segments = 8
	add_mesh("bark", trunk, Transform3D(Basis(), p + Vector3(0, h * 0.5, 0)))
	for i in 3:
		var s := SphereMesh.new()
		var r := rng.randf_range(1.2, 1.8)
		s.radius = r
		s.height = r * 1.7
		s.radial_segments = 10
		s.rings = 6
		add_mesh("leaf", s, Transform3D(Basis(), p + Vector3(rng.randf_range(-0.6, 0.6), h + rng.randf_range(-0.3, 0.8), rng.randf_range(-0.6, 0.6))))

func _build_markings() -> void:
	var dash := BoxMesh.new()
	dash.size = Vector3(0.15, 0.02, 3.0)
	var stripe := BoxMesh.new()
	stripe.size = Vector3(0.55, 0.02, 3.2)
	var xs: Array = []
	var cw: Array = []
	var ext := NB * BLK
	for k in range(-NB, NB + 1):
		var c := k * BLK
		var t := -ext
		while t < ext:
			var near := false
			for j in range(-NB, NB + 1):
				if absf(t - j * BLK) < ROAD * 0.5 + 4.5:
					near = true
			if not near:
				xs.append(Transform3D(Basis(), Vector3(c, 0.012, t)))
				xs.append(Transform3D(Basis(Vector3.UP, PI / 2), Vector3(t, 0.012, c)))
			t += 7.0
		for j in range(-NB, NB + 1):
			var ic := Vector3(c, 0.013, j * BLK)
			for s in [-1.0, 1.0]:
				for i in 8:
					var o := (i - 3.5) * 1.5
					cw.append(Transform3D(Basis(), ic + Vector3(o, 0, s * (ROAD * 0.5 + 2.2))))
					cw.append(Transform3D(Basis(Vector3.UP, PI / 2), ic + Vector3(s * (ROAD * 0.5 + 2.2), 0, o)))
	var wmat := plain(Color(0.9, 0.9, 0.86), 0.55)
	_mm(dash, xs, wmat, false)
	_mm(stripe, cw, wmat, false)

func _build_lamps() -> void:
	var pole := CylinderMesh.new()
	pole.top_radius = 0.07
	pole.bottom_radius = 0.1
	pole.height = 7.0
	pole.radial_segments = 8
	var arm := BoxMesh.new()
	arm.size = Vector3(0.1, 0.1, 1.8)
	var head := BoxMesh.new()
	head.size = Vector3(0.45, 0.14, 0.7)
	var pxs: Array = []
	var axs: Array = []
	var hxs: Array = []
	for bx in range(-NB, NB):
		for bz in range(-NB, NB):
			var x0 := bx * BLK + ROAD * 0.5 + 0.8
			var x1 := (bx + 1) * BLK - ROAD * 0.5 - 0.8
			var z0 := bz * BLK + ROAD * 0.5 + 0.8
			var z1 := (bz + 1) * BLK - ROAD * 0.5 - 0.8
			for c in [[x0, z0, -1.0], [x1, z1, 1.0], [(x0 + x1) * 0.5, z0, -1.0], [(x0 + x1) * 0.5, z1, 1.0]]:
				var p := Vector3(c[0], 0.18, c[1])
				var dz: float = c[2]
				pxs.append(Transform3D(Basis(), p + Vector3(0, 3.5, 0)))
				axs.append(Transform3D(Basis(), p + Vector3(0, 6.9, dz * 0.9)))
				var hp := p + Vector3(0, 6.85, dz * 1.75)
				hxs.append(Transform3D(Basis(), hp))
				if Vector2(hp.x - BLK * 0.5, hp.z).length() < 75.0:
					var l := OmniLight3D.new()
					l.position = hp + Vector3(0, -0.4, 0)
					l.light_color = Color(1.0, 0.82, 0.55)
					l.light_energy = 3.0
					l.omni_range = 16.0
					l.omni_attenuation = 1.2
					l.visible = false
					add_child(l)
					lamp_lights.append(l)
	var pm := plain(Color(0.18, 0.19, 0.2), 0.5, 0.6)
	_mm(pole, pxs, pm, true)
	_mm(arm, axs, pm, true)
	_mm(head, hxs, lamp_head, false)

# ---------------------------------------------------------------- araç
func _fit(inst: Node3D, length: float, front_filter: String) -> Node3D:
	var root := Node3D.new()
	var pivot := Node3D.new()
	root.add_child(pivot)
	pivot.add_child(inst)
	var a := _aabb(inst, root)
	if a.size.x > a.size.z:
		pivot.rotation.y = PI / 2
		a = _aabb(inst, root)
	var s := length / maxf(a.size.z, 0.001)
	pivot.scale = Vector3.ONE * s
	a = _aabb(inst, root)
	if front_filter != "":
		var hl := _aabb(inst, root, front_filter)
		if hl.size != Vector3.ZERO and hl.get_center().z < a.get_center().z:
			pivot.rotation.y += PI
			a = _aabb(inst, root)
	pivot.position = -Vector3(a.get_center().x, a.position.y, a.get_center().z)
	root.set_meta("aabb", AABB(Vector3(a.position.x - a.get_center().x, 0, a.position.z - a.get_center().z), a.size))
	return root

func _paint_car(inst: Node, c1: Color, c2: Color) -> void:
	var p1 := StandardMaterial3D.new()
	p1.albedo_color = c1
	p1.metallic = 0.55
	p1.roughness = 0.28
	p1.clearcoat_enabled = true
	p1.clearcoat = 1.0
	p1.clearcoat_roughness = 0.04
	var p2 := p1.duplicate() as StandardMaterial3D
	p2.albedo_color = c2
	var gl := plain(Color(0.03, 0.04, 0.05, 0.55), 0.03, 0.4)
	gl.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	for mi in inst.find_children("*", "MeshInstance3D", true, false):
		var m := mi as MeshInstance3D
		if m.mesh == null:
			continue
		for i in m.mesh.get_surface_count():
			var mat := m.mesh.surface_get_material(i)
			if mat == null:
				continue
			var nm := mat.resource_name
			if nm.begins_with("Paint 1"):
				m.set_surface_override_material(i, p1)
			elif nm.begins_with("Paint 2"):
				m.set_surface_override_material(i, p2)
			elif nm.begins_with("Glass"):
				m.set_surface_override_material(i, gl)

func _car_visual(police: bool, c1: Color, c2: Color) -> Node3D:
	if car_scene == null:
		car_scene = load("res://assets/models/car.glb")
	var inst: Node3D = car_scene.instantiate()
	_paint_car(inst, c1, c2)
	var root := _fit(inst, 4.45, "Headlight")
	if police:
		var a: AABB = root.get_meta("aabb")
		var top := a.size.y
		var bar := MeshInstance3D.new()
		var bm := BoxMesh.new()
		bm.size = Vector3(1.15, 0.1, 0.32)
		bar.mesh = bm
		bar.material_override = plain(Color(0.05, 0.05, 0.06), 0.4, 0.5)
		bar.position = Vector3(0, top + 0.02, -0.25)
		root.add_child(bar)
		for side in [-1.0, 1.0]:
			var lm := MeshInstance3D.new()
			var lb := BoxMesh.new()
			lb.size = Vector3(0.5, 0.13, 0.28)
			lm.mesh = lb
			lm.material_override = lb_mat_r if side < 0 else lb_mat_b
			lm.position = Vector3(side * 0.29, top + 0.1, -0.25)
			root.add_child(lm)
		for side in [-1.0, 1.0]:
			var t := Label3D.new()
			t.text = "POLİS"
			t.font_size = 96
			t.pixel_size = 0.0045
			t.modulate = Color(0.05, 0.15, 0.45)
			t.outline_size = 0
			t.position = Vector3(side * (a.size.x * 0.5 + 0.01), 0.72, -0.1)
			t.rotation.y = side * PI / 2
			root.add_child(t)
	return root

func _spawn_car(pos: Vector3, yaw: float) -> void:
	car = VehicleBody3D.new()
	car.mass = 1350.0
	car.center_of_mass_mode = RigidBody3D.CENTER_OF_MASS_MODE_CUSTOM
	car.center_of_mass = Vector3(0, 0.3, 0)
	car.position = pos
	car.rotation.y = yaw
	add_child(car)
	var cs := CollisionShape3D.new()
	var bs := BoxShape3D.new()
	bs.size = Vector3(1.85, 0.75, 4.3)
	cs.shape = bs
	cs.position = Vector3(0, 0.75, 0)
	car.add_child(cs)
	var vis := _car_visual(true, Color(0.95, 0.95, 0.96), Color(0.06, 0.16, 0.42))
	car.add_child(vis)
	for wp in [Vector3(0.8, 0.4, 1.35), Vector3(-0.8, 0.4, 1.35), Vector3(0.8, 0.4, -1.38), Vector3(-0.8, 0.4, -1.38)]:
		var w := VehicleWheel3D.new()
		w.position = wp
		w.wheel_radius = 0.34
		w.wheel_rest_length = 0.12
		w.suspension_travel = 0.2
		w.suspension_stiffness = 45.0
		w.suspension_max_force = 14000.0
		w.damping_compression = 0.9
		w.damping_relaxation = 1.3
		w.wheel_friction_slip = 2.6
		w.wheel_roll_influence = 0.12
		w.use_as_steering = wp.z > 0
		w.use_as_traction = wp.z < 0
		car.add_child(w)
	var a: AABB = vis.get_meta("aabb")
	lb_r = OmniLight3D.new()
	lb_r.light_color = Color(1, 0.1, 0.1)
	lb_r.omni_range = 14.0
	lb_r.position = Vector3(-0.3, a.size.y + 0.3, -0.25)
	car.add_child(lb_r)
	lb_b = OmniLight3D.new()
	lb_b.light_color = Color(0.15, 0.3, 1)
	lb_b.omni_range = 14.0
	lb_b.position = Vector3(0.3, a.size.y + 0.3, -0.25)
	car.add_child(lb_b)
	for side in [-1.0, 1.0]:
		var s := SpotLight3D.new()
		s.light_color = Color(1, 0.95, 0.85)
		s.spot_range = 45.0
		s.spot_angle = 28.0
		s.light_energy = 0.0
		s.shadow_enabled = false
		s.position = Vector3(side * 0.62, 0.72, 2.25)
		s.rotation = Vector3(-0.06, PI, 0)
		car.add_child(s)
		if side < 0:
			head_l = s
		else:
			head_r = s

# ---------------------------------------------------------------- karakter
func _human(tint: Color, cop: bool) -> Node3D:
	if human_scene == null:
		human_scene = load("res://assets/models/human.glb")
	var inst: Node3D = human_scene.instantiate()
	var holder := Node3D.new()
	holder.add_child(inst)
	var a := _aabb(inst, holder)
	var s := 1.8 / maxf(a.size.y, 0.001)
	inst.scale = Vector3.ONE * s
	a = _aabb(inst, holder)
	inst.position.y -= a.position.y
	var body := StandardMaterial3D.new()
	body.albedo_color = tint
	body.roughness = 0.72
	for mi in inst.find_children("*", "MeshInstance3D", true, false):
		(mi as MeshInstance3D).material_override = body
	if cop:
		var capm := MeshInstance3D.new()
		var cy := CylinderMesh.new()
		cy.top_radius = 0.13
		cy.bottom_radius = 0.135
		cy.height = 0.09
		capm.mesh = cy
		capm.material_override = plain(Color(0.05, 0.08, 0.18), 0.6)
		capm.position = Vector3(0, a.size.y * 0.985, 0.0)
		holder.add_child(capm)
	var ap := inst.find_child("AnimationPlayer", true, false) as AnimationPlayer
	if ap:
		for n in ["idle", "run", "fight_idle"]:
			if ap.has_animation(n):
				ap.get_animation(n).loop_mode = Animation.LOOP_LINEAR
		if ap.has_animation("idle"):
			ap.play("idle")
	holder.set_meta("anim", ap)
	return holder

func _spawn_player(pos: Vector3) -> void:
	player = CharacterBody3D.new()
	player.position = pos
	add_child(player)
	var cs := CollisionShape3D.new()
	var cap := CapsuleShape3D.new()
	cap.radius = 0.35
	cap.height = 1.8
	cs.shape = cap
	cs.position.y = 0.9
	player.add_child(cs)
	pvis = _human(Color(0.1, 0.17, 0.36), true)
	player.add_child(pvis)
	panim = pvis.get_meta("anim")

func _spawn_extras() -> void:
	var rng := RandomNumberGenerator.new()
	rng.seed = 7
	var spots := [Vector3(BLK * 0.5 + 10, 0.18, ROAD * 0.5 + 2.2), Vector3(BLK * 0.5 - 14, 0.18, ROAD * 0.5 + 1.8), Vector3(BLK * 0.5 + 4, 0.18, -ROAD * 0.5 - 2.0)]
	for p in spots:
		var h := _human(Color.from_hsv(rng.randf(), 0.5, 0.6), false)
		h.position = p
		h.rotation.y = rng.randf() * TAU
		add_child(h)
	var paints := [Color(0.55, 0.05, 0.05), Color(0.1, 0.1, 0.11), Color(0.7, 0.72, 0.74)]
	for i in 3:
		var v := _car_visual(false, paints[i], paints[i] * 0.6)
		v.position = Vector3(BLK * 0.5 + 9 + i * 7.0, 0.0, -ROAD * 0.5 + 1.3)
		v.rotation.y = -PI / 2
		add_child(v)

# ---------------------------------------------------------------- arayüz
func _btn_tex(size: Vector2i, col: Color) -> ImageTexture:
	var img := Image.create(size.x, size.y, false, Image.FORMAT_RGBA8)
	var r := 22.0
	for y in size.y:
		for x in size.x:
			var dx := maxf(maxf(r - x, x - (size.x - 1 - r)), 0.0)
			var dy := maxf(maxf(r - y, y - (size.y - 1 - r)), 0.0)
			var d := sqrt(dx * dx + dy * dy)
			if d <= r:
				var edge := r - d < 3.0
				img.set_pixel(x, y, Color(1, 1, 1, 0.55) if edge else col)
	return ImageTexture.create_from_image(img)

func _mk_btn(id: String, label: String, action: String, size: Vector2i, col: Color, fs: int = 30) -> TouchScreenButton:
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
	l.add_theme_font_size_override("font_size", fs)
	l.add_theme_color_override("font_outline_color", Color(0, 0, 0))
	l.add_theme_constant_override("outline_size", 6)
	l.mouse_filter = Control.MOUSE_FILTER_IGNORE
	b.add_child(l)
	b.set_meta("size", Vector2(size))
	ui_layer.add_child(b)
	btns[id] = b
	return b

func _build_ui() -> void:
	ui_layer = CanvasLayer.new()
	add_child(ui_layer)
	var dark := Color(0.06, 0.1, 0.18, 0.62)
	_mk_btn("left", "◀", "steer_left", Vector2i(150, 150), dark, 64)
	_mk_btn("right", "▶", "steer_right", Vector2i(150, 150), dark, 64)
	_mk_btn("gas", "GAZ\n▲", "gas", Vector2i(150, 200), Color(0.08, 0.38, 0.16, 0.7), 32)
	_mk_btn("brake", "FREN\n▼", "brake", Vector2i(150, 140), Color(0.45, 0.08, 0.08, 0.7), 28)
	_mk_btn("hand", "EL\nFRENİ", "hand", Vector2i(110, 90), dark, 20)
	_mk_btn("siren", "SİREN", "", Vector2i(130, 70), Color(0.35, 0.05, 0.1, 0.7), 22).pressed.connect(func(): siren = not siren)
	_mk_btn("enter", "İN", "", Vector2i(130, 70), dark, 24).pressed.connect(_toggle_enter)
	_mk_btn("view", "1. ŞAHIS", "", Vector2i(130, 70), dark, 20).pressed.connect(_toggle_view)
	_mk_btn("night", "GECE", "", Vector2i(130, 70), dark, 22).pressed.connect(func(): _set_night(not night))
	joy_ctrl = Control.new()
	joy_ctrl.mouse_filter = Control.MOUSE_FILTER_IGNORE
	joy_ctrl.draw.connect(_draw_joy)
	ui_layer.add_child(joy_ctrl)
	hud = Label.new()
	hud.position = Vector2(20, 14)
	hud.add_theme_font_size_override("font_size", 26)
	hud.add_theme_color_override("font_outline_color", Color(0, 0, 0))
	hud.add_theme_constant_override("outline_size", 8)
	ui_layer.add_child(hud)
	get_viewport().size_changed.connect(_layout)
	_layout()

func _layout() -> void:
	var vs := get_viewport().get_visible_rect().size
	var m := 24.0
	btns["left"].position = Vector2(m, vs.y - 150 - m)
	btns["right"].position = Vector2(m + 170, vs.y - 150 - m)
	btns["gas"].position = Vector2(vs.x - 150 - m, vs.y - 200 - m)
	btns["brake"].position = Vector2(vs.x - 320 - m, vs.y - 140 - m)
	btns["hand"].position = Vector2(vs.x - 300 - m, vs.y - 260 - m)
	var x := vs.x - m
	for id in ["night", "view", "enter", "siren"]:
		x -= 140
		btns[id].position = Vector2(x, m)
	joy_ctrl.size = vs

func _btn_hit(p: Vector2) -> bool:
	for b in btns.values():
		if not b.visible:
			continue
		var r := Rect2(b.position, b.get_meta("size"))
		if r.has_point(p):
			return true
	return false

func _draw_joy() -> void:
	if mode != "foot":
		return
	var vs := get_viewport().get_visible_rect().size
	var c := joy_origin if joy_id >= 0 else Vector2(150, vs.y - 150)
	joy_ctrl.draw_circle(c, 85, Color(1, 1, 1, 0.12))
	joy_ctrl.draw_arc(c, 85, 0, TAU, 48, Color(1, 1, 1, 0.4), 3)
	joy_ctrl.draw_circle(c + joy_vec * 60, 36, Color(0.5, 0.7, 1, 0.55))

func _input(e: InputEvent) -> void:
	if e is InputEventScreenTouch:
		var t := e as InputEventScreenTouch
		var vs := get_viewport().get_visible_rect().size
		if t.pressed:
			if _btn_hit(t.position):
				return
			if mode == "foot" and t.position.x < vs.x * 0.4 and joy_id < 0:
				joy_id = t.index
				joy_origin = t.position
				joy_vec = Vector2.ZERO
			elif look_id < 0:
				look_id = t.index
				look_last = t.position
		else:
			if t.index == joy_id:
				joy_id = -1
				joy_vec = Vector2.ZERO
			if t.index == look_id:
				look_id = -1
	elif e is InputEventScreenDrag:
		var d := e as InputEventScreenDrag
		if d.index == joy_id:
			joy_vec = ((d.position - joy_origin) / 70.0).limit_length(1.0)
		elif d.index == look_id:
			var dl := d.position - look_last
			look_last = d.position
			cam_yaw -= dl.x * 0.006
			cam_pitch = clampf(cam_pitch - dl.y * 0.004, -1.2, 0.6)
			cam_free = 2.5

func _set_mode(m: String) -> void:
	mode = m
	var car_ui := m == "car"
	for id in ["left", "right", "gas", "brake", "hand", "siren"]:
		btns[id].visible = car_ui
	(btns["enter"].get_child(0) as Label).text = "İN" if car_ui else "BİN"
	player.visible = not car_ui
	player.process_mode = Node.PROCESS_MODE_DISABLED if car_ui else Node.PROCESS_MODE_INHERIT
	(player.get_child(0) as CollisionShape3D).disabled = car_ui
	joy_ctrl.queue_redraw()

func _toggle_enter() -> void:
	if mode == "car":
		if car.linear_velocity.length() > 3.0:
			return
		var side := car.global_transform.basis.x
		player.global_position = car.global_position + side * 1.6 + Vector3(0, 0.3, 0)
		player.rotation.y = car.rotation.y
		cam_yaw = car.rotation.y + PI
		car.engine_force = 0
		car.brake = 20
		_set_mode("foot")
	else:
		if player.global_position.distance_to(car.global_position) < 4.5:
			_set_mode("car")

func _toggle_view() -> void:
	view = "fps" if view == "tps" else "tps"
	(btns["view"].get_child(0) as Label).text = "3. ŞAHIS" if view == "fps" else "1. ŞAHIS"

func _set_night(on: bool) -> void:
	night = on
	env.sky = sky_night if on else sky_day
	sun.light_energy = 0.06 if on else 2.0
	sun.light_color = Color(0.55, 0.65, 1.0) if on else Color(1.0, 0.95, 0.86)
	env.fog_light_color = Color(0.05, 0.06, 0.1) if on else Color(0.74, 0.79, 0.86)
	env.tonemap_exposure = 1.25 if on else 1.0
	glass_lit.emission_energy_multiplier = 2.4 if on else 0.0
	lamp_head.emission_energy_multiplier = 6.0 if on else 0.2
	for l in lamp_lights:
		l.visible = on
	head_l.light_energy = 6.0 if on else 0.0
	head_r.light_energy = 6.0 if on else 0.0
	(btns["night"].get_child(0) as Label).text = "GÜNDÜZ" if on else "GECE"

# ---------------------------------------------------------------- oyun döngüsü
func _physics_process(delta: float) -> void:
	if mode == "car":
		var s := Input.get_action_strength("steer_left") - Input.get_action_strength("steer_right")
		var spd := car.linear_velocity.length()
		var max_steer := lerpf(0.55, 0.18, clampf(spd / 30.0, 0.0, 1.0))
		steer = move_toward(steer, s * max_steer, delta * 2.2)
		car.steering = steer
		var fwd := car.linear_velocity.dot(car.global_transform.basis.z)
		var g := Input.get_action_strength("gas")
		var b := Input.get_action_strength("brake")
		if b > 0.0 and fwd > 1.0:
			car.engine_force = 0.0
			car.brake = 35.0 * b
		elif b > 0.0:
			car.brake = 0.0
			car.engine_force = -2200.0 * b
		else:
			car.brake = 0.0 if g > 0.0 else 3.0
			car.engine_force = 4800.0 * g * clampf(1.0 - fwd / 48.0, 0.0, 1.0)
		if Input.is_action_pressed("hand"):
			car.brake = 70.0
	else:
		car.engine_force = 0.0
		car.brake = 15.0
		var v := joy_vec
		var kv := Vector2(Input.get_action_strength("steer_right") - Input.get_action_strength("steer_left"), Input.get_action_strength("brake") - Input.get_action_strength("gas"))
		if kv.length() > 0.1:
			v = kv.limit_length(1.0)
		var f := Vector3(sin(cam_yaw + PI), 0, cos(cam_yaw + PI))
		var r := Vector3(-f.z, 0, f.x)
		var dir := (f * -v.y + r * v.x)
		var vel := player.velocity
		vel.x = dir.x * 5.2
		vel.z = dir.z * 5.2
		vel.y = 0.0 if player.is_on_floor() else vel.y - 20.0 * delta
		player.velocity = vel
		player.move_and_slide()
		if dir.length() > 0.1:
			pvis.rotation.y = lerp_angle(pvis.rotation.y, atan2(dir.x, dir.z), 0.25)
			if panim and panim.current_animation != "run" and panim.has_animation("run"):
				panim.play("run", 0.2)
		elif panim and panim.current_animation != "idle" and panim.has_animation("idle"):
			panim.play("idle", 0.25)

func _process(delta: float) -> void:
	t_acc += delta
	var fl := int(t_acc * 7.0) % 2 == 0
	lb_mat_r.emission_energy_multiplier = (6.0 if fl else 0.3) if siren else 0.4
	lb_mat_b.emission_energy_multiplier = (0.3 if fl else 6.0) if siren else 0.4
	lb_r.light_energy = (4.0 if fl else 0.0) if siren else 0.0
	lb_b.light_energy = (0.0 if fl else 4.0) if siren else 0.0
	cam_free = maxf(0.0, cam_free - delta)
	if mode == "foot":
		joy_ctrl.queue_redraw()
	if not cam_override:
		_update_camera(delta)
	var kmh := car.linear_velocity.length() * 3.6
	hud.text = ("ARAÇ  %d km/h" % kmh if mode == "car" else "YAYA") + "   •   " + ("1. şahıs" if view == "fps" else "3. şahıs") + ("   •   SİREN" if siren else "")

func _update_camera(delta: float) -> void:
	if mode == "car":
		var xf := car.global_transform
		if view == "fps":
			cam.global_position = xf * driver_seat
			var look := xf * (driver_seat + Vector3(0, -0.05, 10))
			cam.look_at(look, Vector3.UP)
			cam.fov = 72.0
		else:
			var car_yaw := atan2(xf.basis.z.x, xf.basis.z.z)
			if cam_free <= 0.0:
				cam_yaw = lerp_angle(cam_yaw, car_yaw + PI, 1.0 - exp(-4.0 * delta))
				cam_pitch = lerpf(cam_pitch, -0.18, 1.0 - exp(-3.0 * delta))
			var dist := 7.2 + car.linear_velocity.length() * 0.05
			var off := Vector3(sin(cam_yaw), 0, cos(cam_yaw)) * dist * cos(cam_pitch) + Vector3(0, 2.0 - sin(cam_pitch) * dist, 0)
			var target := car.global_position + Vector3(0, 1.1, 0)
			cam.global_position = cam.global_position.lerp(target + off, 1.0 - exp(-10.0 * delta))
			cam.look_at(target, Vector3.UP)
			cam.fov = 66.0 + minf(12.0, car.linear_velocity.length() * 0.35)
	else:
		var head := player.global_position + Vector3(0, 1.62, 0)
		if view == "fps":
			pvis.visible = false
			cam.global_position = head + Vector3(sin(cam_yaw + PI), 0, cos(cam_yaw + PI)) * 0.15
			cam.rotation = Vector3(cam_pitch, cam_yaw, 0)
			cam.fov = 75.0
		else:
			pvis.visible = true
			var dist := 3.4
			var back := Vector3(sin(cam_yaw), 0, cos(cam_yaw))
			var right := Vector3(back.z, 0, -back.x)
			var p := head + back * dist * cos(cam_pitch) + Vector3(0, -sin(cam_pitch) * dist + 0.2, 0) - right * 0.55
			cam.global_position = cam.global_position.lerp(p, 1.0 - exp(-14.0 * delta))
			cam.look_at(head - right * 0.55 - back * 2.0 + Vector3(0, -0.25, 0), Vector3.UP)
			cam.fov = 68.0

# ---------------------------------------------------------------- otomatik ekran görüntüleri (CI)
func _secs(s: float) -> void:
	await get_tree().create_timer(s).timeout

func _shot(dir: String, n: String) -> void:
	await RenderingServer.frame_post_draw
	await RenderingServer.frame_post_draw
	var img := get_viewport().get_texture().get_image()
	img.save_png(dir + "/" + n + ".png")
	print("SHOT ", n, " draw_calls=", Performance.get_monitor(Performance.RENDER_TOTAL_DRAW_CALLS_IN_FRAME), " objects=", Performance.get_monitor(Performance.RENDER_TOTAL_OBJECTS_IN_FRAME), " fps=", Engine.get_frames_per_second())

func _place_car(p: Vector3, yaw: float) -> void:
	car.global_transform = Transform3D(Basis(Vector3.UP, yaw), p)
	car.linear_velocity = Vector3.ZERO
	car.angular_velocity = Vector3.ZERO

func _run_shots() -> void:
	var dir := ProjectSettings.globalize_path("res://shots")
	DirAccess.make_dir_recursive_absolute(dir)
	await _secs(2.0)
	var p0 := car.global_position
	Input.action_press("gas")
	await _secs(2.5)
	Input.action_release("gas")
	print("DRIVE_TEST move=", car.global_position - p0, " forward=", car.global_transform.basis.z, " speed=", car.linear_velocity.length())
	var y0 := car.rotation.y
	Input.action_press("steer_left")
	Input.action_press("gas")
	await _secs(1.5)
	Input.action_release("steer_left")
	Input.action_release("gas")
	print("STEER_LEFT_TEST dyaw=", car.rotation.y - y0)
	_place_car(Vector3(BLK * 0.5 - 4, 0.5, ROAD * 0.25 + 0.2), PI / 2)
	await _secs(1.5)
	cam_yaw = PI / 2 + PI
	await _secs(1.0)
	await _shot(dir, "01_arac_3sahis")
	view = "fps"
	await _secs(0.6)
	await _shot(dir, "02_arac_1sahis")
	view = "tps"
	_toggle_enter()
	player.global_position = car.global_position + Vector3(-1.5, 0.2, 3.2)
	cam_yaw = PI * 0.15
	await _secs(1.5)
	await _shot(dir, "03_yaya_3sahis")
	view = "fps"
	cam_yaw = PI * 0.75
	cam_pitch = -0.05
	await _secs(0.6)
	await _shot(dir, "04_yaya_1sahis")
	view = "tps"
	_set_mode("car")
	cam_override = true
	cam.global_position = Vector3(BLK * 0.5 + 60, 55, 70)
	cam.look_at(Vector3(BLK * 0.5 - 10, 0, -10), Vector3.UP)
	cam.fov = 60
	await _secs(1.0)
	await _shot(dir, "05_sehir_havadan")
	cam_override = false
	_set_night(true)
	siren = true
	cam_yaw = PI / 2 + PI * 0.75
	cam_free = 5.0
	await _secs(1.5)
	await _shot(dir, "06_gece_siren")
	print("SHOTS_DONE")
	get_tree().quit()
