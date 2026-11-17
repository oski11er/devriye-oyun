import urllib.request, re, sys
UA={"User-Agent":"FutbolMenajerDB/1.0 (+https://github.com/oski11er/devriye-oyun; personal game database)"}
def get(u):
    return urllib.request.urlopen(urllib.request.Request(u,headers=UA),timeout=30).read().decode("utf-8","replace")
print("=== robots"); print(get("https://starting11.com/robots.txt")[:1500])
h=get("https://starting11.com/galatasaray/squad")
print("=== len",len(h))
i=h.find("Osimhen"); print("=== around Osimhen"); print(h[max(0,i-3000):i+1500])
links=sorted(set(re.findall(r'href="(/[^"]+)"',h)))
print("=== links sample", [l for l in links if 'player' in l or 'squad' in l][:40])
l=get("https://starting11.com/super-lig")
print("=== league squad links", sorted(set(re.findall(r'href="(/[a-z0-9-]+/squad)"',l)))[:40])
print("=== league len",len(l))
