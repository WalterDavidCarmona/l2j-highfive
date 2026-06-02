"""
inject_native_banner.py  v2
Banner compacto (fila unica ~30px) para paginas con sidebar (panel 555x455).
Uso: python inject_native_banner.py
"""
import os, re

BASE = os.path.dirname(os.path.abspath(__file__))

# Banner de UNA sola fila - ocupa ~30px, deja espacio al contenido
NATIVE_BANNER = """\t\t\t\t\t\t\t<!-- === BANNER L2 ZONA ZERO === -->
\t\t\t\t\t\t\t<tr>
\t\t\t\t\t\t\t\t<td align=center>
\t\t\t\t\t\t\t\t\t<table border=0 cellpadding=0 cellspacing=0 width=530 bgcolor="202040">
\t\t\t\t\t\t\t\t\t\t<tr>
\t\t\t\t\t\t\t\t\t\t\t<td width=44 align=center><img src="L2UI_CH3.herotower_deco" width=40 height=22></td>
\t\t\t\t\t\t\t\t\t\t\t<td align=center>
\t\t\t\t\t\t\t\t\t\t\t\t<font color="50B4FF" name="hs12">ZONA </font><font color="FF5028" name="hs12">ZERO</font>
\t\t\t\t\t\t\t\t\t\t\t\t&nbsp;&nbsp;<font color="7A8AAA">|</font>&nbsp;&nbsp;
\t\t\t\t\t\t\t\t\t\t\t\t<font color="C8A060">HIGH FIVE &nbsp;&middot;&nbsp; SEASON I &nbsp;&middot;&nbsp; PVP SERVER</font>
\t\t\t\t\t\t\t\t\t\t\t</td>
\t\t\t\t\t\t\t\t\t\t\t<td width=44 align=center><img src="L2UI_CH3.herotower_deco" width=40 height=22></td>
\t\t\t\t\t\t\t\t\t\t</tr>
\t\t\t\t\t\t\t\t\t</table>
\t\t\t\t\t\t\t\t</td>
\t\t\t\t\t\t\t</tr>
\t\t\t\t\t\t\t<tr><td height=4></td></tr>
\t\t\t\t\t\t\t<!-- === FIN BANNER === -->"""

FOOTER_OLD = "LINEAGE II - COMMUNITY BOARD"
FOOTER_NEW = "L2 ZONA ZERO &nbsp;&middot;&nbsp; HIGH FIVE &nbsp;&middot;&nbsp; SEASON I"

# Reemplazar cualquier version previa del banner (multi-row o single-row)
OLD_BANNER_BLOCK = re.compile(
    r'\t*<!-- === BANNER L2 ZONA ZERO === -->.*?<!-- === FIN BANNER === -->',
    re.DOTALL
)
# Patron fallback: img externo viejo
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

    if "BANNER L2 ZONA ZERO" in content:
        new_content = OLD_BANNER_BLOCK.sub(NATIVE_BANNER, content)
    else:
        new_content, n = OLD_IMG.subn(NATIVE_BANNER + "\n", content)
        if n == 0:
            print(f"[warn] patron no encontrado: {os.path.relpath(path, BASE)}")
            continue

    new_content = new_content.replace(FOOTER_OLD, FOOTER_NEW)

    with open(path, "w", encoding="utf-8") as f:
        f.write(new_content)

    print(f"[OK]  {os.path.relpath(path, BASE)}")

print("\nListo.")
