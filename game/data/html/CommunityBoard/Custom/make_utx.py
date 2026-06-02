"""
make_utx.py  v3 - Generador UTX con formato CORRECTO para L2 After Crows H5
Clona la estructura exacta de Crest.utx (version 123 / licensee 37)
y la adapta para el banner de L2 Zona Zero.

Encoding compact int CORRECTO (verificado en Crest.utx):
    bit7 = SIGNO
    bit6 = CONTINUACION
    bits0-5 = valor (primer byte)

Uso: python make_utx.py
"""

import struct, os, hashlib
from PIL import Image

# ── Rutas ─────────────────────────────────────────────────────────────────────
SCRIPT_DIR   = os.path.dirname(os.path.abspath(__file__))
LOGO_IN      = os.path.join(SCRIPT_DIR, "logo_l2zonazero.png")
CREST_REF    = "C:/L2 Liberty Client + Parch - copia/systextures/Crest.utx"
CLIENT_DIR   = "C:/Users/david/Downloads/Lineage 2 H5/Lineage 2 H5/L2   (HighFive) Cliente limpio/L2 After Crows  (HighFive)/SysTextures"
UTX_NAME     = "CustomUI.utx"
UTX_OUT      = os.path.join(CLIENT_DIR, UTX_NAME)
UTX_BACKUP   = os.path.join(SCRIPT_DIR,  UTX_NAME)
PKG_NAME     = "CustomUI"
TEX_NAME     = "BannerZonaZero"

# Encriptacion del cliente L2 After Crows
L2_HEADER    = "Lineage2Ver121".encode("utf-16-le")   # 28 bytes
L2_XOR       = sum(ord(c) for c in UTX_NAME.lower()) % 256  # clave XOR derivada del nombre

# Formato de textura: copiamos DXT1 (format=3) de Crest.utx
# pero usamos RGBA8 (format=5) para simplificar la generacion de pixel data
TEXF_RGBA8   = 5
# ─────────────────────────────────────────────────────────────────────────────


# ── Compact int CORRECTO (bit7=SIGN, bit6=CONTINUATION) ──────────────────────
def rci(data, pos):
    """Lee compact int con encoding correcto de L2 H5."""
    b = data[pos]; pos += 1
    v = b & 0x3F
    neg = bool(b & 0x80)
    sh = 6
    while b & 0x40:
        b = data[pos]; pos += 1
        v |= (b & 0x3F) << sh
        sh += 6
    return (-v if neg else v), pos


def wci(val):
    """Escribe compact int con encoding correcto."""
    neg = val < 0
    val = abs(val)
    out = bytearray()
    b = val & 0x3F
    if neg:  b |= 0x80      # bit7 = SIGN
    val >>= 6
    if val:  b |= 0x40      # bit6 = CONTINUATION
    out.append(b)
    while val:
        b = val & 0x3F
        val >>= 6
        if val:  b |= 0x40
        out.append(b)
    return bytes(out)


# ── Analizar Crest.utx de referencia ─────────────────────────────────────────
def read_crest_ref(path):
    """Lee el Crest.utx de Liberty Client y extrae info del primer objeto."""
    with open(path, "rb") as f:
        dec = f.read()

    nc, noff = struct.unpack_from("<II", dec, 12)
    ec, eoff = struct.unpack_from("<II", dec, 20)
    ic, ioff = struct.unpack_from("<II", dec, 28)

    # Name table
    pos = noff
    names = []
    for i in range(nc):
        slen = dec[pos]; pos += 1
        name = dec[pos:pos+slen-1].decode("latin-1", "replace")
        pos += slen + 4
        names.append(name)

    # Export[0] - sin name_num field (verificado)
    pos = eoff
    ci, pos = rci(dec, pos)
    si, pos = rci(dec, pos)
    oi = struct.unpack_from("<i", dec, pos)[0]; pos += 4
    ni, pos = rci(dec, pos)
    fl = struct.unpack_from("<I", dec, pos)[0]; pos += 4  # flags (NO name_num)
    ssz, pos = rci(dec, pos)
    sof, pos = rci(dec, pos)

    obj_template = dec[sof:sof+ssz]
    print(f"  Crest.utx ref: export[0] name={names[ni]} size={ssz} offset={sof}")
    print(f"  Flags: 0x{fl:08X}")
    print(f"  Import table (raw 14 bytes): {dec[ioff:ioff+14].hex(' ')}")

    return {
        "names":        names,
        "obj_template": obj_template,  # binary del objeto textura de referencia
        "flags":        fl,
        "import_raw":   dec[ioff:ioff+14],   # 2 imports (7 bytes cada uno)
    }


# ── Generar pixel data RGBA8 del logo ────────────────────────────────────────
def make_rgba8(logo_path, W=256, H=256):
    img = Image.open(logo_path).convert("RGBA")
    print(f"  Logo: {img.size} -> {W}x{H} RGBA8")
    img = img.resize((W, H), Image.LANCZOS)
    # L2 usa BGRA internamente
    r, g, b, a = img.split()
    bgra = Image.merge("RGBA", (b, g, r, a))
    return bgra.tobytes()


# ── Construir objeto textura RGBA8 ────────────────────────────────────────────
# Estructura verificada de Crest.utx (L2 Liberty Client, v123):
#   [47 bytes propiedades] [1083 zeros UTexture base] [1 NumMips] [4 SkipOff]
#   [CI DataCount] [pixels] [4 USize] [4 VSize] [1 UBits] [1 VBits]
#
# Los 1083 zeros son la serializacion de UTexture->UBitmapMaterial->UMaterial
# (FShaderProperty, FMaterialStageProperty, etc.) cuando estan vacios.
UTEXTURE_BASE_ZEROS = 1083

def build_texture_obj(W, H, px_bgra, names_map):
    """Construye el objeto textura con formato verificado de L2 H5."""
    import math
    ubits = int(math.log2(W))
    vbits = int(math.log2(H))
    n = names_map

    obj = bytearray()

    # ── 1. Propiedades tagged (47 bytes, igual que Crest.utx) ─────────────────
    # InternalTime[0] - INT
    obj += wci(n["InternalTime"])
    obj += bytes([0x22])
    obj += struct.pack("<I", 0x00000000)

    # InternalTime[1] - INT, array element
    obj += wci(n["InternalTime"])
    obj += bytes([0xA2])                   # 0x22 | 0x80 = array flag
    obj += bytes([0x01])                   # index = 1
    obj += struct.pack("<I", 0x00000000)

    # Format - BYTE
    obj += wci(n["Format"])
    obj += bytes([0x01])
    obj += struct.pack("<B", TEXF_RGBA8)

    # UBits - BYTE
    obj += wci(n["UBits"])
    obj += bytes([0x01])
    obj += struct.pack("<B", ubits)

    # VBits - BYTE
    obj += wci(n["VBits"])
    obj += bytes([0x01])
    obj += struct.pack("<B", vbits)

    # USize - INT
    obj += wci(n["USize"])
    obj += bytes([0x22])
    obj += struct.pack("<I", W)

    # VSize - INT
    obj += wci(n["VSize"])
    obj += bytes([0x22])
    obj += struct.pack("<I", H)

    # UClamp - INT
    obj += wci(n["UClamp"])
    obj += bytes([0x22])
    obj += struct.pack("<I", W)

    # VClamp - INT
    obj += wci(n["VClamp"])
    obj += bytes([0x22])
    obj += struct.pack("<I", H)

    # None - fin de propiedades
    obj += wci(n["None"])

    prop_size = len(obj)
    print(f"    Properties: {prop_size} bytes")

    # ── 2. UTexture base class serialization ─────────────────────────────────
    # Verificado en ColorSelection.utx (After Crows, lic=37):
    #   byte 0: Palette reference (compact int 0 = null)
    #   byte 1: StaticPermutations/MaterialInfo array count (CI 0 = empty)
    #   byte 2: ShaderProperties array count (CI 0 = empty)
    # Para textura sin shader = 3 bytes: [00 00 00]
    obj += bytes([0x00, 0x00, 0x00])

    # ── 3. Mipmap section ─────────────────────────────────────────────────────
    mip_section_start = len(obj)

    # NumMips (compact int = 1)
    obj += wci(1)

    # SkipOffset placeholder (INT32) - parchear despues
    skip_pos = len(obj)
    obj += struct.pack("<I", 0)            # placeholder

    # DataCount (compact int)
    pixel_count = len(px_bgra)
    count_ci = wci(pixel_count)
    obj += count_ci

    # Pixel data
    obj += px_bgra

    # USize, VSize, UBits, VBits del mip
    obj += struct.pack("<I", W)
    obj += struct.pack("<I", H)
    obj += struct.pack("<B", ubits)
    obj += struct.pack("<B", vbits)

    print(f"    UTexture base: 3 bytes (palette=null, 2x empty array)")
    print(f"    Mipmap pixel data: {pixel_count} bytes")
    print(f"    Total object: {len(obj)} bytes")

    return bytes(obj), skip_pos


# ── Tabla de nombres ──────────────────────────────────────────────────────────
NAMES_LIST = [
    "InternalTime", "USize", "VSize", "UClamp", "VBits", "VClamp",
    "None", "UBits", "Format",
    "Package", "Engine", "Core", "Class", "Texture",
    PKG_NAME, TEX_NAME,
]

# Flags por nombre (de Crest.utx: 0x00070010 para la mayoria)
NAME_FLAGS = {
    "None":    0x04070410,
    "Class":   0x04070410,
    "Package": 0x04070410,
    "Core":    0x04070010,
    "Engine":  0x04070010,
    "Texture": 0x04070010,
}
DEFAULT_FLAG = 0x00070010


def build_name_table(names):
    data = bytearray()
    for name in names:
        enc = (name + "\0").encode("latin-1")
        data += struct.pack("<B", len(enc))
        data += enc
        data += struct.pack("<I", NAME_FLAGS.get(name, DEFAULT_FLAG))
    return bytes(data)


def build_import_table(n):
    """
    Import[0]: Core.Class -> Texture (outer=-2 = import#2)
    Import[1]: Core.Package -> Engine (outer=0)
    """
    data = bytearray()
    # Import[0]: Engine.Texture class
    data += wci(n["Core"])
    data += wci(n["Class"])
    data += struct.pack("<i", -2)
    data += wci(n["Texture"])
    # Import[1]: Engine package
    data += wci(n["Core"])
    data += wci(n["Package"])
    data += struct.pack("<i", 0)
    data += wci(n["Engine"])
    return bytes(data)


def build_export_entry(n, tex_name, flags, serial_size, serial_offset):
    """Sin name_num field (verificado en Crest.utx)."""
    data = bytearray()
    data += wci(-1)                         # class = import#1 (Texture)
    data += wci(0)                          # super = 0
    data += struct.pack("<i", 0)            # outer = 0
    data += wci(n[tex_name])               # name
    data += struct.pack("<I", flags)        # flags (4 bytes, SIN name_num)
    data += wci(serial_size)               # serial_size
    data += wci(serial_offset)             # serial_offset
    return bytes(data)


# ── Encriptacion ──────────────────────────────────────────────────────────────
def encrypt_utx(raw):
    return L2_HEADER + bytes(b ^ L2_XOR for b in raw)


# ── Ensamblar UTX ─────────────────────────────────────────────────────────────
def build_utx(tex_obj, tex_obj_skip_pos, tex_name):
    names    = NAMES_LIST
    nm = {n: i for i, n in enumerate(names)}

    name_table   = build_name_table(names)
    import_table = build_import_table(nm)

    # Calcular offsets:
    HEADER_SIZE   = 64
    name_offset   = HEADER_SIZE
    tex_obj_start = name_offset + len(name_table)

    # Parchear SkipOffset dentro del objeto textura
    # SkipOffset = posicion absoluta en el archivo justo despues del pixel data
    # Verificado en Crest.utx: obj_start + prop + zeros + 1(nummips) + 4(skip) + CI(count) + pixels
    # = tex_obj_start + len(tex_obj) - 10 (4+4+1+1 = USize+VSize+UBits+VBits)
    tex_obj = bytearray(tex_obj)
    abs_skip = tex_obj_start + len(tex_obj) - 10
    struct.pack_into("<I", tex_obj, tex_obj_skip_pos, abs_skip)
    print(f"  SkipOffset patched: {abs_skip}")
    tex_obj = bytes(tex_obj)

    import_offset = tex_obj_start + len(tex_obj)
    export_offset = import_offset + len(import_table)

    # Export object flags - de Crest.utx verificado: 0x040F0004
    export_flags  = 0x040F0004

    export_entry  = build_export_entry(
        nm, tex_name, export_flags,
        len(tex_obj), tex_obj_start
    )

    # GUID determinista
    guid = hashlib.md5((PKG_NAME + tex_name).encode()).digest()

    # Header (64 bytes)
    hdr = bytearray()
    hdr += struct.pack("<I", 0x9E2A83C1)    # signature
    hdr += struct.pack("<H", 123)            # version
    hdr += struct.pack("<H", 37)             # licensee (L2 After Crows)
    hdr += struct.pack("<I", 0x00000001)     # flags
    hdr += struct.pack("<I", len(names))     # name_count
    hdr += struct.pack("<I", name_offset)    # name_offset
    hdr += struct.pack("<I", 1)              # export_count
    hdr += struct.pack("<I", export_offset)  # export_offset
    hdr += struct.pack("<I", 2)              # import_count
    hdr += struct.pack("<I", import_offset)  # import_offset
    hdr += guid                              # GUID 16 bytes
    hdr += struct.pack("<I", 1)              # generation_count
    hdr += struct.pack("<I", 1)              # gen_export_count
    hdr += struct.pack("<I", len(names))     # gen_name_count
    assert len(hdr) == HEADER_SIZE

    return bytes(hdr) + name_table + tex_obj + import_table + export_entry


# ── Main ──────────────────────────────────────────────────────────────────────
def main():
    W, H = 256, 256   # power-of-2, razonable para el banner

    print("[*] Leyendo referencia Crest.utx ...")
    if os.path.exists(CREST_REF):
        ref = read_crest_ref(CREST_REF)
    else:
        print("  [warn] Crest.utx de referencia no encontrado, continuando sin el")
        ref = {}

    print("[*] Cargando logo ...")
    px = make_rgba8(LOGO_IN, W, H)

    print("[*] Construyendo objeto textura ...")
    nm = {n: i for i, n in enumerate(NAMES_LIST)}
    tex_obj, skip_pos = build_texture_obj(W, H, px, nm)
    print(f"  Objeto textura: {len(tex_obj)} bytes")

    print("[*] Ensamblando UTX ...")
    raw_utx = build_utx(tex_obj, skip_pos, TEX_NAME)

    print(f"[*] Encriptando (Lineage2Ver121 + XOR 0x{L2_XOR:02X}) ...")
    enc = encrypt_utx(raw_utx)

    os.makedirs(CLIENT_DIR, exist_ok=True)
    with open(UTX_OUT, "wb") as f:
        f.write(enc)
    print(f"[OK] {UTX_OUT}  ({len(enc)//1024} KB)")

    with open(UTX_BACKUP, "wb") as f:
        f.write(enc)
    print(f"[OK] Copia local: {UTX_BACKUP}")

    print(f"\nReferencia en HTML:")
    print(f'  <img src="{PKG_NAME}.{TEX_NAME}" width={W} height={H}>')
    print(f"\nDistribuir: systextures/{UTX_NAME}")


if __name__ == "__main__":
    main()
