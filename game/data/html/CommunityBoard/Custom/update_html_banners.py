"""
update_html_banners.py
Inyecta el banner animado en todos los HTMLs del Community Board custom.
Uso: python update_html_banners.py
"""

import os
import re

BASE = os.path.dirname(os.path.abspath(__file__))

# Archivos a modificar y su ruta relativa al banner
HTML_FILES = {
    os.path.join(BASE, "home.html"):                       "Custom/banner_zonazero.gif",
    os.path.join(BASE, "buffer",    "main.html"):          "Custom/banner_zonazero.gif",
    os.path.join(BASE, "gatekeeper","main.html"):          "Custom/banner_zonazero.gif",
    os.path.join(BASE, "merchant",  "main.html"):          "Custom/banner_zonazero.gif",
    os.path.join(BASE, "dropsearch","main.html"):          "Custom/banner_zonazero.gif",
    os.path.join(BASE, "delevel",   "main.html"):          "Custom/banner_zonazero.gif",
    os.path.join(BASE, "premium",   "main.html"):          "Custom/banner_zonazero.gif",
}

BANNER_HTML = """\t\t\t\t\t\t\t<tr>
\t\t\t\t\t\t\t\t<td align=center>
\t\t\t\t\t\t\t\t\t<img src="{src}" width=540 height=110>
\t\t\t\t\t\t\t\t</td>
\t\t\t\t\t\t\t</tr>
\t\t\t\t\t\t\t<tr><td height=6></td></tr>"""

# Patron: primera <tr> dentro del panel principal (el spacer de height=25 al inicio)
PATTERN = re.compile(
    r'(<table[^>]*background="L2UI_CT1\.Windows_DF_TooltipBG"[^>]*>)\s*'
    r'(<tr>\s*<td height=25></td>\s*</tr>)',
    re.DOTALL
)

FOOTER_OLD = '<font color=696969>LINEAGE II - COMMUNITY BOARD</font>'
FOOTER_NEW = '<font color=696969>L2 ZONA ZERO &nbsp;·&nbsp; HIGH FIVE &nbsp;·&nbsp; SEASON I</font>'


def already_patched(content):
    return "banner_zonazero.gif" in content


def patch_file(path, banner_src):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()

    if already_patched(content):
        print(f"  [skip] ya tiene banner: {os.path.basename(os.path.dirname(path))}/{os.path.basename(path)}")
        return False

    banner_row = BANNER_HTML.format(src=banner_src)

    def replacer(m):
        table_open = m.group(1)
        spacer_row = m.group(2)
        return f"{table_open}\n{banner_row}\n\t\t\t\t\t\t\t<tr><td height=8></td></tr>\n{spacer_row}"

    new_content, count = re.subn(PATTERN, replacer, content, count=1)

    if count == 0:
        # Fallback: insertar despues de la primera tabla principal si el patron no matchea
        print(f"  [warn] patron no encontrado en {os.path.basename(path)}, usando fallback")
        new_content = content

    # Actualizar footer
    new_content = new_content.replace(FOOTER_OLD, FOOTER_NEW)

    with open(path, "w", encoding="utf-8") as f:
        f.write(new_content)

    label = os.path.relpath(path, BASE)
    print(f"  [OK]   {label}")
    return True


def main():
    print("[*] Inyectando banner en HTMLs del Community Board...\n")
    updated = 0
    for path, src in HTML_FILES.items():
        if not os.path.exists(path):
            print(f"  [miss] no existe: {path}")
            continue
        if patch_file(path, src):
            updated += 1

    print(f"\n[OK] {updated} archivo(s) actualizados.")
    print("[!]  Reinicia el servidor para ver los cambios en el juego.")


if __name__ == "__main__":
    main()
