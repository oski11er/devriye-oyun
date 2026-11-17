extends Node3D
## Devriye: Şehrin Nabzı — native 3D (Godot 4.3)
## Ana düğüm: ortam, oyuncu aracı ve memur, kameralar, gün döngüsü, hava, ses, iç mekân.

var city
var traffic
var game
var ui
var env: Environment
var sun: DirectionalLight3D
var sky_day: Sky
var sky_night: Sky
var car: VehicleBody3D
var car_type := "sedan"
var car_vis: Node3D
var car_scene: PackedScene
var human_scene: PackedScene
var car_mesh_cache := {}
var lb_on_r: StandardMaterial3D
var lb_on_b: StandardMaterial3D
var lb_off_r: StandardMaterial3D
var lb_off_b: StandardMaterial3D
var my_lb: Array = []
var lb_r: OmniLight3D
var lb_b: OmniLight3D
var head_l: SpotLight3D
var head_r: SpotLight3D
var player: CharacterBody3D
var pvis: Node3D
var panim: AnimationPlayer
var pbody_mat := StandardMaterial3D.new()
var cam: Camera3D
var mode := "car"          # car / foot / interior
var view := "tps"          # tps / fps / top
var siren := false
var lights_on := false
var weather := "clear"
var steer := 0.0
var cam_yaw := 0.0
var cam_pitch := -0.15
var cam_free := 0.0
var shake := 0.0
var joy_vec := Vector2.ZERO
var lamp_pool: Array = []
var lamp_t := 0.0
var rain: GPUParticles3D
var t_acc := 0.0
var prev_vel := Vector3.ZERO
var fuel := 100.0
var tire := 100.0
var dmg := 0.0
var interior: Node3D
var title_ang := 0.0
var shots := false
var snd_siren: AudioStreamPlayer
var snd_horn: AudioStreamPlayer
var snd_beep: AudioStreamPlayer
var horn_down := false
var driver_seat := Vector3(0.36, 1.12, -0.15)
var quality := "medium"

func quality_name() -> String:
	return {"high": "Yüksek", "medium": "Orta", "low": "Düşük"}[quality]

func cycle_quality() -> void:
	quality = {"high": "medium", "medium": "low", "low": "high"}[quality]
	apply_quality()
	var f := FileAccess.open("user://quality.txt", FileAccess.WRITE)
	if f:
		f.store_string(quality)

func apply_quality() -> void:
	var vp := get_viewport()
	vp.scaling_3d_scale = {"high": 1.0, "medium": 0.85, "low": 0.7}[quality]
	vp.msaa_3d = Viewport.MSAA_2X if quality == "high" else Viewport.MSAA_DISABLED
	sun.directional_shadow_max_distance = {"high": 140.0, "medium": 100.0, "low": 60.0}[quality]
	env.glow_enabled = quality != "low"
	for i in lamp_pool.size():
		(lamp_pool[i] as OmniLight3D).set_meta("allowed", i < {"high": 8, "medium": 6, "low": 3}[quality])

# ---------------------------------------------------------------- yardımcılar
func tex(n: String) -> Texture2D:
	var p := "res://assets/tex/" + n
	if ResourceLoader.exists(p):
		return load(p)
	return null

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

func aabb_of(n: Node, anc: Node3D, filter: String = "") -> AABB:
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

func player_pos() -> Vector2:
	if mode == "car":
		return Vector2(car.global_position.x, car.global_position.z)
	return Vector2(player.global_position.x, player.global_position.z)

func car_pos() -> Vector2:
	return Vector2(car.global_position.x, car.global_position.z)

func car_speed() -> float:
	return car.linear_velocity.length()

func car_fwd() -> Vector2:
	var z := car.global_transform.basis.z
	return Vector2(z.x, z.z).normalized()

func player_yaw() -> float:
	if mode == "car":
		var z := car.global_transform.basis.z
		return atan2(z.x, z.z)
	return pvis.rotation.y

# ---------------------------------------------------------------- kurulum
func _ready() -> void:
	shots = "--shots" in OS.get_cmdline_user_args()
	if shots:
		DisplayServer.window_set_size(Vector2i(1280, 720))
	randomize()
	_setup_input()
	_setup_env()
	_setup_materials()
	_setup_audio()
	city = preload("res://scripts/city.gd").new()
	add_child(city)
	city.setup(self)
	traffic = preload("res://scripts/traffic.gd").new()
	add_child(traffic)
	traffic.setup(self)
	game = preload("res://scripts/game.gd").new()
	add_child(game)
	_spawn_player(Vector3(20, 0.5, 60))
	spawn_car("sedan", Vector3(20, 0.6, 67.5), PI / 2)
	cam = Camera3D.new()
	cam.fov = 68.0
	cam.far = 900.0
	add_child(cam)
	cam.current = true
	_setup_rain()
	for i in 8:
		var l := OmniLight3D.new()
		l.light_color = Color(1.0, 0.82, 0.55)
		l.light_energy = 3.0
		l.omni_range = 17.0
		l.omni_attenuation = 1.2
		l.visible = false
		add_child(l)
		lamp_pool.append(l)
	ui = preload("res://scripts/ui.gd").new()
	add_child(ui)
	ui.setup(self)
	game.setup(self)
	if FileAccess.file_exists("user://quality.txt"):
		var qf := FileAccess.open("user://quality.txt", FileAccess.READ)
		var q := qf.get_as_text().strip_edges()
		if q in ["high", "medium", "low"]:
			quality = q
	elif OS.get_name() != "Android":
		quality = "high"
	apply_quality()
	set_mode("car")
	city.update_stream(player_pos(), 0.0, true)
	if shots:
		var t = preload("res://scripts/autotest.gd").new()
		add_child(t)
		t.run(self)
	else:
		game.title_screen()

func _setup_input() -> void:
	var keys := {"steer_left": [KEY_LEFT, KEY_A], "steer_right": [KEY_RIGHT, KEY_D], "gas": [KEY_UP, KEY_W], "brake": [KEY_DOWN, KEY_S], "hand": [KEY_SPACE], "horn": [KEY_H]}
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
	sun.directional_shadow_max_distance = 110.0
	sun.directional_shadow_mode = DirectionalLight3D.SHADOW_PARALLEL_2_SPLITS
	sun.shadow_blur = 1.4
	add_child(sun)

func _setup_materials() -> void:
	lb_on_r = plain(Color(0.6, 0, 0), 0.3)
	lb_on_r.emission_enabled = true
	lb_on_r.emission = Color(1, 0.05, 0.05)
	lb_on_r.emission_energy_multiplier = 6.0
	lb_on_b = plain(Color(0, 0, 0.6), 0.3)
	lb_on_b.emission_enabled = true
	lb_on_b.emission = Color(0.1, 0.25, 1)
	lb_on_b.emission_energy_multiplier = 6.0
	lb_off_r = plain(Color(0.35, 0.03, 0.03), 0.3)
	lb_off_b = plain(Color(0.03, 0.05, 0.35), 0.3)

func _setup_rain() -> void:
	rain = GPUParticles3D.new()
	rain.amount = 1400
	rain.lifetime = 1.2
	rain.visibility_aabb = AABB(Vector3(-30, -20, -30), Vector3(60, 40, 60))
	var pm := ParticleProcessMaterial.new()
	pm.emission_shape = ParticleProcessMaterial.EMISSION_SHAPE_BOX
	pm.emission_box_extents = Vector3(25, 1, 25)
	pm.direction = Vector3(0.1, -1, 0)
	pm.spread = 3.0
	pm.initial_velocity_min = 22.0
	pm.initial_velocity_max = 28.0
	pm.gravity = Vector3(0, -9.8, 0)
	rain.process_material = pm
	var dm := BoxMesh.new()
	dm.size = Vector3(0.015, 0.55, 0.015)
	var mm := StandardMaterial3D.new()
	mm.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	mm.albedo_color = Color(0.75, 0.82, 0.95, 0.35)
	mm.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	dm.material = mm
	rain.draw_pass_1 = dm
	rain.emitting = false
	add_child(rain)

# ---------------------------------------------------------------- ses (prosedürel)
func _wav(gen: Callable, secs: float, loop: bool) -> AudioStreamWAV:
	var rate := 22050
	var n := int(rate * secs)
	var data := PackedByteArray()
	data.resize(n * 2)
	var ph := 0.0
	for i in n:
		var t := float(i) / rate
		var f: float = gen.call(t)
		ph += TAU * f / rate
		var s := clampf(sin(ph) * 1.6, -1.0, 1.0) * 0.5
		data.encode_s16(i * 2, int(s * 32767.0))
	var w := AudioStreamWAV.new()
	w.format = AudioStreamWAV.FORMAT_16_BITS
	w.mix_rate = rate
	w.data = data
	if loop:
		w.loop_mode = AudioStreamWAV.LOOP_FORWARD
		w.loop_end = n
	return w

func _setup_audio() -> void:
	snd_siren = AudioStreamPlayer.new()
	snd_siren.stream = _wav(func(t): return 750.0 + 550.0 * (0.5 - 0.5 * cos(t / 2.4 * TAU)), 2.4, true)
	snd_siren.volume_db = -14.0
	add_child(snd_siren)
	snd_horn = AudioStreamPlayer.new()
	snd_horn.stream = _wav(func(t): return 330.0 + 85.0 * float(int(t * 660.0) % 2), 0.5, true)
	snd_horn.volume_db = -12.0
	add_child(snd_horn)
	snd_beep = AudioStreamPlayer.new()
	snd_beep.stream = _wav(func(t): return 1200.0 if t < 0.08 else 1600.0, 0.2, false)
	snd_beep.volume_db = -16.0
	add_child(snd_beep)

func beep() -> void:
	snd_beep.play()

# ---------------------------------------------------------------- oyuncu aracı
func _fit(inst: Node3D, length: float, front_filter: String) -> Node3D:
	var root := Node3D.new()
	var pivot := Node3D.new()
	root.add_child(pivot)
	pivot.add_child(inst)
	var a := aabb_of(inst, root)
	if a.size.x > a.size.z:
		pivot.rotation.y = PI / 2
		a = aabb_of(inst, root)
	pivot.scale = Vector3.ONE * (length / maxf(a.size.z, 0.001))
	a = aabb_of(inst, root)
	if front_filter != "":
		var hl := aabb_of(inst, root, front_filter)
		if hl.size != Vector3.ZERO and hl.get_center().z < a.get_center().z:
			pivot.rotation.y += PI
			a = aabb_of(inst, root)
	pivot.position = -Vector3(a.get_center().x, a.position.y, a.get_center().z)
	root.set_meta("aabb", AABB(Vector3(-a.size.x * 0.5, 0, -a.size.z * 0.5), a.size))
	return root

func _merge(root: Node3D) -> ArrayMesh:
	var by_mat := {}
	var order: Array = []
	for mi in root.find_children("*", "MeshInstance3D", true, false):
		var m := mi as MeshInstance3D
		if m.mesh == null:
			continue
		var xf := _xform_to(m, root)
		for i in m.mesh.get_surface_count():
			var mat: Material = m.get_active_material(i)
			if not by_mat.has(mat):
				var st := SurfaceTool.new()
				st.begin(Mesh.PRIMITIVE_TRIANGLES)
				by_mat[mat] = st
				order.append(mat)
			(by_mat[mat] as SurfaceTool).append_from(m.mesh, i, xf)
	var am := ArrayMesh.new()
	for mat in order:
		(by_mat[mat] as SurfaceTool).commit(am)
		am.surface_set_material(am.get_surface_count() - 1, mat)
	return am

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

func car_visual(c1: Color, c2: Color, scl: float = 1.0) -> Node3D:
	if car_scene == null:
		car_scene = load("res://assets/models/car.glb")
	var key := c1.to_html() + c2.to_html()
	if not car_mesh_cache.has(key):
		var inst: Node3D = car_scene.instantiate()
		_paint_car(inst, c1, c2)
		var tmp := _fit(inst, 4.45, "Headlight")
		car_mesh_cache[key] = [_merge(tmp), tmp.get_meta("aabb")]
		tmp.free()
	var root := Node3D.new()
	var body := MeshInstance3D.new()
	body.mesh = car_mesh_cache[key][0]
	body.scale = Vector3.ONE * scl
	root.add_child(body)
	var a: AABB = car_mesh_cache[key][1]
	root.set_meta("aabb", AABB(a.position * scl, a.size * scl))
	return root

func _moto_visual(c1: Color) -> Node3D:
	var root := Node3D.new()
	var parts := [[Vector3(0, 0.62, 0), Vector3(0.22, 0.3, 1.6), Color(0.1, 0.1, 0.1), 0.5, 0.4], [Vector3(0, 0.88, 0.25), Vector3(0.34, 0.26, 0.7), c1, 0.3, 0.5], [Vector3(0, 0.95, -0.35), Vector3(0.3, 0.1, 0.6), Color(0.08, 0.08, 0.08), 0.8, 0.0], [Vector3(0, 1.15, 0.72), Vector3(0.5, 0.35, 0.06), Color(0.2, 0.25, 0.3), 0.1, 0.3], [Vector3(-0.32, 0.8, -0.45), Vector3(0.22, 0.35, 0.5), Color(0.95, 0.95, 0.96), 0.3, 0.4], [Vector3(0.32, 0.8, -0.45), Vector3(0.22, 0.35, 0.5), Color(0.95, 0.95, 0.96), 0.3, 0.4]]
	for pa in parts:
		var mi := MeshInstance3D.new()
		var bm := BoxMesh.new()
		bm.size = pa[1]
		mi.mesh = bm
		mi.material_override = plain(pa[2], pa[3], pa[4])
		mi.position = pa[0]
		root.add_child(mi)
	for z in [-0.72, 0.72]:
		var w := MeshInstance3D.new()
		var cy := CylinderMesh.new()
		cy.top_radius = 0.33
		cy.bottom_radius = 0.33
		cy.height = 0.14
		w.mesh = cy
		w.rotation.z = PI / 2
		w.position = Vector3(0, 0.33, z)
		w.material_override = plain(Color(0.05, 0.05, 0.05), 0.85)
		root.add_child(w)
	var rider: Node3D = _human_model(Color(0.1, 0.17, 0.36), false)
	rider.position = Vector3(0, 0.45, -0.2)
	rider.scale = Vector3.ONE * 0.95
	root.add_child(rider)
	root.set_meta("aabb", AABB(Vector3(-0.4, 0, -1.0), Vector3(0.8, 1.9, 2.0)))
	return root

func spawn_car(t: String, pos: Vector3, yaw: float) -> void:
	if car != null and is_instance_valid(car):
		car.queue_free()
	car_type = t
	var sp: Dictionary = C.VEH[t]
	car = VehicleBody3D.new()
	car.mass = sp["mass"]
	car.center_of_mass_mode = RigidBody3D.CENTER_OF_MASS_MODE_CUSTOM
	car.center_of_mass = Vector3(0, 0.25, 0)
	car.contact_monitor = true
	car.max_contacts_reported = 4
	car.position = pos
	car.rotation.y = yaw
	add_child(car)
	var moto: bool = sp.get("moto", false)
	var cs := CollisionShape3D.new()
	var bs := BoxShape3D.new()
	bs.size = Vector3(0.8, 1.0, 2.1) if moto else Vector3(1.85, 0.75, 4.3)
	cs.shape = bs
	cs.position = Vector3(0, 0.75, 0)
	car.add_child(cs)
	var c1 := Color(sp["c1"])
	var c2 := Color(sp["c2"])
	var liv := "std"
	if game and game.G.size() > 0:
		liv = game.G.get("livery", "std")
	if liv == "retro" and not sp.get("civil", false):
		c2 = Color(0.75, 0.1, 0.1)
	elif liv == "matte" and not sp.get("civil", false):
		c1 = Color(0.12, 0.13, 0.15)
	var scl := 1.12 if t == "special" else (1.05 if t == "suv" else 1.0)
	car_vis = _moto_visual(c1) if moto else car_visual(c1, c2, scl)
	car.add_child(car_vis)
	var a: AABB = car_vis.get_meta("aabb")
	var wx := 0.32 if moto else 0.8
	var wz := 0.72 if moto else 1.36
	for wp in [Vector3(wx, 0.4, wz), Vector3(-wx, 0.4, wz), Vector3(wx, 0.4, -wz), Vector3(-wx, 0.4, -wz)]:
		var w := VehicleWheel3D.new()
		w.position = wp
		w.wheel_radius = 0.34
		w.wheel_rest_length = 0.12
		w.suspension_travel = 0.2
		w.suspension_stiffness = 60.0 if t == "special" else 45.0
		w.suspension_max_force = 30000.0 if t == "special" else 14000.0
		w.damping_compression = 0.9
		w.damping_relaxation = 1.3
		w.wheel_friction_slip = 2.6
		w.wheel_roll_influence = 0.02 if moto else 0.12
		w.use_as_steering = wp.z > 0
		w.use_as_traction = wp.z < 0
		car.add_child(w)
	my_lb.clear()
	if sp["lb"] and not moto:
		var top := a.size.y
		var bar := MeshInstance3D.new()
		var bm := BoxMesh.new()
		bm.size = Vector3(1.15, 0.1, 0.32)
		bar.mesh = bm
		bar.material_override = plain(Color(0.05, 0.05, 0.06), 0.4, 0.5)
		bar.position = Vector3(0, top + 0.02, -0.25)
		car_vis.add_child(bar)
		for side in [-1.0, 1.0]:
			var lm := MeshInstance3D.new()
			var lb := BoxMesh.new()
			lb.size = Vector3(0.5, 0.13, 0.28)
			lm.mesh = lb
			lm.position = Vector3(side * 0.29, top + 0.1, -0.25)
			car_vis.add_child(lm)
			my_lb.append(lm)
		for side in [-1.0, 1.0]:
			var tl := Label3D.new()
			tl.text = "POLİS"
			tl.font_size = 96
			tl.pixel_size = 0.0045
			tl.modulate = Color(0.05, 0.15, 0.45) if c1.get_luminance() > 0.5 else Color(0.95, 0.95, 1)
			tl.outline_size = 0
			tl.position = Vector3(side * (a.size.x * 0.5 + 0.01), 0.72, -0.1)
			tl.rotation.y = side * PI / 2
			car_vis.add_child(tl)
	else:
		for side in [-1.0, 1.0]:
			var lm := MeshInstance3D.new()
			var lb := BoxMesh.new()
			lb.size = Vector3(0.12, 0.12, 0.12) if moto else Vector3(0.25, 0.06, 0.04)
			lm.mesh = lb
			lm.position = Vector3(side * 0.32, 1.1, -0.6) if moto else Vector3(side * 0.2, 0.45, a.size.z * 0.5 + 0.02)
			car_vis.add_child(lm)
			my_lb.append(lm)
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
		s.position = Vector3(side * (0.0 if moto else 0.62), 0.72, a.size.z * 0.5)
		s.rotation = Vector3(-0.06, PI, 0)
		car.add_child(s)
		if side < 0:
			head_l = s
		else:
			head_r = s
	fuel = 100.0
	tire = 100.0
	dmg = 0.0
	prev_vel = Vector3.ZERO

# ---------------------------------------------------------------- memur
func _human_model(tint: Color, cop: bool) -> Node3D:
	if human_scene == null:
		human_scene = load("res://assets/models/human.glb")
	var inst: Node3D = human_scene.instantiate()
	var holder := Node3D.new()
	holder.add_child(inst)
	var a := aabb_of(inst, holder)
	var s := 1.8 / maxf(a.size.y, 0.001)
	inst.scale = Vector3.ONE * s
	a = aabb_of(inst, holder)
	inst.position.y -= a.position.y
	var body := pbody_mat if cop else StandardMaterial3D.new()
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
	pvis = _human_model(Color(0.1, 0.17, 0.36), true)
	player.add_child(pvis)
	panim = pvis.get_meta("anim")

func set_outfit(col: Color) -> void:
	pbody_mat.albedo_color = col

func set_mode(m: String) -> void:
	mode = m
	var in_car := m == "car"
	player.visible = not in_car
	(player.get_child(0) as CollisionShape3D).disabled = in_car
	if ui:
		ui.on_mode()

func toggle_enter() -> void:
	if mode == "interior":
		return
	if mode == "car":
		if car_speed() > 3.0:
			ui.toast("Önce aracı durdur.")
			return
		var side := car.global_transform.basis.x
		var q := car.global_position + side * 1.6
		var r: Array = city.collide_circle(Vector2(q.x, q.z), 0.5)
		player.global_position = Vector3(r[0].x, 0.4, r[0].y)
		cam_yaw = player_yaw() + PI
		car.engine_force = 0
		car.brake = 20
		set_mode("foot")
	else:
		if player.global_position.distance_to(car.global_position) < 4.5:
			set_mode("car")
		else:
			ui.toast("Araca yaklaş.")

func cycle_view() -> void:
	view = {"tps": "fps", "fps": "top", "top": "tps"}[view]

func set_siren(on: bool) -> void:
	siren = on
	if on:
		if not snd_siren.playing:
			snd_siren.play()
	else:
		snd_siren.stop()

# ---------------------------------------------------------------- iç mekân
func enter_interior(spots: Array) -> void:
	if interior:
		interior.queue_free()
	interior = Node3D.new()
	interior.position = Vector3(0, -200, 0)
	add_child(interior)
	var w := 16.0
	var d := 10.5
	var floor_m := MeshInstance3D.new()
	var pm := PlaneMesh.new()
	pm.size = Vector2(w, d)
	floor_m.mesh = pm
	floor_m.material_override = plain(Color(0.5, 0.36, 0.24), 0.55)
	floor_m.position = Vector3(w / 2, 0, d / 2)
	interior.add_child(floor_m)
	var fl := StaticBody3D.new()
	var fcs := CollisionShape3D.new()
	var fbs := BoxShape3D.new()
	fbs.size = Vector3(w, 0.2, d)
	fcs.shape = fbs
	fl.position = Vector3(w / 2, -0.1, d / 2)
	fl.add_child(fcs)
	interior.add_child(fl)
	var wm: Material = city.pbr("plaster", 0.5, Color(0.95, 0.92, 0.85), Color(0.8, 0.77, 0.7))
	var walls := [[Vector3(w / 2, 1.4, -0.1), Vector3(w + 0.4, 2.8, 0.2)], [Vector3(-0.1, 1.4, d / 2), Vector3(0.2, 2.8, d)], [Vector3(w + 0.1, 1.4, d / 2), Vector3(0.2, 2.8, d)], [Vector3(w / 4 - 0.45, 0.3, d + 0.1), Vector3(w / 2 - 0.9, 0.6, 0.2)], [Vector3(w * 0.75 + 0.45, 0.3, d + 0.1), Vector3(w / 2 - 0.9, 0.6, 0.2)]]
	for wl in walls:
		var mi := MeshInstance3D.new()
		var bm := BoxMesh.new()
		bm.size = wl[1]
		mi.mesh = bm
		mi.material_override = wm
		mi.position = wl[0]
		interior.add_child(mi)
		var sb := StaticBody3D.new()
		var cs := CollisionShape3D.new()
		var bs := BoxShape3D.new()
		bs.size = wl[1]
		cs.shape = bs
		sb.position = wl[0]
		sb.add_child(cs)
		interior.add_child(sb)
	var furn := [[Vector3(2.6, 0.3, 1.8), Vector3(3.2, 0.6, 1.8), Color(0.35, 0.3, 0.42)], [Vector3(13.2, 0.42, 7.6), Vector3(3.2, 0.85, 2.2), Color(0.42, 0.28, 0.2)], [Vector3(8, 0.38, 4.6), Vector3(3, 0.75, 1.5), Color(0.24, 0.36, 0.3)], [Vector3(1.6, 0.95, 8.6), Vector3(2, 1.9, 1.4), Color(0.3, 0.3, 0.34)]]
	for f in furn:
		var mi := MeshInstance3D.new()
		var bm := BoxMesh.new()
		bm.size = f[1]
		mi.mesh = bm
		mi.material_override = plain(f[2], 0.75)
		mi.position = f[0]
		interior.add_child(mi)
	var lamp := OmniLight3D.new()
	lamp.light_color = Color(1, 0.86, 0.65)
	lamp.light_energy = 2.2
	lamp.omni_range = 14.0
	lamp.position = Vector3(w / 2, 2.6, d / 2)
	interior.add_child(lamp)
	var exitm := MeshInstance3D.new()
	var ep := PlaneMesh.new()
	ep.size = Vector2(1.6, 0.6)
	exitm.mesh = ep
	exitm.material_override = plain(Color(0.25, 0.7, 0.35), 0.5)
	exitm.position = Vector3(w / 2, 0.01, d - 0.3)
	interior.add_child(exitm)
	for sp in spots:
		var g := MeshInstance3D.new()
		var sm := SphereMesh.new()
		sm.radius = 0.14
		sm.height = 0.28
		g.mesh = sm
		var em := plain(Color(1, 0.8, 0.2), 0.3)
		em.emission_enabled = true
		em.emission = Color(1, 0.8, 0.2)
		em.emission_energy_multiplier = 2.5
		g.material_override = em
		g.position = Vector3(sp["x"], 1.1, sp["z"])
		interior.add_child(g)
		sp["node"] = g
	player.global_position = interior.global_position + Vector3(w / 2, 0.3, d - 1.0)
	player.velocity = Vector3.ZERO
	set_mode("interior")

func exit_interior(door_p: Vector2, out: float) -> void:
	set_mode("foot")
	player.global_position = Vector3(door_p.x, 0.4, door_p.y + out * 1.2)
	if interior:
		interior.queue_free()
		interior = null

func interior_local() -> Vector2:
	var l := player.global_position - interior.global_position
	return Vector2(l.x, l.z)

# ---------------------------------------------------------------- döngü
func _physics_process(delta: float) -> void:
	if game == null or not game.playing():
		car.engine_force = 0.0
		car.brake = 10.0
		return
	var G: Dictionary = game.G
	if mode == "car":
		var sp: Dictionary = C.VEH[car_type]
		var s := Input.get_action_strength("steer_left") - Input.get_action_strength("steer_right")
		var fat: float = G["fatigue"]
		if fat > 72.0:
			s += sin(t_acc * 1.5) * 0.18 * (fat - 72.0) / 28.0
		var spd := car_speed()
		var max_steer := lerpf(0.55, 0.18, clampf(spd / 30.0, 0.0, 1.0))
		var resp := 1.0 - maxf(0.0, fat - 50.0) / 110.0
		steer = move_toward(steer, s * max_steer, delta * 2.2 * resp)
		car.steering = steer
		var fwd := car.linear_velocity.dot(car.global_transform.basis.z)
		var g := Input.get_action_strength("gas")
		var b := Input.get_action_strength("brake")
		var vmax: float = float(sp["max"]) * (1.0 - dmg / 230.0) * (0.8 if tire < 20.0 else 1.0)
		if fuel <= 0.0:
			g = 0.0
		if b > 0.0 and fwd > 1.0:
			car.engine_force = 0.0
			car.brake = 35.0 * b
		elif b > 0.0:
			car.brake = 0.0
			car.engine_force = -2400.0 * b
		else:
			car.brake = 0.0 if g > 0.0 else 3.0
			car.engine_force = float(sp["eng"]) * g * clampf(1.0 - fwd / vmax, 0.0, 1.0)
		if Input.is_action_pressed("hand"):
			car.brake = 70.0
		fuel = maxf(0.0, fuel - (g * 0.11 + 0.006) * delta)
		var grip := 2.6 * (0.5 + 0.5 * tire / 100.0) * (0.7 if weather == "rain" else 1.0)
		var skid := 0.0
		for wch in car.get_children():
			if wch is VehicleWheel3D:
				var vw := wch as VehicleWheel3D
				vw.wheel_friction_slip = grip * (0.55 if Input.is_action_pressed("hand") and not vw.use_as_steering else 1.0)
				skid += 1.0 - vw.get_skidinfo()
		if spd > 4.0:
			tire = maxf(0.0, tire - skid * delta * 0.6)
		var dvl := (car.linear_velocity - prev_vel).length()
		if dvl / maxf(delta, 0.001) > 60.0 and car.get_contact_count() > 0:
			var dur: float = sp.get("dur", 1.0)
			dmg = minf(100.0, dmg + dvl * 2.2 * dur)
			shake = minf(1.0, dvl / 12.0)
			if dvl > 14.0:
				G["hp"] = maxf(5.0, float(G["hp"]) - dvl * 0.8)
			for bdy in car.get_colliding_bodies():
				if bdy.has_meta("ncar"):
					game.on_car_hit(bdy.get_meta("ncar"), dvl)
		prev_vel = car.linear_velocity
	else:
		car.engine_force = 0.0
		car.brake = 15.0
		var v := joy_vec
		var kv := Vector2(Input.get_action_strength("steer_right") - Input.get_action_strength("steer_left"), Input.get_action_strength("brake") - Input.get_action_strength("gas"))
		if kv.length() > 0.1:
			v = kv.limit_length(1.0)
		var f := Vector3(sin(cam_yaw + PI), 0, cos(cam_yaw + PI))
		var r := Vector3(-f.z, 0, f.x)
		if mode == "interior":
			f = Vector3(0, 0, -1)
			r = Vector3(1, 0, 0)
		var dir := (f * -v.y + r * v.x)
		var fat2: float = G["fatigue"]
		var spd2 := 5.2 * (1.0 - maxf(0.0, fat2 - 40.0) / 140.0) * (0.8 if float(G["hp"]) < 40.0 else 1.0) * (0.9 if float(G["hunger"]) > 85.0 else 1.0)
		if mode == "interior":
			spd2 = 3.2
		var vel := player.velocity
		vel.x = dir.x * spd2
		vel.z = dir.z * spd2
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
	for i in my_lb.size():
		var red := i == 0
		var lit := siren and (fl == red)
		(my_lb[i] as MeshInstance3D).material_override = (lb_on_r if red else lb_on_b) if lit else (lb_off_r if red else lb_off_b)
	lb_r.light_energy = (4.0 if fl else 0.0) if siren else 0.0
	lb_b.light_energy = (0.0 if fl else 4.0) if siren else 0.0
	var hp := (Input.is_action_pressed("horn") or horn_down) and not get_tree().paused
	if hp and not snd_horn.playing:
		snd_horn.play()
	elif not hp and snd_horn.playing:
		snd_horn.stop()
	cam_free = maxf(0.0, cam_free - delta)
	shake = maxf(0.0, shake - delta * 2.0)
	if game and game.G.size() > 0:
		_env_update(delta)
		var pp := player_pos()
		if mode != "interior":
			city.update_stream(pp, delta)
		rain.global_position = cam.global_position + Vector3(0, 14, 0) - cam.global_transform.basis.z * 10.0
		lamp_t -= delta
		if lamp_t <= 0:
			lamp_t = 0.5
			var night: bool = game.night_f() > 0.35
			var pts: Array = city.lamps_near(pp, lamp_pool.size()) if night else []
			for i in lamp_pool.size():
				var l: OmniLight3D = lamp_pool[i]
				l.visible = i < pts.size() and l.get_meta("allowed", true)
				if l.visible:
					l.global_position = (pts[i] as Vector3) + Vector3(0, -0.4, 0)
	else:
		city.update_stream(Vector2(20, 64), delta)
	_update_camera(delta)

func _env_update(_delta: float) -> void:
	var h: float = float(game.G["time"]) / 60.0
	var n: float = game.night_f()
	var d := 1.0 - n
	var ang := (h - 6.0) / 12.0 * PI
	sun.rotation = Vector3(-maxf(0.25, sin(clampf(ang, 0.0, PI))), -0.6 + h * 0.08, 0)
	var dusk := clampf(1.0 - absf(h - 19.0) / 1.6, 0.0, 1.0) + clampf(1.0 - absf(h - 6.5) / 1.4, 0.0, 1.0)
	var ov := 1.0 if (weather == "overcast" or weather == "rain") else 0.0
	sun.light_energy = d * (2.1 - ov * 1.3) + n * 0.07
	sun.light_color = Color(1.0, 0.95, 0.86).lerp(Color(1.0, 0.62, 0.35), clampf(dusk, 0, 1) * 0.7).lerp(Color(0.55, 0.65, 1.0), n)
	sun.shadow_enabled = n < 0.7
	env.sky = sky_night if n > 0.55 else sky_day
	env.background_energy_multiplier = (lerpf(1.0, 0.35, clampf(n * 2.0, 0, 1)) * (0.7 if ov > 0 else 1.0)) if n <= 0.55 else 1.0
	env.ambient_light_energy = lerpf(1.0, 0.45, n) * (0.8 if ov > 0 else 1.0)
	env.fog_light_color = Color(0.74, 0.79, 0.86).lerp(Color(0.62, 0.64, 0.68), ov).lerp(Color(0.05, 0.06, 0.1), n)
	var tfog := 0.0018
	if weather == "fog":
		tfog = 0.03
	elif weather == "rain":
		tfog = 0.006
	env.fog_density = lerpf(env.fog_density, tfog, 0.03)
	env.tonemap_exposure = 1.0 + n * 0.25
	city.glass_lit.emission_energy_multiplier = clampf(n * 1.6, 0, 1) * 2.4
	city.lamp_head.emission_energy_multiplier = 0.2 + clampf(n * 1.6, 0, 1) * 6.0
	var hl := 6.0 if (lights_on or n > 0.35) else 0.0
	head_l.light_energy = hl
	head_r.light_energy = hl
	rain.emitting = weather == "rain" and mode != "interior"

func _update_camera(delta: float) -> void:
	if game == null or not game.playing():
		title_ang += delta * 0.05
		var c := Vector3(20, 0, 64)
		cam.global_position = c + Vector3(cos(title_ang) * 90, 50, sin(title_ang) * 90)
		cam.look_at(c, Vector3.UP)
		cam.fov = 58.0
		return
	var sh := Vector3(randf() - 0.5, randf() - 0.5, 0) * shake * 0.3
	if mode == "interior" and interior:
		var ip := interior.global_position
		cam.global_position = ip + Vector3(8, 9.5, 16.5)
		cam.look_at(ip + Vector3(8, 0, 5.8), Vector3.UP)
		cam.fov = 52.0
		pvis.visible = true
		return
	if mode == "car":
		var xf := car.global_transform
		var car_yaw := atan2(xf.basis.z.x, xf.basis.z.z)
		if view == "fps" and not C.VEH[car_type].get("moto", false):
			cam.global_position = xf * driver_seat + sh * 0.3
			cam.look_at(xf * (driver_seat + Vector3(0, -0.05, 10)), Vector3.UP)
			cam.fov = 72.0
		elif view == "top":
			var fwd := Vector3(sin(car_yaw), 0, cos(car_yaw))
			cam.global_position = cam.global_position.lerp(car.global_position + Vector3(0, 42, 0) - fwd * 14.0, 1.0 - exp(-5.0 * delta))
			cam.look_at(car.global_position + fwd * 6.0, Vector3.UP)
			cam.fov = 60.0
		else:
			if cam_free <= 0.0:
				var back_yaw := car_yaw + PI
				if car.linear_velocity.dot(xf.basis.z) < -3.0:
					back_yaw = car_yaw
				cam_yaw = lerp_angle(cam_yaw, back_yaw, 1.0 - exp(-4.0 * delta))
				cam_pitch = lerpf(cam_pitch, -0.18, 1.0 - exp(-3.0 * delta))
			var dist := 7.2 + car_speed() * 0.05
			var target := car.global_position + Vector3(0, 1.1, 0)
			var dirv := Vector3(sin(cam_yaw), 0, cos(cam_yaw))
			for i in 4:
				var q := target + dirv * dist
				if not city.inside_building(Vector2(q.x, q.z), 0.4):
					break
				dist *= 0.65
			var off := dirv * dist * cos(cam_pitch) + Vector3(0, 2.0 - sin(cam_pitch) * dist, 0)
			cam.global_position = cam.global_position.lerp(target + off, 1.0 - exp(-10.0 * delta)) + sh
			cam.look_at(target, Vector3.UP)
			cam.fov = 66.0 + minf(12.0, car_speed() * 0.35)
	else:
		var head := player.global_position + Vector3(0, 1.62, 0)
		if view == "fps":
			pvis.visible = false
			cam.global_position = head + Vector3(sin(cam_yaw + PI), 0, cos(cam_yaw + PI)) * 0.15
			cam.rotation = Vector3(cam_pitch, cam_yaw, 0)
			cam.fov = 75.0
		elif view == "top":
			pvis.visible = true
			cam.global_position = cam.global_position.lerp(player.global_position + Vector3(0, 26, 8), 1.0 - exp(-6.0 * delta))
			cam.look_at(player.global_position, Vector3.UP)
			cam.fov = 60.0
		else:
			pvis.visible = true
			var dist2 := 3.4
			var back := Vector3(sin(cam_yaw), 0, cos(cam_yaw))
			var right := Vector3(back.z, 0, -back.x)
			var p := head + back * dist2 * cos(cam_pitch) + Vector3(0, -sin(cam_pitch) * dist2 + 0.2, 0) - right * 0.55
			cam.global_position = cam.global_position.lerp(p, 1.0 - exp(-14.0 * delta))
			cam.look_at(head - right * 0.55 - back * 2.0 + Vector3(0, -0.25, 0), Vector3.UP)
			cam.fov = 68.0
