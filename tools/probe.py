import urllib.request, re, json
UA={"User-Agent":"FutbolMenajerDB/1.0 (+https://github.com/oski11er/devriye-oyun)"}
def get(u): return urllib.request.urlopen(urllib.request.Request(u,headers=UA),timeout=30).read().decode("utf-8","replace")
h=get("https://starting11.com/galatasaray/squad")
for m in re.finditer(r'<script type="application/ld\+json"[^>]*>(.*?)</script>',h,re.S):
    s=m.group(1); print("=== LD", s[:1200]); print()
for w in ["Eren","Uğurcan","Ugurcan","Goalkeepers","Defenders","shirt","position"]:
    i=h.find(w); print("=== find",w,i, re.sub(r'\s+',' ',h[max(0,i-300):i+500]) if i>=0 else "")
p=get("https://starting11.com/player/elmali")
for m in re.finditer(r'<script type="application/ld\+json"[^>]*>(.*?)</script>',p,re.S):
    print("=== PLAYER LD", m.group(1)[:1500])
i=p.find("Eren"); print("=== player Eren", re.sub(r'\s+',' ',p[max(0,i-200):i+400]))
for w in ["value","Value","€","height","foot","contract"]:
    i=p.find(w); print("=== pfind",w,i, re.sub(r'\s+',' ',re.sub(r'<[^>]+>',' ',p[max(0,i-200):i+300])) if i>=0 else "")
