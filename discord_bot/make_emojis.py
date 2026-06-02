"""
Convierte los stickers a tamaño de emoji Discord (128x128) y los guarda en /emojis/
Uso: python make_emojis.py
"""
from PIL import Image
import os

STICKER_DIR = os.path.join(os.path.dirname(__file__), "stickers")
EMOJI_DIR   = os.path.join(os.path.dirname(__file__), "emojis")
os.makedirs(EMOJI_DIR, exist_ok=True)

for filename in os.listdir(STICKER_DIR):
    if not filename.endswith(".png"):
        continue
    src = os.path.join(STICKER_DIR, filename)
    img = Image.open(src).convert("RGBA")
    img.thumbnail((128, 128), Image.LANCZOS)
    canvas = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    offset = ((128 - img.width) // 2, (128 - img.height) // 2)
    canvas.paste(img, offset, img)
    out = os.path.join(EMOJI_DIR, filename)
    canvas.save(out, "PNG", optimize=True)
    size_kb = os.path.getsize(out) / 1024
    print(f"OK {filename} ({size_kb:.1f} KB)")

print(f"\nListo. Emojis guardados en: {EMOJI_DIR}")
