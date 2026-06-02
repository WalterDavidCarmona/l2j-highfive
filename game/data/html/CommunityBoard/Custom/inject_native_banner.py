"""
inject_native_banner.py
Reemplaza el <img> de banner externo por un banner HTML nativo compatible con L2J.
Uso: python inject_native_banner.py
"""
import os, re

BASE = os.path.dirname(os.path.abspath(__file__))

NATIVE_BANNER = """\t\t\t\t\t\t\t<!-- === BANNER L2 ZONA ZERO === -->
\t\t\t\t\t\t\t<tr>
\t\t\t\t\t\t\t\t<td align=center>
\t\t\t\t\t\t\t\t\t<table border=0 cellpadding=0 cellspacing=0 width=530 bgcolor="202040">
\t\t\t\t\t\t\t\t\t\t<tr><td height=2></td></tr>
\t\t\t\t\t\t\t\t\t\t<tr>
\t\t\t\t\t\t\t\t\t\t\t<td align=center>
\t\t\t\t\t\t\t\t\t\t\t\t<table border=0 cellpadding=0 cellspacing=2 width=528>
\t\t\t\t\t\t\t\t\t\t\t\t\t<tr>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td width=50 align=center valign=middle>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<img src="L2UI_CH3.herotower_deco" width=44 height=24>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t</td>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td align=center valign=middle>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<table border=0 cellpadding=0 cellspacing=0 width=400>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr><td height=4></td></tr>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr><td align=center><font color="4080FF" name="hs12">&#9670; L2 &#9670;</font></td></tr>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr><td align=center><font color="50B4FF" name="hs16">ZONA </font><font color="FF5028" name="hs16">ZERO</font></td></tr>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr><td height=2></td></tr>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr><td align=center><img src="L2UI.SquareGray" width=300 height=1></td></tr>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr><td height=2></td></tr>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr><td align=center><font color="C8A060">HIGH FIVE &nbsp;&middot;&nbsp; SEASON I &nbsp;&middot;&nbsp; PVP SERVER</font></td></tr>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr><td height=4></td></tr>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</table>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t</td>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td width=50 align=center valign=middle>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<img src="L2UI_CH3.herotower_deco" width=44 height=24>
\t\t\t\t\t\t\t\t\t\t\t\t\t\t</td>
\t\t\t\t\t\t\t\t\t\t\t\t\t</tr>
\t\t\t\t\t\t\t\t\t\t\t\t</table>
\t\t\t\t\t\t\t\t\t\t\t</td>
\t\t\t\t\t\t\t\t\t\t</tr>
\t\t\t\t\t\t\t\t\t\t<tr><td height=2></td></tr>
\t\t\t\t\t\t\t\t\t</table>
\t\t\t\t\t\t\t\t</td>
\t\t\t\t\t\t\t</tr>
\t\t\t\t\t\t\t<tr><td height=6></td></tr>
\t\t\t\t\t\t\t<!-- === FIN BANNER === -->"""

FOOTER_OLD = "LINEAGE II - COMMUNITY BOARD"
FOOTER_NEW = "L2 ZONA ZERO &nbsp;&middot;&nbsp; HIGH FIVE &nbsp;&middot;&nbsp; SEASON I"

# Patron: bloque del img externo (incluye lineas de spacer alrededor)
IMG_PATTERN = re.compile(
    r'\t*<!-- === BANNER L2 ZONA ZERO === -->.*?<!-- === FIN BANNER === -->',
    re.DOTALL
)
OLD_IMG = re.compile(
    r'\t*<tr>\s*<td align=center>\s*<img src="Custom/banner_zonazero\.gif"[^>]*>\s*</td>\s*</tr>\s*'
    r'<tr><td height=6></td></tr>',
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

    # Si ya tiene el banner nativo, reemplazarlo (para actualizar)
    if "BANNER L2 ZONA ZERO" in content:
        new_content = IMG_PATTERN.sub(NATIVE_BANNER, content)
    else:
        # Reemplazar el img externo por el banner nativo
        new_content, n = OLD_IMG.subn(NATIVE_BANNER + "\n", content)
        if n == 0:
            print(f"[warn] patron no encontrado: {os.path.relpath(path, BASE)}")
            continue

    new_content = new_content.replace(FOOTER_OLD, FOOTER_NEW)

    with open(path, "w", encoding="utf-8") as f:
        f.write(new_content)

    print(f"[OK]  {os.path.relpath(path, BASE)}")

print("\nListo. Recarga el Community Board en el juego (Alt+B).")
