"""
generate_banner.py
Genera el banner animado GIF de L2 Zona Zero para los HTMLs del Community Board.
Requiere: pip install pillow
Uso: python generate_banner.py
"""

from PIL import Image, ImageDraw, ImageFont, ImageFilter
import math
import os

# ── Configuracion ──────────────────────────────────────────────────────────────
OUTPUT_DIR   = os.path.dirname(os.path.abspath(__file__))
GIF_OUT      = os.path.join(OUTPUT_DIR, "banner_zonazero.gif")
PNG_OUT      = os.path.join(OUTPUT_DIR, "banner_zonazero.png")

W, H         = 560, 110          # ancho x alto (ajustado al CB de L2J ~560px)
FRAMES       = 24                # cantidad de frames del GIF
FRAME_DELAY  = 60                # ms por frame  (24 frames × 60ms ≈ loop 1.4s)

# Paleta de colores del logo L2 Zona Zero
COL_BG_TOP   = (5,   8,  30)     # azul oscuro casi negro
COL_BG_BOT   = (10, 12,  50)     # azul medianoche
COL_GOLD     = (220, 160,  40)   # dorado del "L2"
COL_BLUE     = ( 40, 140, 255)   # azul brillante "ZONA"
COL_RED      = (220,  50,  30)   # rojo/naranja "ZERO"
COL_GLOW_B   = ( 60, 180, 255)   # halo azul
COL_GLOW_P   = (150,  60, 255)   # halo purpura
COL_SPARK    = (255, 255, 255)   # destellos blancos
# ──────────────────────────────────────────────────────────────────────────────


def lerp_color(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def add_colors(a, b, alpha=1.0):
    return tuple(min(255, int(a[i] + b[i] * alpha)) for i in range(3))


def gradient_bg(draw, w, h, c_top, c_bot):
    for y in range(h):
        t = y / h
        c = lerp_color(c_top, c_bot, t)
        draw.line([(0, y), (w, y)], fill=c)


def glow_rect(img, x, y, w, h, color, radius=8, alpha=0.5):
    """Dibuja un rectangulo con halo difuso."""
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    draw  = ImageDraw.Draw(layer)
    for r in range(radius, 0, -1):
        a = int(255 * alpha * (1 - r / radius) ** 2)
        c = color + (a,)
        draw.rectangle([x - r, y - r, x + w + r, y + h + r], outline=c)
    img.alpha_composite(layer)


def draw_text_glow(draw, img, text, xy, color, glow_color, font, glow_r=4):
    """Texto con halo difuso."""
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ld    = ImageDraw.Draw(layer)
    for r in range(glow_r, 0, -1):
        a = int(200 * (1 - r / glow_r) ** 1.5)
        for dx in range(-r, r + 1):
            for dy in range(-r, r + 1):
                if abs(dx) == r or abs(dy) == r:
                    ld.text((xy[0] + dx, xy[1] + dy), text,
                             font=font, fill=glow_color + (a,))
    ld.text(xy, text, font=font, fill=color + (255,))
    img.alpha_composite(layer)


def spark_positions(frame, n=12):
    """Genera posiciones de destellos que se mueven con el tiempo."""
    sparks = []
    for i in range(n):
        phase = (frame / FRAMES + i / n) % 1.0
        x = int(W * 0.15 + (W * 0.70) * ((i * 0.618 + phase * 0.3) % 1.0))
        y = int(H * 0.1  + H * 0.80 * abs(math.sin(math.pi * (phase + i * 0.17))))
        brightness = abs(math.sin(math.pi * (phase * 3 + i * 0.4)))
        sparks.append((x, y, brightness))
    return sparks


def make_frame(frame_idx):
    t   = frame_idx / FRAMES                           # 0.0 → 1.0
    phi = 2 * math.pi * t                              # fase angular

    # Pulso: varía entre 0 y 1 suavemente
    pulse  = (math.sin(phi) + 1) / 2                  # 0..1
    pulse2 = (math.sin(phi * 2 + 1) + 1) / 2          # frecuencia doble

    img  = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    draw = ImageDraw.Draw(img)

    # ── Fondo gradiente ────────────────────────────────────────────────────────
    gradient_bg(draw, W, H, COL_BG_TOP, COL_BG_BOT)

    # ── Lineas de escaneo horizontales (efecto tech) ───────────────────────────
    for y in range(0, H, 4):
        a = int(15 + 8 * math.sin(phi + y * 0.05))
        draw.line([(0, y), (W, y)], fill=(80, 120, 255, a))

    # ── Borde exterior con glow pulsante ──────────────────────────────────────
    border_a = int(120 + 100 * pulse)
    glow_rect(img, 2, 2, W - 4, H - 4,
              lerp_color(COL_GLOW_B, COL_GLOW_P, pulse),
              radius=6, alpha=0.6 + 0.3 * pulse)

    # Borde fino solido
    draw = ImageDraw.Draw(img)
    border_col = lerp_color(COL_BLUE, COL_GOLD, pulse)
    draw.rectangle([2, 2, W - 3, H - 3], outline=border_col + (border_a,))
    draw.rectangle([4, 4, W - 5, H - 5], outline=border_col + (60,))

    # ── Linea decorativa superior e inferior ──────────────────────────────────
    line_a = int(160 + 80 * pulse2)
    draw.line([(20, 8),  (W - 20, 8)],  fill=COL_GOLD + (line_a,), width=1)
    draw.line([(20, H - 9), (W - 20, H - 9)], fill=COL_GOLD + (line_a,), width=1)

    # ── Rombos decorativos en esquinas ────────────────────────────────────────
    diamond_a = int(180 + 60 * pulse)
    diamond_c = lerp_color(COL_BLUE, COL_GOLD, pulse2)
    for cx, cy in [(20, H // 2), (W - 20, H // 2)]:
        s = 5
        draw.polygon([(cx, cy - s), (cx + s, cy),
                       (cx, cy + s), (cx - s, cy)],
                      fill=diamond_c + (diamond_a,))

    # ── Destellos de particulas ────────────────────────────────────────────────
    for sx, sy, br in spark_positions(frame_idx):
        sa = int(220 * br)
        sr = int(1 + 2 * br)
        draw.ellipse([sx - sr, sy - sr, sx + sr, sy + sr],
                      fill=COL_SPARK + (sa,))

    # ── Intentar cargar fuentes del sistema ───────────────────────────────────
    font_big  = None
    font_med  = None
    font_sm   = None
    font_paths = [
        "C:/Windows/Fonts/arialbd.ttf",
        "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/impact.ttf",
        "C:/Windows/Fonts/trebucbd.ttf",
    ]
    for fp in font_paths:
        if os.path.exists(fp):
            try:
                font_big = ImageFont.truetype(fp, 36)
                font_med = ImageFont.truetype(fp, 18)
                font_sm  = ImageFont.truetype(fp, 11)
                break
            except Exception:
                pass
    if font_big is None:
        font_big = font_med = font_sm = ImageFont.load_default()

    # ── Texto "L2" pequeño ────────────────────────────────────────────────────
    l2_col   = lerp_color(COL_GOLD, (255, 220, 100), pulse)
    l2_glow  = lerp_color(COL_GOLD, (255, 180,  40), pulse2)

    # Bounding box del texto principal para centrar
    txt_main = "ZONA ZERO"
    bbox_main = draw.textbbox((0, 0), txt_main, font=font_big)
    tw_main   = bbox_main[2] - bbox_main[0]
    th_main   = bbox_main[3] - bbox_main[1]
    cx_main   = (W - tw_main) // 2
    cy_main   = (H - th_main) // 2 + 4

    # "L2" encima del texto principal
    txt_l2   = "L2"
    bbox_l2  = draw.textbbox((0, 0), txt_l2, font=font_med)
    tw_l2    = bbox_l2[2] - bbox_l2[0]
    cx_l2    = (W - tw_l2) // 2
    cy_l2    = cy_main - th_main + 2

    draw_text_glow(draw, img, txt_l2,
                   (cx_l2, cy_l2),
                   l2_col, l2_glow, font_med, glow_r=5)

    # ── Texto principal "ZONA ZERO" ──────────────────────────────────────────
    # "ZONA" en azul, "ZERO" en rojo-naranja
    txt_zona = "ZONA "
    bbox_z   = draw.textbbox((0, 0), txt_zona, font=font_big)
    tw_zona  = bbox_z[2] - bbox_z[0]

    zona_col  = lerp_color(COL_BLUE,  (100, 200, 255), pulse)
    zona_glow = lerp_color(COL_GLOW_B, COL_BLUE,       pulse2)
    zero_col  = lerp_color(COL_RED,   (255, 120,  50), pulse2)
    zero_glow = lerp_color((180, 30, 10), COL_RED,     pulse)

    draw_text_glow(draw, img, txt_zona,
                   (cx_main, cy_main),
                   zona_col, zona_glow, font_big, glow_r=7)

    draw_text_glow(draw, img, "ZERO",
                   (cx_main + tw_zona, cy_main),
                   zero_col, zero_glow, font_big, glow_r=7)

    # ── Subtitulo ─────────────────────────────────────────────────────────────
    txt_sub  = "HIGH FIVE  ·  SEASON I  ·  PVP SERVER"
    bbox_sub = draw.textbbox((0, 0), txt_sub, font=font_sm)
    tw_sub   = bbox_sub[2] - bbox_sub[0]
    sub_col  = lerp_color((120, 120, 140), (180, 160, 100), pulse)
    draw.text(((W - tw_sub) // 2, H - 20), txt_sub,
               font=font_sm, fill=sub_col + (200,))

    return img.convert("RGBA")


def main():
    print(f"[*] Generando {FRAMES} frames ({W}x{H}px)...")
    frames = []
    for i in range(FRAMES):
        f = make_frame(i)
        frames.append(f.convert("P", palette=Image.ADAPTIVE, colors=128))
        print(f"    frame {i+1:02d}/{FRAMES}", end="\r")

    print(f"\n[*] Guardando GIF animado -> {GIF_OUT}")
    frames[0].save(
        GIF_OUT,
        save_all=True,
        append_images=frames[1:],
        optimize=True,
        loop=0,                   # loop infinito
        duration=FRAME_DELAY,
        disposal=2,
    )

    print(f"[*] Guardando PNG estatico  -> {PNG_OUT}")
    best = make_frame(0)          # frame 0 = estado "tranquilo"
    best.convert("RGB").save(PNG_OUT, "PNG", optimize=True)

    gif_size = os.path.getsize(GIF_OUT) / 1024
    png_size = os.path.getsize(PNG_OUT) / 1024
    print(f"\n[OK] GIF: {gif_size:.1f} KB   PNG: {png_size:.1f} KB")
    print("[OK] Listo. Coloca banner_zonazero.gif/png en:")
    print("     game/data/html/CommunityBoard/Custom/")


if __name__ == "__main__":
    main()
