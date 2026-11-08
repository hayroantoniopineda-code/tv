#!/usr/bin/env python3
"""Genera la lista central de paisajes (fondos/*.json) desde Wikimedia Commons.

Corre en GitHub Actions una vez al día. Las teles solo leen estos archivos de texto:
nunca consultan la API ni guardan fotos. Sin claves.
Cada foto: u=url (1920 px), a=autor, l=lugar, t=m/t/n/d (mañana/tarde/noche/día), s=o/i/p/v (estación).
"""
import html, json, os, re, sys, time, urllib.parse, urllib.request
from concurrent.futures import ThreadPoolExecutor

API = "https://commons.wikimedia.org/w/api.php"
UA = "MiTV-fondos/1.0 (https://github.com/hayroantoniopineda-code/tv)"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "fondos")
MAX = 300

NATURE = re.compile(r"\b(landscapes?|mountains?|lakes?|rivers?|beach(es)?|sea|ocean|coast(line)?|forests?|woods|"
                    r"waterfalls?|falls|valley|sunset|sunrise|dawn|dusk|canyon|desert|volcano|islands?|bay|skyline|"
                    r"hills?|meadow|fields?|glacier|fjord|clouds|snow|autumn|foliage|panorama|lagoon|cliffs?|dunes|"
                    r"national park|night sky|milky way|paisaje|montañas?|lago|río|playa|mar|costa|bosque|cascada|"
                    r"valle|atardecer|amanecer|volcán|isla|bahía|laguna)\b")
BAD = re.compile(r"\b(people|person|persons|man|men|woman|women|boy|girl|child(ren)?|kids?|portrait|selfie|hands?|"
                 r"face|crowd|tourists?|wedding|couple|phone|smartphone|books?|laptop|computer|screen|interior|"
                 r"indoors?|museum|food|dish|meal|cars?|truck|bus|train|map|document|text|signs?|logo|poster|"
                 r"statue|sculpture|painting|drawing|illustration|stamp|coin|insects?|birds?|dogs?|cats?|horses?|"
                 r"cows?|macro|close-up|closeup|persona|personas|hombre|mujer|niños?|niñas?|retrato|manos?|"
                 r"libros?|celular|teléfono|comida|museo|coche|auto|mapa|cartel)\b")
NIGHT = re.compile(r"\b(night|noche|nocturn\w*|stars|starry|milky way|moon(light|rise)?|luna)\b")
EVE = re.compile(r"\b(sunset|atardecer|dusk|evening|golden hour|puesta|twilight)\b")
MORN = re.compile(r"\b(sunrise|amanecer|dawn|morning|mañana|fog|mist|niebla)\b")
SEASON = [("o", re.compile(r"\b(autumn|fall foliage|otoño|fall colou?rs)\b")),
          ("i", re.compile(r"\b(winter|snow|snowy|nieve|invierno|frozen)\b")),
          ("p", re.compile(r"\b(spring|blossoms?|primavera|cherry)\b")),
          ("v", re.compile(r"\b(summer|verano)\b"))]

COUNTRIES = {
    "HN": "Honduras", "GT": "Guatemala", "SV": "El Salvador", "NI": "Nicaragua", "CR": "Costa Rica",
    "PA": "Panama", "BZ": "Belize", "MX": "Mexico", "US": "United States", "CA": "Canada", "CU": "Cuba",
    "DO": "Dominican Republic", "PR": "Puerto Rico", "JM": "Jamaica", "CO": "Colombia", "VE": "Venezuela",
    "EC": "Ecuador", "PE": "Peru", "BO": "Bolivia", "CL": "Chile", "AR": "Argentina", "UY": "Uruguay",
    "PY": "Paraguay", "BR": "Brazil", "ES": "Spain", "PT": "Portugal", "FR": "France", "IT": "Italy",
    "DE": "Germany", "GB": "United Kingdom", "IE": "Ireland", "NL": "Netherlands", "BE": "Belgium",
    "CH": "Switzerland", "AT": "Austria", "NO": "Norway", "SE": "Sweden", "FI": "Finland", "DK": "Denmark",
    "IS": "Iceland", "PL": "Poland", "CZ": "Czech Republic", "GR": "Greece", "HR": "Croatia", "TR": "Turkey",
    "RO": "Romania", "UA": "Ukraine", "RU": "Russia", "MA": "Morocco", "EG": "Egypt", "ZA": "South Africa",
    "KE": "Kenya", "TZ": "Tanzania", "NA": "Namibia", "IN": "India", "NP": "Nepal", "CN": "China",
    "JP": "Japan", "KR": "South Korea", "TH": "Thailand", "VN": "Vietnam", "PH": "Philippines",
    "ID": "Indonesia", "MY": "Malaysia", "AU": "Australia", "NZ": "New Zealand", "AE": "United Arab Emirates",
    "IL": "Israel", "JO": "Jordan", "SA": "Saudi Arabia",
}
STATES = {
    "AL": "Alabama", "AK": "Alaska", "AZ": "Arizona", "AR": "Arkansas", "CA": "California", "CO": "Colorado",
    "CT": "Connecticut", "DE": "Delaware", "FL": "Florida", "GA": "Georgia", "HI": "Hawaii", "ID": "Idaho",
    "IL": "Illinois", "IN": "Indiana", "IA": "Iowa", "KS": "Kansas", "KY": "Kentucky", "LA": "Louisiana",
    "ME": "Maine", "MD": "Maryland", "MA": "Massachusetts", "MI": "Michigan", "MN": "Minnesota",
    "MS": "Mississippi", "MO": "Missouri", "MT": "Montana", "NE": "Nebraska", "NV": "Nevada",
    "NH": "New Hampshire", "NJ": "New Jersey", "NM": "New Mexico", "NY": "New York", "NC": "North Carolina",
    "ND": "North Dakota", "OH": "Ohio", "OK": "Oklahoma", "OR": "Oregon", "PA": "Pennsylvania",
    "RI": "Rhode Island", "SC": "South Carolina", "SD": "South Dakota", "TN": "Tennessee", "TX": "Texas",
    "UT": "Utah", "VT": "Vermont", "VA": "Virginia", "WA": "Washington State", "WV": "West Virginia",
    "WI": "Wisconsin", "WY": "Wyoming",
}
ES = {"United States": "Estados Unidos", "Mexico": "México", "Panama": "Panamá", "Dominican Republic":
      "Rep. Dominicana", "Peru": "Perú", "Brazil": "Brasil", "Spain": "España", "France": "Francia",
      "Italy": "Italia", "Germany": "Alemania", "United Kingdom": "Reino Unido", "Ireland": "Irlanda",
      "Netherlands": "Países Bajos", "Belgium": "Bélgica", "Switzerland": "Suiza", "Norway": "Noruega",
      "Sweden": "Suecia", "Finland": "Finlandia", "Denmark": "Dinamarca", "Iceland": "Islandia",
      "Poland": "Polonia", "Czech Republic": "Chequia", "Greece": "Grecia", "Croatia": "Croacia",
      "Turkey": "Turquía", "Romania": "Rumania", "Ukraine": "Ucrania", "Russia": "Rusia", "Morocco": "Marruecos",
      "Egypt": "Egipto", "South Africa": "Sudáfrica", "Kenya": "Kenia", "Japan": "Japón", "South Korea":
      "Corea del Sur", "Thailand": "Tailandia", "Philippines": "Filipinas", "Malaysia": "Malasia",
      "New Zealand": "Nueva Zelanda", "United Arab Emirates": "Emiratos Árabes", "Jordan": "Jordania",
      "Saudi Arabia": "Arabia Saudita", "Canada": "Canadá", "Belize": "Belice", "Hawaii": "Hawái",
      "Washington State": "Washington"}
TERMS = ["landscape", "sunset", "night", "mountains lake", "autumn", "snow"]


def get(params):
    url = API + "?" + urllib.parse.urlencode(params)
    for _ in range(3):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.load(r)
        except Exception as e:
            print("error", e, file=sys.stderr, flush=True)
            time.sleep(3)
    return {}


def search(q):
    d = get(dict(action="query", format="json", generator="search", gsrnamespace=6, gsrsearch=q,
                 gsrlimit=50, prop="imageinfo", iiprop="url|size|extmetadata", iiurlwidth=1920,
                 iiextmetadatafilter="ImageDescription|Artist|Categories"))
    time.sleep(0.5)
    return list(d.get("query", {}).get("pages", {}).values())


def clean(s):
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", s or ""))).strip()


def classify(page, label):
    ii = (page.get("imageinfo") or [{}])[0]
    w, h = ii.get("width", 0), ii.get("height", 0)
    if w < 1600 or not h or not 1.3 <= w / h <= 2.4:
        return None
    md = ii.get("extmetadata", {})
    text = " ".join([page.get("title", "").replace("_", " "),
                     clean(md.get("ImageDescription", {}).get("value", ""))[:600],
                     md.get("Categories", {}).get("value", "").replace("|", " ")]).lower()
    if BAD.search(text) or not NATURE.search(text):
        return None
    t = "n" if NIGHT.search(text) else "t" if EVE.search(text) else "m" if MORN.search(text) else "d"
    s = next((k for k, rx in SEASON if rx.search(text)), "")
    url = ii.get("thumburl") or ii.get("url")
    if not url:
        return None
    return {"u": url, "a": clean(md.get("Artist", {}).get("value", ""))[:40], "l": label, "t": t, "s": s}


def region(queries, label):
    out, seen = [], set()
    for q in queries:
        for p in search(q + " filetype:bitmap"):
            e = classify(p, label)
            if e and e["u"] not in seen:
                seen.add(e["u"])
                out.append(e)
    out.sort(key=lambda e: e["u"])  # orden fijo: todas las teles ven lo mismo
    return out[:MAX]


def save(name, data):
    with open(os.path.join(OUT, name + ".json"), "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
    print(name, len(data), flush=True)


def main():
    os.makedirs(OUT, exist_ok=True)
    cfg = os.path.join(OUT, "config.json")
    if not os.path.exists(cfg):
        with open(cfg, "w") as f:
            json.dump({"intervalo": 120}, f)
    jobs = [(c, [f"{n} {t}" for t in TERMS], ES.get(n, n)) for c, n in COUNTRIES.items()]
    jobs += [("US-" + c, [f'"{n}" United States {t}' for t in TERMS[:4]], ES.get(n, n)) for c, n in STATES.items()]
    world = []

    def run(job):
        code, qs, label = job
        data = region(qs, label)
        if data:
            save(code, data)
        return code, data

    with ThreadPoolExecutor(4) as ex:
        for code, data in ex.map(run, jobs):
            if "-" not in code:
                world += data[::6]
    if world:
        save("MUNDO", sorted(world, key=lambda e: e["u"])[:600])


if __name__ == "__main__":
    main()
