"""
gen_pvp_closed.py
Generates pvp_zone_closed.png — 640x380px banner for "PvP Zone Closed" state.
"""
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import math, os

W, H = 640, 380
CX, CY = 320, 182  # visual center (slightly above physical center for text balance)

OUT = os.path.join(os.path.dirname(__file__), "pvp_zone_closed.png")

# ── Color palette ─────────────────────────────────────────────────
BG          = ( 7,  7, 16, 255)
PURPLE_GLOW = (80, 20,130)
CYAN_GLOW   = ( 0,180,220)
RED_MAIN    = (220, 38, 38)
GOLD_HI     = (210,168, 62)
GOLD_MID    = (165,120, 36)
GOLD_DRK    = (110, 82, 18)
STEEL_HI    = (195,208,222)
STEEL_MID   = (120,133,148)
STEEL_DRK   = ( 58, 68, 82)
LEATHER     = ( 48, 30, 18)
LEATHER_HI  = ( 82, 55, 30)
RUNE_CYAN   = (  0,180,210, 90)

# ── Geometry helpers ──────────────────────────────────────────────
def rot(x, y, cx, cy, a):
    c, s = math.cos(a), math.sin(a)
    dx, dy = x - cx, y - cy
    return (cx + dx*c - dy*s, cy + dx*s + dy*c)

def rp(poly, cx, cy, a):
    return [rot(x, y, cx, cy, a) for x, y in poly]

# ── Sword geometry ────────────────────────────────────────────────
def sword_shapes(cx, cy, L, angle_deg):
    """Returns list of (polygon, color) for a sword at given angle."""
    a = math.radians(angle_deg - 90)   # 0° → right, 90° → up

    # ── Blade ──────────────────────────────────────────────────
    TIP   = -L * 0.500
    GUARD =  L * 0.115
    BW    =  L * 0.019   # blade half-width at base

    blade = [
        (cx,        cy + TIP),
        (cx + BW*1.5, cy + GUARD - L*0.01),
        (cx - BW*1.5, cy + GUARD - L*0.01),
    ]
    # Edge highlight (bright sliver along one face)
    blade_hi = [
        (cx + BW*0.05, cy + TIP + L*0.025),
        (cx + BW*1.10, cy + GUARD - L*0.025),
        (cx + BW*0.65, cy + GUARD - L*0.025),
        (cx + BW*0.03, cy + TIP + L*0.048),
    ]
    # Shadow side
    blade_sh = [
        (cx - BW*0.05, cy + TIP + L*0.025),
        (cx - BW*1.10, cy + GUARD - L*0.025),
        (cx - BW*0.60, cy + GUARD - L*0.025),
        (cx - BW*0.03, cy + TIP + L*0.048),
    ]

    # Fuller (central groove along blade)
    fuller = [
        (cx - BW*0.18, cy + TIP + L*0.06),
        (cx + BW*0.18, cy + TIP + L*0.06),
        (cx + BW*0.12, cy + GUARD - L*0.04),
        (cx - BW*0.12, cy + GUARD - L*0.04),
    ]

    # Rune marks (3 small rectangles etched into blade)
    runes = []
    for i in range(3):
        ry = TIP + L * 0.12 + i * L * 0.110
        r = [
            (cx - BW*0.50, cy + ry),
            (cx + BW*0.50, cy + ry),
            (cx + BW*0.50, cy + ry + L*0.018),
            (cx - BW*0.50, cy + ry + L*0.018),
        ]
        runes.append(r)

    # ── Crossguard ─────────────────────────────────────────────
    GW = L * 0.225
    GH = L * 0.040
    GY = GUARD

    # Central bar
    g_bar = [
        (cx - GW/2,          cy + GY - GH*0.35),
        (cx + GW/2,          cy + GY - GH*0.35),
        (cx + GW/2,          cy + GY + GH*0.65),
        (cx - GW/2,          cy + GY + GH*0.65),
    ]
    # Left finial
    g_fin_L = [
        (cx - GW/2,           cy + GY - GH*0.65),
        (cx - GW/2 + L*0.022, cy + GY - GH*0.65),
        (cx - GW/2 + L*0.022, cy + GY + GH*0.90),
        (cx - GW/2,           cy + GY + GH*0.90),
    ]
    # Right finial
    g_fin_R = [
        (cx + GW/2 - L*0.022, cy + GY - GH*0.65),
        (cx + GW/2,            cy + GY - GH*0.65),
        (cx + GW/2,            cy + GY + GH*0.90),
        (cx + GW/2 - L*0.022, cy + GY + GH*0.90),
    ]
    # Top highlight on guard
    g_hi = [
        (cx - GW/2 + L*0.005, cy + GY - GH*0.65),
        (cx + GW/2 - L*0.005, cy + GY - GH*0.65),
        (cx + GW/2 - L*0.005, cy + GY - GH*0.15),
        (cx - GW/2 + L*0.005, cy + GY - GH*0.15),
    ]

    # ── Grip ───────────────────────────────────────────────────
    GT  = GY + GH * 0.65
    GB  = GT + L * 0.215
    GRW = L * 0.028

    grip = [
        (cx - GRW, cy + GT),
        (cx + GRW, cy + GT),
        (cx + GRW, cy + GB),
        (cx - GRW, cy + GB),
    ]
    # Leather wrapping bands (6 tight horizontal bands)
    bands = []
    for i in range(7):
        by = GT + (GB - GT) * (i + 0.5) / 7
        band = [
            (cx - GRW*1.30, cy + by - L*0.0045),
            (cx + GRW*1.30, cy + by - L*0.0045),
            (cx + GRW*1.30, cy + by + L*0.0045),
            (cx - GRW*1.30, cy + by + L*0.0045),
        ]
        bands.append(band)

    # ── Pommel ─────────────────────────────────────────────────
    PS = L * 0.058
    PY = GB
    pommel = [
        (cx,         cy + PY),
        (cx + PS*1.1, cy + PY + PS * 0.95),
        (cx,          cy + PY + PS * 1.95),
        (cx - PS*1.1, cy + PY + PS * 0.95),
    ]
    pom_hi = [
        (cx,          cy + PY + L*0.006),
        (cx + PS*0.65, cy + PY + PS * 0.90),
        (cx,           cy + PY + PS * 1.55),
        (cx - PS*0.65, cy + PY + PS * 0.90),
    ]
    pom_dot = [
        (cx - L*0.008, cy + PY + PS*0.82),
        (cx + L*0.008, cy + PY + PS*0.82),
        (cx + L*0.008, cy + PY + PS*1.12),
        (cx - L*0.008, cy + PY + PS*1.12),
    ]

    # ── Assemble with rotation ─────────────────────────────────
    def R(poly): return rp(poly, cx, cy, a)

    result = [
        (R(blade),      STEEL_MID),
        (R(blade_sh),   STEEL_DRK),
        (R(blade_hi),   STEEL_HI),
        (R(fuller),     STEEL_DRK),
    ]
    for run in runes:
        result.append((R(run), RUNE_CYAN[:3]))

    result += [
        (R(g_bar),    GOLD_DRK),
        (R(g_fin_L),  GOLD_MID),
        (R(g_fin_R),  GOLD_MID),
        (R(g_hi),     GOLD_HI),
        (R(grip),     LEATHER),
    ]
    for b in bands:
        result.append((R(b), LEATHER_HI))

    result += [
        (R(pommel),  GOLD_MID),
        (R(pom_hi),  GOLD_HI),
        (R(pom_dot), GOLD_DRK),
    ]
    return result


# ═══════════════════════════════════════════════════════════════════
# COMPOSE IMAGE
# ═══════════════════════════════════════════════════════════════════

img = Image.new('RGBA', (W, H), BG)
draw = ImageDraw.Draw(img, 'RGBA')

# ── 1. Background ambiance: radial purple fog ──────────────────────
fog = Image.new('RGBA', (W, H), (0, 0, 0, 0))
fd  = ImageDraw.Draw(fog, 'RGBA')
for r in range(220, 0, -3):
    t = 1 - r / 220
    alpha = int(t * t * 38)
    fd.ellipse([CX-r, CY-r, CX+r, CY+r], fill=(*PURPLE_GLOW, alpha))
fog = fog.filter(ImageFilter.GaussianBlur(30))
img = Image.alpha_composite(img, fog)

draw = ImageDraw.Draw(img, 'RGBA')

# ── 2. Arcane rune circle behind swords ───────────────────────────
for ring_r, ring_a in [(90, 18), (93, 18), (118, 14), (121, 14)]:
    for deg in range(0, 360, 1):
        ang = math.radians(deg)
        x = CX + ring_r * math.cos(ang)
        y = CY + ring_r * math.sin(ang)
        draw.point((x, y), fill=(*CYAN_GLOW, ring_a))

# Tick marks (rune indicators around the outer ring)
for i in range(32):
    ang = math.radians(i * (360 / 32))
    is_major = (i % 4 == 0)
    r_in  = 123
    r_out = 132 if is_major else 128
    alpha = 50 if is_major else 28
    x1 = CX + r_in  * math.cos(ang)
    y1 = CY + r_in  * math.sin(ang)
    x2 = CX + r_out * math.cos(ang)
    y2 = CY + r_out * math.sin(ang)
    draw.line([(x1, y1), (x2, y2)], fill=(*CYAN_GLOW, alpha), width=1)

# Diamond markers at cardinal points of circle
for deg in [0, 90, 180, 270]:
    ang = math.radians(deg)
    mx = CX + 130 * math.cos(ang)
    my = CY + 130 * math.sin(ang)
    ds = 4
    draw.polygon([(mx, my-ds), (mx+ds, my), (mx, my+ds), (mx-ds, my)],
                 fill=(*GOLD_MID, 180))

# Inner decorative circle (very faint)
for deg in range(0, 360, 3):
    ang = math.radians(deg)
    x = CX + 70 * math.cos(ang)
    y = CY + 70 * math.sin(ang)
    draw.point((x, y), fill=(*CYAN_GLOW, 10))


# ── 3. Sword glow layer ────────────────────────────────────────────
L = 272

glow_layer = Image.new('RGBA', (W, H), (0, 0, 0, 0))
gd = ImageDraw.Draw(glow_layer, 'RGBA')

for ang in [-45, 45]:
    for shape, col in sword_shapes(CX, CY, L, ang):
        r, g, b = col[:3]
        gd.polygon(shape, fill=(min(255, r+60), min(255, g+50), min(255, b+50), 45))

glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(18))
img = Image.alpha_composite(img, glow_layer)

# ── 4. Draw swords ────────────────────────────────────────────────
draw = ImageDraw.Draw(img, 'RGBA')

# Sword 1 (angled left, drawn first → behind)
for shape, col in sword_shapes(CX, CY, L, -45):
    draw.polygon(shape, fill=col)

# Sword 2 (angled right, drawn second → in front at intersection)
for shape, col in sword_shapes(CX, CY, L, 45):
    draw.polygon(shape, fill=col)


# ── 5. Center intersection sparkle ───────────────────────────────
spark = Image.new('RGBA', (W, H), (0, 0, 0, 0))
sd    = ImageDraw.Draw(spark, 'RGBA')
sd.ellipse([CX-14, CY-14, CX+14, CY+14], fill=(0, 210, 255, 100))
spark = spark.filter(ImageFilter.GaussianBlur(10))
img = Image.alpha_composite(img, spark)

# Tiny bright cross at intersection
draw = ImageDraw.Draw(img, 'RGBA')
draw.line([(CX-6, CY), (CX+6, CY)], fill=(200, 235, 255, 200), width=1)
draw.line([(CX, CY-6), (CX, CY+6)], fill=(200, 235, 255, 200), width=1)


# ── 6. Horizontal ornamental rules ────────────────────────────────
RULE_PAD = 72
LINE_TOP  = 30
LINE_BOT  = H - 30

def draw_rule(y, fade=True):
    for x in range(RULE_PAD, W - RULE_PAD):
        t = (x - RULE_PAD) / (W - 2*RULE_PAD)
        if fade:
            alpha = int(50 * math.sin(t * math.pi))
        else:
            alpha = 40
        draw.point((x, y), fill=(*CYAN_GLOW, alpha))

draw_rule(LINE_TOP)
draw_rule(LINE_BOT)

# Diamond ornaments at rule center
for y in [LINE_TOP, LINE_BOT]:
    ds = 4
    draw.polygon([(CX, y-ds), (CX+ds, y), (CX, y+ds), (CX-ds, y)],
                 fill=(*GOLD_HI, 210))
    # Small diamonds at ends
    for ex in [RULE_PAD + 10, W - RULE_PAD - 10]:
        ds2 = 2
        draw.polygon([(ex, y-ds2), (ex+ds2, y), (ex, y+ds2), (ex-ds2, y)],
                     fill=(*GOLD_MID, 140))

# Thin corner marks
for cx2, cy2, sx, sy in [(RULE_PAD, LINE_TOP, 1, 1),
                           (W-RULE_PAD, LINE_TOP, -1, 1),
                           (RULE_PAD, LINE_BOT, 1, -1),
                           (W-RULE_PAD, LINE_BOT, -1, -1)]:
    draw.line([(cx2, cy2), (cx2 + sx*12, cy2)], fill=(*GOLD_DRK, 130), width=1)
    draw.line([(cx2, cy2), (cx2, cy2 + sy*12)], fill=(*GOLD_DRK, 130), width=1)


# ── 7. Typography ─────────────────────────────────────────────────
FONT_DIR = "C:/Windows/Fonts/"

def tf(paths, size):
    for p in paths:
        full = FONT_DIR + p
        if os.path.exists(full):
            try:
                return ImageFont.truetype(full, size)
            except:
                pass
    return ImageFont.load_default()

# Font selection
f_header  = tf(['ARIALNB.TTF', 'arialbd.ttf'], 11)   # "ZONA PvP" — spaced caps
f_main    = tf(['ariblk.ttf', 'arialbd.ttf'],  56)   # "CERRADA" — heavy
f_tagline = tf(['georgiai.ttf', 'georgiaz.ttf', 'georgia.ttf'], 11)  # tagline italic

# ─ "Z  O  N  A     P v P" ─ top, letter-spaced ─────────────────
header_text = "Z  O  N  A     P v P"
bb = draw.textbbox((0, 0), header_text, font=f_header)
tw = bb[2] - bb[0]
tx_h = (W - tw) // 2
draw.text((tx_h, LINE_TOP + 7), header_text, font=f_header,
          fill=(155, 162, 178, 200))

# ─ "CERRADA" — large red seal ──────────────────────────────────
cerrada_text = "CERRADA"
bb = draw.textbbox((0, 0), cerrada_text, font=f_main)
tw_c = bb[2] - bb[0]
th_c = bb[3] - bb[1]
tx_c = (W - tw_c) // 2
ty_c = CY + 118

# Red glow behind CERRADA
c_glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
cgd    = ImageDraw.Draw(c_glow, 'RGBA')
cgd.text((tx_c, ty_c), cerrada_text, font=f_main, fill=(*RED_MAIN, 130))
c_glow = c_glow.filter(ImageFilter.GaussianBlur(12))
img = Image.alpha_composite(img, c_glow)

draw = ImageDraw.Draw(img, 'RGBA')
draw.text((tx_c, ty_c), cerrada_text, font=f_main, fill=(*RED_MAIN, 235))

# Thin separator line below CERRADA
sep_y = ty_c + th_c + 6
sep_w = tw_c
draw.line([(tx_c + sep_w//4, sep_y), (tx_c + 3*sep_w//4, sep_y)],
          fill=(*RED_MAIN, 70), width=1)

# ─ Tagline ─────────────────────────────────────────────────────
tag_text = "Prepárate.  El mejor está por llegar."
bb = draw.textbbox((0, 0), tag_text, font=f_tagline)
tw_t = bb[2] - bb[0]
draw.text(((W - tw_t) // 2, LINE_BOT - 20), tag_text, font=f_tagline,
          fill=(*GOLD_MID, 185))


# ── 8. Final vignette (darken edges) ─────────────────────────────
vig = Image.new('RGBA', (W, H), (0, 0, 0, 0))
vd  = ImageDraw.Draw(vig, 'RGBA')
for i in range(40):
    alpha = int((i / 40) ** 1.8 * 120)
    vd.rectangle([i, i, W-i, H-i], outline=(0, 0, 0, alpha))
img = Image.alpha_composite(img, vig)


# ── Save ──────────────────────────────────────────────────────────
img.convert('RGB').save(OUT, 'PNG', optimize=True)
print(f"Saved → {OUT}  ({W}x{H}px)")
