#!/usr/bin/env python3
"""Aden Chronicles — Community Board visual canvas generator."""

from PIL import Image, ImageDraw, ImageFont, ImageFilter
import math, os

W, H = 1240, 1800

FDIR = (
    r"C:\Users\david\AppData\Roaming\Claude\local-agent-mode-sessions"
    r"\skills-plugin\cc71bd61-4f4d-4651-9423-2c73dad3a964"
    r"\5dbd1f5b-f822-4ad6-b6d5-837ae953be4f\skills\canvas-design\canvas-fonts"
)
ODIR = r"C:\Users\david\OneDrive\Desktop\l2j\game\data\html\CommunityBoard\Custom"

# ── Palette ───────────────────────────────────────────────
BG       = (4,   4,  14)
PANEL    = (8,   8,  28)
PANEL_LT = (12,  12, 36)
GOLD     = (200, 168, 75)
GDIM     = (138, 108, 42)
GGLOW    = (255, 224, 138)
TEXT_C   = (200, 192, 160)
TDIM     = (122, 112, 96)
ONLINE_C = (62,  207, 106)

def fnt(name, size):
    return ImageFont.truetype(os.path.join(FDIR, name), size)

# ── Glow helper ───────────────────────────────────────────
def add_glow(img, cx, cy, r, rgb, alpha=50):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for i in range(8):
        t  = i / 8
        a  = int(alpha * (1 - t ** 0.6))
        rr = int(r * (1 - t * 0.5))
        d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=(*rgb, a))
    return Image.alpha_composite(img, layer.filter(ImageFilter.GaussianBlur(70)))

# ── Card background helper ────────────────────────────────
def draw_card(img, x, y, w, h):
    ov = Image.new("RGBA", (w, h), (*PANEL, 240))
    img.paste(ov, (x, y), ov)
    d = ImageDraw.Draw(img)
    d.rectangle([(x, y), (x + w - 1, y + h - 1)], outline=(*GDIM, 70), width=1)
    for i in range(w):
        t = 4.0 * (i / w) * (1.0 - i / w)
        d.line([(x + i, y), (x + i, y + 1)], fill=(*GDIM, int(148 * t)))
    return d

# ── Build canvas ──────────────────────────────────────────
img = Image.new("RGBA", (W, H), (*BG, 255))

img = add_glow(img, W // 2, 215,  700, (118, 84, 16),  78)
img = add_glow(img, 80,     740,  320, (100, 14, 14),  46)
img = add_glow(img, W - 80, 960,  320, (12,  18, 82),  50)
img = add_glow(img, W // 2, 1600, 500, (8,   12, 56),  38)

d = ImageDraw.Draw(img)

# ══════════════════════════════════════════════════════════
# HERO BANNER   y 0 → 440
# ══════════════════════════════════════════════════════════
HBH = 440
hbg = Image.new("RGBA", (W, HBH), (4, 4, 18, 250))
img.paste(hbg, (0, 0), hbg)
d = ImageDraw.Draw(img)

CX, CY = W // 2, 206

# Concentric diamond sigils
for sz in (312, 248, 184, 120, 60):
    a   = int(10 + (312 - sz) * 0.13)
    pts = [(CX, CY - sz), (CX + sz, CY), (CX, CY + sz), (CX - sz, CY), (CX, CY - sz)]
    d.line(pts, fill=(*GDIM, a), width=1)

# Radial tick ring
for angle in range(0, 360, 8):
    rad = math.radians(angle)
    r1  = 124
    r2  = 134 if angle % 90 == 0 else (129 if angle % 45 == 0 else 126)
    aa  = 88  if angle % 90 == 0 else (58  if angle % 45 == 0 else 24)
    d.line(
        [(CX + r1 * math.cos(rad), CY + r1 * math.sin(rad)),
         (CX + r2 * math.cos(rad), CY + r2 * math.sin(rad))],
        fill=(*GDIM, aa), width=1,
    )

# Corner sigil marks
for ang in (45, 135, 225, 315):
    rad = math.radians(ang)
    bx  = CX + 268 * math.cos(rad)
    by  = CY + 268 * math.sin(rad)
    s   = 11
    d.line([(bx, by - s), (bx + s, by), (bx, by + s), (bx - s, by), (bx, by - s)],
           fill=(*GDIM, 55), width=1)
    d.line([(bx - s * 1.9, by), (bx - s, by)], fill=(*GDIM, 32), width=1)
    d.line([(bx + s, by), (bx + s * 1.9, by)], fill=(*GDIM, 32), width=1)

# Title glow layer
gl  = Image.new("RGBA", (W, H), (0, 0, 0, 0))
gld = ImageDraw.Draw(gl)
ft  = fnt("Italiana-Regular.ttf", 94)
ttl = "ADEN  CHRONICLES"
tb  = gld.textbbox((0, 0), ttl, font=ft)
tx  = (W - (tb[2] - tb[0])) // 2
ty  = CY - 68
gld.text((tx, ty), ttl, font=ft, fill=(*GGLOW, 52))
img = Image.alpha_composite(img, gl.filter(ImageFilter.GaussianBlur(14)))
d   = ImageDraw.Draw(img)
d.text((tx, ty), ttl, font=ft, fill=GOLD)

# Subtitle
fs  = fnt("Jura-Light.ttf", 16)
sub = "LINEAGE  II    HIGH FIVE    CHRONICLES  OF  THE  ABYSS"
sb  = d.textbbox((0, 0), sub, font=fs)
sw  = sb[2] - sb[0]
sx  = (W - sw) // 2
d.text((sx, ty + 100), sub, font=fs, fill=TDIM)

# Flanking rules
ry = ty + 124
d.line([(sx, ry), (sx + 160, ry)], fill=(*GDIM, 55), width=1)
d.line([(sx + sw - 160, ry), (sx + sw, ry)], fill=(*GDIM, 55), width=1)

# Hero bottom border
d.line([(0, HBH - 1), (W, HBH - 1)], fill=(*GDIM, 88), width=1)

# ══════════════════════════════════════════════════════════
# STATUS BAR   y 440 → 496
# ══════════════════════════════════════════════════════════
SBY, SBH = HBH, 56
img.paste(Image.new("RGBA", (W, SBH), (4, 4, 15, 253)), (0, SBY),
          Image.new("RGBA", (W, SBH), (4, 4, 15, 253)))
d = ImageDraw.Draw(img)
d.line([(0, SBY + SBH - 1), (W, SBY + SBH - 1)], fill=(*GDIM, 72), width=1)

fp    = fnt("GeistMono-Regular.ttf", 11)
pills = [
    ("+ ONLINE", ONLINE_C),
    ("EXP  x10", GOLD),
    ("SP   x10", GOLD),
    ("DROP  x8", GOLD),
    ("ADENA x10", GOLD),
]
px, py = 30, SBY + 15
for lbl, col in pills:
    lb = d.textbbox((0, 0), lbl, font=fp)
    lw = lb[2] - lb[0] + 26
    d.rounded_rectangle([(px, py), (px + lw, py + 26)],
                         radius=3, fill=(*PANEL_LT, 255), outline=(*GDIM, 46))
    d.text((px + 13, py + 7), lbl, font=fp, fill=col)
    px += lw + 12

# ══════════════════════════════════════════════════════════
# NAV TABS   y 496 → 552
# ══════════════════════════════════════════════════════════
NVY, NVH = SBY + SBH, 56
d.rectangle([(0, NVY), (W, NVY + NVH)], fill=BG)
d.line([(0, NVY + NVH - 1), (W, NVY + NVH - 1)], fill=(*GDIM, 108), width=2)

fn   = fnt("ArsenalSC-Regular.ttf", 12)
tabs = ["HOME", "EVENTS", "RANKINGS", "SHOP", "RULES", "COMMANDS"]
tx3  = 30
for i, tab in enumerate(tabs):
    tb3 = d.textbbox((0, 0), tab, font=fn)
    tw3 = tb3[2] - tb3[0]
    p   = 15
    if i == 0:
        d.rectangle([(tx3 - p, NVY), (tx3 + tw3 + p, NVY + NVH - 2)],
                    fill=(*GOLD, 18))
        d.line([(tx3 - p, NVY + 1), (tx3 + tw3 + p, NVY + 1)],
               fill=GOLD, width=2)
        d.text((tx3, NVY + 18), tab, font=fn, fill=GGLOW)
    else:
        d.text((tx3, NVY + 18), tab, font=fn, fill=TDIM)
    tx3 += tw3 + p * 2 + 16

# ══════════════════════════════════════════════════════════
# WELCOME CARD   y 572 → 800
# ══════════════════════════════════════════════════════════
PX  = 32
WCY = NVY + NVH + 20
WCH = 228
d   = draw_card(img, PX, WCY, W - PX * 2, WCH)

fct = fnt("Italiana-Regular.ttf", 33)
fcb = fnt("CrimsonPro-Regular.ttf", 15)
fjl = fnt("Jura-Light.ttf", 11)

d.text((PX + 26, WCY + 18), "Welcome to Aden Chronicles", font=fct, fill=GOLD)

lore = (
    "In the age before reckoning, when the Dragon Lords still cast their shadows across Aden,\n"
    "the Eternal Chronicle was inscribed — a living record of those who dared challenge destiny.\n"
    "You stand at the threshold. The Abyss remembers every name carved into its ancient walls."
)
d.multiline_text((PX + 26, WCY + 65), lore, font=fcb, fill=TEXT_C, spacing=7)
d.line([(PX + 26, WCY + 178), (W - PX - 26, WCY + 178)], fill=(*GDIM, 45), width=1)
d.text((PX + 26, WCY + 188),
       "Season I  ·  EXP x10  SP x10  Drop x8  Adena x10",
       font=fjl, fill=TDIM)

# ══════════════════════════════════════════════════════════
# FEATURES GRID   y 820 → 1232
# ══════════════════════════════════════════════════════════
GY  = WCY + WCH + 20
GAP = 13
CW  = (W - PX * 2 - GAP * 2) // 3
RH  = 192

feats = [
    ("I",   "Custom PvP Zone",
     "Enter the Sacred Grounds.\nRanked combat, dynamic zone\ntitles and PvP scoring await."),
    ("II",  "Seasonal Events",
     "Limited-time tournaments\nand challenges rewarded with\nexclusive treasures and titles."),
    ("III", "Augment Master",
     "Custom life stone crafting.\nRare augmentation options\nbeyond standard boundaries."),
    ("IV",  "Olympiad System",
     "Prove worth in the grand\narena. Competitive ranks and\nthe hero status await."),
    ("V",   "Community Board",
     "Gateway to all features,\nrankings, shop, and the living\nvoice of the realm."),
    ("VI",  "Vote Rewards",
     "Vote daily and receive\nrare currency, blessings,\nand ancient relics."),
]

ffi = fnt("CrimsonPro-Bold.ttf",    22)
fft = fnt("ArsenalSC-Regular.ttf",  12)
ffb = fnt("CrimsonPro-Regular.ttf", 13)

for idx, (numeral, title, body) in enumerate(feats):
    col = idx % 3
    row = idx // 3
    fx  = PX + col * (CW + GAP)
    fy  = GY + row * (RH + GAP)
    d   = draw_card(img, fx, fy, CW, RH)
    # Bordered numeral box
    bx0, by0 = fx + 14, fy + 14
    bx1, by1 = fx + 52, fy + 52
    d.rectangle([(bx0, by0), (bx1, by1)], outline=(*GDIM, 68), width=1)
    nb  = d.textbbox((0, 0), numeral, font=ffi)
    nox = bx0 + (38 - (nb[2] - nb[0])) // 2
    noy = by0 + (38 - (nb[3] - nb[1])) // 2
    d.text((nox, noy), numeral, font=ffi, fill=GDIM)
    d.text((fx + 14, fy + 60), title, font=fft, fill=GOLD)
    d.multiline_text((fx + 14, fy + 82), body, font=ffb, fill=TDIM, spacing=3)

# ══════════════════════════════════════════════════════════
# RANKINGS CARD   y 1246 → 1518
# ══════════════════════════════════════════════════════════
RKY = GY + 2 * (RH + GAP) + 14
RKH = 272
d   = draw_card(img, PX, RKY, W - PX * 2, RKH)

frt = fnt("ArsenalSC-Regular.ttf", 14)
frh = fnt("GeistMono-Regular.ttf", 10)
frn = fnt("CrimsonPro-Bold.ttf",   14)
frd = fnt("GeistMono-Regular.ttf", 12)

d.text((PX + 26, RKY + 18), "Hall of Legends  —  Kill Rankings", font=frt, fill=GOLD)

TBX = PX + 26
TBY = RKY + 56
TBW = W - PX * 2 - 52

d.rectangle([(TBX, TBY), (TBX + TBW, TBY + 24)], fill=(*GDIM, 22))
cxs = [TBX + 10, TBX + 55, TBX + 295, TBX + 500, TBX + 700]
for hx4, ht4 in zip(cxs, ["#", "PLAYER", "CLASS", "KILLS", "RANK"]):
    d.text((hx4, TBY + 7), ht4, font=frh, fill=TDIM)

players = [
    ("1", "DarkSeraph",  "Arcana Lord",    "4,821", "Grand Master"),
    ("2", "VoidCrusher", "Dreadnought",    "3,617", "Champion"),
    ("3", "ShadowVeil",  "Storm Screamer", "2,994", "Warlord"),
]
rcols = [GGLOW, (200, 200, 200), (180, 120, 60)]

for i, (rk, nm, cl, ki, ti) in enumerate(players):
    ry4 = TBY + 26 + i * 46
    d.rectangle([(TBX, ry4), (TBX + TBW, ry4 + 44)],
                fill=(*PANEL_LT, 200 if i % 2 == 0 else 160))
    d.line([(TBX, ry4 + 44), (TBX + TBW, ry4 + 44)], fill=(*GDIM, 22), width=1)
    d.text((cxs[0], ry4 + 15), rk, font=frd, fill=rcols[i])
    d.text((cxs[1], ry4 + 13), nm, font=frn, fill=TEXT_C)
    d.text((cxs[2], ry4 + 15), cl, font=frd, fill=TDIM)
    d.text((cxs[3], ry4 + 15), ki, font=frd, fill=GOLD)
    d.text((cxs[4], ry4 + 15), ti, font=frd, fill=TDIM)

# ══════════════════════════════════════════════════════════
# SERVER CHRONICLES STRIP   y 1538 → 1668
# ══════════════════════════════════════════════════════════
SNY = RKY + RKH + 20
SNH = 130
d   = draw_card(img, PX, SNY, W - PX * 2, SNH)

fsnt = fnt("ArsenalSC-Regular.ttf",  13)
fsnb = fnt("CrimsonPro-Regular.ttf", 14)
d.text((PX + 26, SNY + 16), "Latest Chronicles", font=fsnt, fill=GOLD)

news = [
    "NEW    Season I has begun. The Sacred Grounds are open — seek glory.",
    "EVENT  Siege of Castle Aden — every Saturday at 20:00 server time.",
    "UPDATE Augment Master NPC added to Giran Town. Life stone rewards active.",
]
for j, item in enumerate(news):
    d.text((PX + 26, SNY + 44 + j * 26), item, font=fsnb, fill=TDIM)

# ══════════════════════════════════════════════════════════
# FOOTER ORNAMENT   y ~1720
# ══════════════════════════════════════════════════════════
FTY = SNY + SNH + 52
cxf = W // 2

d.line([(PX + 60, FTY), (cxf - 22, FTY)], fill=(*GDIM, 44), width=1)
d.line([(cxf + 22, FTY), (W - PX - 60, FTY)], fill=(*GDIM, 44), width=1)
# Center diamond
d.line([(cxf - 9, FTY), (cxf, FTY - 9), (cxf + 9, FTY),
        (cxf, FTY + 9), (cxf - 9, FTY)], fill=(*GOLD, 82), width=1)
d.ellipse([cxf - 2, FTY - 2, cxf + 2, FTY + 2], fill=(*GOLD, 100))

fftf = fnt("Jura-Light.ttf", 10)
ftf  = "ADEN CHRONICLES  ·  HIGH FIVE  ·  ETERNAL ABYSS  ·  SEASON I"
ftfb = d.textbbox((0, 0), ftf, font=fftf)
d.text(((W - (ftfb[2] - ftfb[0])) // 2, FTY + 18), ftf, font=fftf, fill=TDIM)

# ══════════════════════════════════════════════════════════
# EXPORT
# ══════════════════════════════════════════════════════════
out  = img.convert("RGB")
path = os.path.join(ODIR, "aden_chronicles_board.png")
out.save(path, "PNG")
print("Saved:", path)
