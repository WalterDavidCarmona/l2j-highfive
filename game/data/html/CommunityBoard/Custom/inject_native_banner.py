"""
remove_banner.py
Elimina el bloque del banner L2 Zona Zero de todos los HTMLs del Community Board.
Uso: python inject_native_banner.py
"""
import os, re

BASE = os.path.dirname(os.path.abspath(__file__))

BANNER_BLOCK = re.compile(
    r'\t*<!-- === BANNER L2 ZONA ZERO === -->.*?<!-- === FIN BANNER === -->\n?',
    re.DOTALL
)

FILES = [
    os.path.join(BASE, "buffer",    "main.html"),
    os.path.join(BASE, "gatekeeper","main.html"),
    os.path.join(BASE, "merchant",  "main.html"),
    os.path.join(BASE, "dropsearch","main.html"),
    os.path.join(BASE, "delevel",   "main.html"),
    os.path.join(BASE, "premium",   "main.html"),
]

for path in FILES:
    if not os.path.exists(path):
        print(f"[miss] {path}")
        continue

    with open(path, "r", encoding="utf-8") as f:
        content = f.read()

    if "BANNER L2 ZONA ZERO" not in content:
        print(f"[skip] sin banner: {os.path.relpath(path, BASE)}")
        continue

    new_content = BANNER_BLOCK.sub("", content)

    with open(path, "w", encoding="utf-8") as f:
        f.write(new_content)

    print(f"[OK]  {os.path.relpath(path, BASE)}")

print("\nListo.")
