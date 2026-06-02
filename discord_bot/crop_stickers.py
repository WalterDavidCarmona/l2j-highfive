"""
Recorta los personajes del sticker sheet de L2 y elimina el fondo blanco.
Uso: python crop_stickers.py
Genera PNGs en la carpeta discord_bot/stickers/
"""

from PIL import Image, ImageOps
import os

STICKER_DIR = os.path.join(os.path.dirname(__file__), "stickers")
os.makedirs(STICKER_DIR, exist_ok=True)

SHEET_PATH = r"C:\Users\david\OneDrive\Desktop\stickers\maggie-fess-2xj1f66khj0.jpg"
SINGLE_PATHS = [
    (r"C:\Users\david\OneDrive\Desktop\stickers\maggie-fess-.jpg", "dark_elf_female"),
    (r"C:\Users\david\OneDrive\Desktop\stickers\maggie-fess-vuit-b34suy.jpg", "dark_elf_male"),
]

# Coordenadas manuales de cada personaje en el sheet (x1, y1, x2, y2)
# Basadas en imagen de ~1280x800px aprox — ajustadas visualmente
CROPS = [
    # Fila 1 (y: 0 - 203)
    ("l2_tyrr_warrior",        (10,   0,  215, 203)),
    ("l2_necromancer",         (205,  0,  395, 203)),
    ("l2_dual_sword",          (385,  0,  590, 203)),
    ("l2_iss_enchanter",       (575,  0,  770, 203)),
    ("l2_wynn_summoner",       (760,  0,  950, 203)),
    ("l2_aeore_healer",        (940,  0, 1130, 203)),
    ("l2_feoh_wizard",         (1115, 0, 1280, 203)),
    # Fila 2 (y: 203 - 406)
    ("l2_shillien_knight",     (10,  203,  210, 406)),
    ("l2_yul_archer",          (195, 203,  400, 406)),
    ("l2_evas_saint",          (385, 203,  590, 406)),
    ("l2_iss_summoner",        (575, 203,  760, 406)),
    ("l2_othell_rogue",        (745, 203,  945, 406)),
    ("l2_bishop",              (930, 203, 1120, 406)),
    ("l2_maestro",             (1105,203, 1280, 406)),
    # Fila 3 (y: 406 - 609)
    ("l2_orc_destroyer",       (10,  406,  215, 609)),
    ("l2_dwarf_bounty",        (200, 406,  395, 609)),
    ("l2_unicorn",             (380, 406,  590, 609)),
    ("l2_iss_hierophant",      (575, 406,  770, 609)),
    ("l2_warlord",             (755, 406,  950, 609)),
    ("l2_dark_summoner",       (935, 406, 1125, 609)),
    ("l2_kamael",              (1110,406, 1280, 609)),
]


def remove_white_bg(img: Image.Image, threshold: int = 240) -> Image.Image:
    img = img.convert("RGBA")
    pixels = list(img.getdata())
    new_data = []
    for r, g, b, a in pixels:
        if r >= threshold and g >= threshold and b >= threshold:
            new_data.append((255, 255, 255, 0))
        else:
            new_data.append((r, g, b, a))
    img.putdata(new_data)
    return img


def autocrop(img: Image.Image) -> Image.Image:
    bbox = img.getbbox()
    if bbox:
        img = img.crop(bbox)
    return img


def save_sticker(img: Image.Image, name: str):
    img = remove_white_bg(img)
    img = autocrop(img)
    # Redimensionar a 320x320 manteniendo proporción
    img.thumbnail((320, 320), Image.LANCZOS)
    canvas = Image.new("RGBA", (320, 320), (0, 0, 0, 0))
    offset = ((320 - img.width) // 2, (320 - img.height) // 2)
    canvas.paste(img, offset, img)
    out_path = os.path.join(STICKER_DIR, f"{name}.png")
    canvas.save(out_path, "PNG", optimize=True)
    print(f"  OK {name}.png ({canvas.width}x{canvas.height})")
    return out_path


print("=== Procesando sticker sheet ===")
sheet = Image.open(SHEET_PATH).convert("RGB")
w, h = sheet.size
print(f"Tamaño del sheet: {w}x{h}px")

# Escalar coordenadas si el sheet tiene tamaño diferente al esperado
BASE_W, BASE_H = 1280, 609
scale_x = w / BASE_W
scale_y = h / BASE_H

for name, (x1, y1, x2, y2) in CROPS:
    sx1 = int(x1 * scale_x)
    sy1 = int(y1 * scale_y)
    sx2 = int(x2 * scale_x)
    sy2 = int(y2 * scale_y)
    cropped = sheet.crop((sx1, sy1, sx2, sy2))
    save_sticker(cropped, name)

print("\n=== Procesando imágenes individuales ===")
for path, name in SINGLE_PATHS:
    img = Image.open(path).convert("RGB")
    save_sticker(img, name)

print(f"\nListo. {len(CROPS) + len(SINGLE_PATHS)} stickers guardados en:")
print(f"   {STICKER_DIR}")
