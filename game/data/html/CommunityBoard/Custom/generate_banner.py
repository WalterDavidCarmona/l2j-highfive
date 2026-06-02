"""
generate_banner.py  v2
Banner L2 Zona Zero - version alto contraste para cliente L2J
Uso: python generate_banner.py
"""

from PIL import Image, ImageDraw, ImageFont
import math, os

OUTPUT_DIR = os.path.dirname(os.path.abspath(__file__))
GIF_OUT    = os.path.join(OUTPUT_DIR, "banner_zonazero.gif")
PNG_OUT    = os.path.join(OUTPUT_DIR, "banner_zonazero.png")

W, H       = 555, 100
FRAMES     = 20
DELAY      = 80   # ms por frame

# ── Colores brillantes, alto contraste ──────────────────────────────────────
BG1        = ( 15,  25,  70)   # azul medio (no tan oscuro)
BG2        = ( 25,  10,  60)   # purpura medio
GOLD       = (255, 210,  50)   # dorado brillante
GOLD2      = (255, 240, 120)   # dorado claro para pulse
BLUE       = ( 80, 180, 255)   # azul cielo brillante
BLUE2      = (140, 220, 255)   # azul claro
RED        = (255,  80,  40)   # naranja-rojo vivo
RED2       = (255, 160,  60)   # naranja claro
WHITE      = (255, 255, 255)
BORDER_C   = (100, 200, 255)   # borde azul claro
ACCENT     = (200, 100, 255)   # purpura acento
# ────────────────────────────────────────────────────────────────────────────


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i]-a[i]) * t) for i in range(3))


def clamp(v):
    return max(0, min(255, int(v)))


def gradient_bg(draw, w, h, c1, c2):
    for y in range(h):
        t = y / (h - 1)
        c = lerp(c1, c2, t)
        draw.line([(0, y), (w, y)], fill=c)


def draw_glow_text(img, text, pos, font, text_color, glow_color, glow_r=6):
    """Dibuja texto con halo visible."""
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d     = ImageDraw.Draw(layer)
    # Capas de glow de afuera hacia adentro
    for r in range(glow_r, 0, -1):
        alpha = int(255 * (1 - (r / glow_r)) ** 1.2)
        col   = glow_color + (alpha,)
        for dx in (-r, 0, r):
            for dy in (-r, 0, r):
                if dx == 0 and dy == 0:
                    continue
                d.text((pos[0]+dx, pos[1]+dy), text, font=font, fill=col)
    # Texto solido encima
    d.text(pos, text, font=font, fill=text_color+(255,))
    img.alpha_composite(layer)


def make_frame(i):
    t   = i / FRAMES
    phi = 2 * math.pi * t

    pulse  = (math.sin(phi) + 1) / 2          # 0..1  lento
    pulse2 = (math.sin(phi * 2 + 0.8) + 1)/2  # doble frecuencia

    img  = Image.new("RGBA", (W, H))
    draw = ImageDraw.Draw(img)

    # ── Fondo: gradiente azul-purpura, NO negro puro ─────────────────────────
    bg_top = lerp(BG1, lerp(BG1, (30, 15, 80), pulse), 0.4)
    bg_bot = lerp(BG2, lerp(BG2, (10, 30, 90), pulse2), 0.4)
    gradient_bg(draw, W, H, bg_top, bg_bot)

    # ── Franja central mas clara (efecto vitral) ─────────────────────────────
    mid_bright = int(18 + 12 * pulse)
    for y in range(H):
        dist = abs(y - H//2) / (H//2)
        extra = int(mid_bright * (1 - dist))
        x_off = int((W * 0.15) + (W * 0.70) * ((t * 0.5 + y * 0.003) % 1.0))
        draw.line([(x_off, y), (min(x_off+80, W), y)],
                  fill=(clamp(bg_top[0]+extra), clamp(bg_top[1]+extra), clamp(bg_top[2]+extra)))

    # ── Borde doble ──────────────────────────────────────────────────────────
    border_pulse = lerp(BORDER_C, GOLD, pulse)
    draw.rectangle([0, 0, W-1, H-1], outline=border_pulse + (255,), width=2)
    draw.rectangle([3, 3, W-4, H-4], outline=ACCENT + (int(120 + 80*pulse2),), width=1)

    # ── Lineas decorativas sup/inf ───────────────────────────────────────────
    line_a = int(180 + 70 * pulse)
    draw.line([(15, 10), (W-15, 10)], fill=GOLD + (line_a,), width=1)
    draw.line([(15, H-11), (W-15, H-11)], fill=GOLD + (line_a,), width=1)

    # ── Rombos en extremos ───────────────────────────────────────────────────
    dia_col = lerp(GOLD, GOLD2, pulse)
    for cx in (12, W-12):
        cy = H // 2
        s  = int(5 + 2 * pulse)
        draw.polygon([(cx, cy-s),(cx+s,cy),(cx,cy+s),(cx-s,cy)],
                     fill=dia_col+(220,))

    # ── Particulas flotantes ─────────────────────────────────────────────────
    for k in range(14):
        px = int(W * 0.1 + W * 0.80 * ((k * 0.137 + t * 0.25) % 1.0))
        py = int(H * 0.15 + H * 0.70 *
                 abs(math.sin(math.pi*(t*1.5 + k*0.22))))
        br = abs(math.sin(math.pi*(t*2 + k*0.31)))
        pa = int(200 * br)
        pr = int(1 + 2 * br)
        if pa > 30:
            draw.ellipse([px-pr, py-pr, px+pr, py+pr], fill=WHITE+(pa,))

    # ── Fuentes ──────────────────────────────────────────────────────────────
    font_paths = [
        "C:/Windows/Fonts/impact.ttf",
        "C:/Windows/Fonts/ariblk.ttf",
        "C:/Windows/Fonts/arialbd.ttf",
        "C:/Windows/Fonts/arial.ttf",
    ]
    f_big = f_med = f_sm = None
    for fp in font_paths:
        if os.path.exists(fp):
            from PIL import ImageFont as IF
            f_big = IF.truetype(fp, 40)
            f_med = IF.truetype(fp, 16)
            f_sm  = IF.truetype(fp, 10)
            break
    if f_big is None:
        from PIL import ImageFont as IF
        f_big = f_med = f_sm = IF.load_default()

    # ── "L2" arriba centrado ─────────────────────────────────────────────────
    l2_col  = lerp(GOLD, GOLD2, pulse)
    l2_glow = lerp(GOLD, (255, 140, 0), pulse2)
    bbox    = draw.textbbox((0,0), "L2", font=f_med)
    tw      = bbox[2]-bbox[0]
    draw_glow_text(img, "L2", ((W-tw)//2, 12), f_med, l2_col, l2_glow, glow_r=5)

    # ── "ZONA" + "ZERO" centrado ─────────────────────────────────────────────
    zona_col  = lerp(BLUE,  BLUE2, pulse)
    zona_glow = lerp(BLUE, (0, 100, 255), pulse2)
    zero_col  = lerp(RED,   RED2,  pulse2)
    zero_glow = lerp(RED, (255, 60, 0), pulse)

    bz  = draw.textbbox((0,0), "ZONA ", font=f_big)
    tw_zona = bz[2]-bz[0]
    bze = draw.textbbox((0,0), "ZERO",  font=f_big)
    tw_zero = bze[2]-bze[0]
    total_w = tw_zona + tw_zero
    bh      = bz[3]-bz[1]
    cx      = (W - total_w) // 2
    cy      = (H - bh) // 2 + 6

    draw_glow_text(img, "ZONA ", (cx, cy),          f_big, zona_col, zona_glow, glow_r=8)
    draw_glow_text(img, "ZERO",  (cx+tw_zona, cy),  f_big, zero_col, zero_glow, glow_r=8)

    # ── Subtitulo ────────────────────────────────────────────────────────────
    sub     = "HIGH FIVE  *  SEASON I  *  PVP SERVER"
    sub_col = lerp((180, 180, 200), (220, 200, 120), pulse)
    bs      = draw.textbbox((0,0), sub, font=f_sm)
    tw_s    = bs[2]-bs[0]
    draw_glow_text(img, sub, ((W-tw_s)//2, H-18), f_sm,
                   sub_col, (150, 150, 200), glow_r=2)

    return img


def main():
    print(f"Generando {FRAMES} frames {W}x{H}px ...")
    frames = []
    for i in range(FRAMES):
        f = make_frame(i)
        # Convertir a paleta de 200 colores para GIF
        frames.append(f.convert("P", palette=Image.ADAPTIVE, colors=200))
        print(f"  frame {i+1:02d}/{FRAMES}", end="\r")

    print(f"\nGuardando GIF -> {GIF_OUT}")
    frames[0].save(
        GIF_OUT,
        save_all=True,
        append_images=frames[1:],
        optimize=False,
        loop=0,
        duration=DELAY,
        disposal=2,
    )

    print(f"Guardando PNG -> {PNG_OUT}")
    # Frame del "pico" de brillo para el PNG estatico
    best = make_frame(FRAMES // 4)
    best.convert("RGB").save(PNG_OUT, "PNG")

    print(f"\n[OK] GIF: {os.path.getsize(GIF_OUT)//1024} KB")
    print(f"[OK] PNG: {os.path.getsize(PNG_OUT)//1024} KB")


if __name__ == "__main__":
    main()
