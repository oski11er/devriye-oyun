"""Poly Haven'dan (CC0) fotogerçekçi PBR dokular ve HDRI gökyüzü indirir -> assets/tex/
Her rol için en çok indirilen uygun varlığı seçer; sonuçları assets/tex/CREDITS.txt'e yazar."""
import json, os, urllib.request, sys

OUT = os.path.join(os.path.dirname(__file__), '..', 'assets', 'tex')
os.makedirs(OUT, exist_ok=True)
UA = {'User-Agent': 'devriye-ci/1.0'}

def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
        return r.read()

def js(url):
    return json.loads(get(url))

ROLES = {
    # rol: (tercih edilen id'ler, anahtar kelimeler)
    'asphalt':  (['asphalt_02', 'asphalt_track', 'asphalt_pit_lane'], ['asphalt', 'road']),
    'pavement': (['concrete_pavement', 'paving_stones_02', 'square_floor_patern_01'], ['pavement', 'paving', 'sidewalk', 'concrete_floor']),
    'brick':    (['red_brick_03', 'brick_wall_001', 'red_brick_wall'], ['brick']),
    'plaster':  (['painted_plaster_wall', 'beige_wall_001', 'white_plaster_02'], ['plaster']),
    'concrete': (['concrete_wall_008', 'concrete_wall_004', 'concrete_wall_003'], ['concrete_wall', 'concrete']),
    'grass':    (['aerial_grass_rock', 'forrest_ground_01', 'coast_land_rocks_01'], ['grass']),
}
HDRIS = {
    'sky_day':   (['kloofendal_48d_partly_cloudy_puresky', 'urban_street_01', 'potsdamer_platz'], ['urban', 'partly cloudy']),
    'sky_night': (['dikhololo_night', 'kloppenheim_02', 'satara_night'], ['night']),
}

credits = []
try:
    textures = js('https://api.polyhaven.com/assets?t=textures')
    hdris = js('https://api.polyhaven.com/assets?t=hdris')
except Exception as e:
    print('Poly Haven API erişilemedi:', e); sys.exit(0)

def choose(pool, prefs, kws):
    for p in prefs:
        if p in pool:
            return p
    best, bc = None, -1
    for k, v in pool.items():
        hay = (k + ' ' + ' '.join(v.get('tags', [])) + ' ' + ' '.join(v.get('categories', []))).lower()
        if any(w in hay for w in kws) and v.get('download_count', 0) > bc:
            best, bc = k, v.get('download_count', 0)
    return best

def save(url, name):
    path = os.path.join(OUT, name)
    if os.path.exists(path) and os.path.getsize(path) > 1000:
        return True
    try:
        data = get(url)
        open(path, 'wb').write(data)
        print('  indirildi', name, len(data))
        return True
    except Exception as e:
        print('  HATA', name, e)
        return False

for role, (prefs, kws) in ROLES.items():
    aid = choose(textures, prefs, kws)
    if not aid:
        print('bulunamadı:', role); continue
    files = js('https://api.polyhaven.com/files/' + aid)
    print(role, '->', aid)
    credits.append(f'{role}: {aid} (Poly Haven, CC0)')
    for key, suffix in [('Diffuse', 'diff'), ('nor_gl', 'nor'), ('Rough', 'rough')]:
        try:
            url = files[key]['1k']['jpg']['url']
        except KeyError:
            alt = [k for k in files if k.lower().startswith(key.lower()[:4])]
            if not alt:
                continue
            url = files[alt[0]]['1k']['jpg']['url']
        save(url, f'{role}_{suffix}.jpg')

for name, (prefs, kws) in HDRIS.items():
    aid = choose(hdris, prefs, kws)
    if not aid:
        continue
    files = js('https://api.polyhaven.com/files/' + aid)
    print(name, '->', aid)
    credits.append(f'{name}: {aid} (Poly Haven, CC0)')
    save(files['hdri']['1k']['hdr']['url'], name + '.hdr')

# ---- foto-taranmış 3D modeller (glTF) -> assets/props/<rol>/<rol>.gltf
PROPS = {
    'bench':   (['park_bench', 'bench'], ['picnic', 'table', 'vice', 'work', 'tool']),
    'bin':     (['trash_can', 'trashcan', 'trash_bin', 'garbage', 'waste_bin', 'dustbin', 'bin_'], ['cabinet']),
    'hydrant': (['fire_hydrant', 'hydrant'], []),
    'planter': (['planter', 'potted_plant'], []),
    'barrier': (['road_barrier', 'barrier', 'barricade', 'jersey'], ['fence', 'chain']),
    'bollard': (['bollard'], ['lamp']),
    'tree':    (['tree_small', 'tree'], ['street', 'dead', 'stump', 'log']),
    'cone':    (['traffic_cone', 'cone'], ['pine']),
}
try:
    models = js('https://api.polyhaven.com/assets?t=models')
    print('MODEL_IDS', ' '.join(sorted(models.keys())))
except Exception as e:
    models = {}
    print('model listesi alınamadı', e)
PROOT = os.path.join(os.path.dirname(__file__), '..', 'assets', 'props')
for role, (kws, bad) in PROPS.items():
    best = None
    for w in kws:
        cands = [k for k in models if w in k.lower() and not any(x in k.lower() for x in bad)]
        if cands:
            best = max(cands, key=lambda k: models[k].get('download_count', 0))
            break
    if not best:
        print('model yok:', role); continue
    try:
        f = js('https://api.polyhaven.com/files/' + best)
        g = f['gltf']['1k']['gltf']
        d = os.path.join(PROOT, role)
        os.makedirs(d, exist_ok=True)
        open(os.path.join(d, role + '.gltf'), 'wb').write(get(g['url']))
        for rel, inc in g.get('include', {}).items():
            dst = os.path.join(d, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            open(dst, 'wb').write(get(inc['url']))
        print('model', role, '->', best)
        credits.append(f'{role} (model): {best} (Poly Haven, CC0)')
    except Exception as e:
        print('model HATA', role, best, e)

open(os.path.join(OUT, 'CREDITS.txt'), 'w').write('\n'.join(credits) + '\n')
print('tamam')
