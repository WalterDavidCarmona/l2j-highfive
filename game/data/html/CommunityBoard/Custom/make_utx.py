"""
make_utx.py
Genera CustomUI.utx con la textura del banner L2 Zona Zero.
Requiere: pip install pillow
Uso: python make_utx.py

Basado en el formato Unreal Engine 2 (version 123, licensee 30)
analizado de Crest.utx del cliente L2 Liberty High Five.

Resultado: CustomUI.utx -> copiar a systextures del cliente
Referencia en HTML: <img src="CustomUI.BannerZonaZero" width=512 height=256>
"""

import struct, os, math
from PIL import Image

# ── Rutas ─────────────────────────────────────────────────────────────────────
SCRIPT_DIR  = os.path.dirname(os.path.abspath(__file__))
LOGO_IN     = os.path.join(SCRIPT_DIR, "logo_l2zonazero.png")
CLIENT_DIR  = "C:/Users/david/Downloads/Lineage 2 H5/Lineage 2 H5/L2   (HighFive) Cliente limpio/L2 After Crows  (HighFive)/SysTextures"
UTX_NAME    = "CustomUI.utx"
UTX_OUT     = os.path.join(CLIENT_DIR, UTX_NAME)
UTX_BACKUP  = os.path.join(SCRIPT_DIR,  UTX_NAME)   # copia local
TEX_NAME    = "BannerZonaZero"
PKG_NAME    = "CustomUI"
# ─────────────────────────────────────────────────────────────────────────────

# Formato textura: TEXF_RGBA8 = 5  (32-bit, sin compresion, facil de generar)
TEXF_RGBA8 = 5

# ── Compact Integer (UE2) ─────────────────────────────────────────────────────
def write_compact(val):
    """Codifica entero como compact int UE2."""
    neg = val < 0
    val = abs(val)
    out = bytearray()
    b = val & 0x3F
    if neg:
        b |= 0x40
    val >>= 6
    if val:
        b |= 0x80
    out.append(b)
    while val:
        b = val & 0x7F
        val >>= 7
        if val:
            b |= 0x80
        out.append(b)
    return bytes(out)

def read_compact(data, pos):
    b = data[pos]; pos += 1
    val = b & 0x3F
    neg = bool(b & 0x40)
    shift = 6
    while b & 0x80:
        b = data[pos]; pos += 1
        val |= (b & 0x7F) << shift
        shift += 7
    return (-val if neg else val), pos

# ── Serializar FName ──────────────────────────────────────────────────────────
def fname(name_idx, number=0):
    return write_compact(name_idx) + write_compact(number)

# ── Serializar propiedad de textura ──────────────────────────────────────────
# Tipos UE2: Byte=1 Int=2 Bool=3 Float=4 Object=5 Name=6 String=7 Class=8
# Array=9 Struct=10
PROP_INT  = 2
PROP_BYTE = 1

def prop_int(name_idx, value):
    """Propiedad INT."""
    return (write_compact(name_idx) +          # nombre
            bytes([PROP_INT << 4]) +            # type info byte
            struct.pack('<i', value))           # valor

def prop_byte_val(name_idx, value):
    """Propiedad BYTE (sin enum)."""
    return (write_compact(name_idx) +
            bytes([PROP_BYTE << 4]) +
            struct.pack('<B', value))

def prop_none(none_idx):
    return write_compact(none_idx)

# ── Imagen ───────────────────────────────────────────────────────────────────
def load_and_resize_logo(path):
    img = Image.open(path).convert("RGBA")
    orig_w, orig_h = img.size
    print(f"  Logo original: {orig_w}x{orig_h}")

    # Escalar al power-of-2 mas cercano que quepa bien
    # 512x256 es buena relacion para este logo (800x448 -> ~16:9)
    target_w, target_h = 512, 256
    img = img.resize((target_w, target_h), Image.LANCZOS)
    print(f"  Redimensionado a: {target_w}x{target_h} (RGBA8)")
    return img

def img_to_rgba8(img):
    """Convierte imagen PIL a bytes RGBA8 raw (formato L2 = BGRA invertido)."""
    r, g, b, a = img.split()
    # L2 usa BGRA internamente para TEXF_RGBA8
    bgra = Image.merge("RGBA", (b, g, r, a))
    return bgra.tobytes()

# ── Construir objeto Texture ──────────────────────────────────────────────────
def build_texture_object(img, names):
    """Construye los bytes del objeto Texture (properties + mipmaps)."""
    W, H = img.size
    ubits = int(math.log2(W))
    vbits = int(math.log2(H))
    px = img_to_rgba8(img)

    n = {name: i for i, name in enumerate(names)}

    obj = bytearray()

    # ── Properties ──────────────────────────────────────────────────────────
    # USize
    obj += write_compact(n["USize"])
    obj += bytes([PROP_INT << 4])
    obj += struct.pack('<i', W)
    # VSize
    obj += write_compact(n["VSize"])
    obj += bytes([PROP_INT << 4])
    obj += struct.pack('<i', H)
    # UClamp
    obj += write_compact(n["UClamp"])
    obj += bytes([PROP_INT << 4])
    obj += struct.pack('<i', W)
    # VClamp
    obj += write_compact(n["VClamp"])
    obj += bytes([PROP_INT << 4])
    obj += struct.pack('<i', H)
    # UBits
    obj += write_compact(n["UBits"])
    obj += bytes([PROP_BYTE << 4])
    obj += struct.pack('<B', ubits)
    # VBits
    obj += write_compact(n["VBits"])
    obj += bytes([PROP_BYTE << 4])
    obj += struct.pack('<B', vbits)
    # Format
    obj += write_compact(n["Format"])
    obj += bytes([PROP_BYTE << 4])
    obj += struct.pack('<B', TEXF_RGBA8)
    # None (fin de properties)
    obj += write_compact(n["None"])

    # ── Mip maps ─────────────────────────────────────────────────────────────
    # Numero de mip maps del lado cliente
    obj += write_compact(1)

    # Mip 0 (resolucion completa)
    obj += write_compact(len(px))   # tamanio de datos
    obj += px                        # pixels BGRA
    obj += struct.pack('<II', W, H)  # dimensiones
    obj += struct.pack('<BB', ubits, vbits)  # bits

    return bytes(obj)

# ── Construir Name Table ──────────────────────────────────────────────────────
def build_name_table(names):
    data = bytearray()
    for name in names:
        enc  = (name + '\0').encode('latin-1')
        data += struct.pack('<B', len(enc))
        data += enc
        data += struct.pack('<I', 0x00070010)  # flags estandar (de Crest.utx)
    return bytes(data)

# ── Construir Import Table ────────────────────────────────────────────────────
def build_import_table(names):
    """
    Import 0: Engine.Texture  (la clase de nuestro objeto)
    Import 1: Engine          (el package)
    """
    n = {name: i for i, name in enumerate(names)}
    data = bytearray()

    # Import 0: Engine.Texture
    # class_package = Core (index n["Core"])
    # class_name    = Class (index n["Class"])
    # outer_index   = -2 (import #2 = Engine package)
    # object_name   = Texture
    data += write_compact(n["Core"])
    data += write_compact(n["Class"])
    data += struct.pack('<i', -2)
    data += write_compact(n["Texture"])

    # Import 1: Engine (package)
    # class_package = Core
    # class_name    = Package
    # outer_index   = 0 (top level)
    # object_name   = Engine
    data += write_compact(n["Core"])
    data += write_compact(n["Package"])
    data += struct.pack('<i', 0)
    data += write_compact(n["Engine"])

    return bytes(data)

# ── Construir Export Table ────────────────────────────────────────────────────
def build_export_entry(names, tex_name_idx, serial_size, serial_offset):
    """Un unico export: nuestra textura."""
    n = {name: i for i, name in enumerate(names)}
    data = bytearray()

    # class_index  = -1 (import #1 = Engine.Texture)
    data += write_compact(-1)
    # super_index  = 0 (sin superclase explicita)
    data += write_compact(0)
    # outer_index  = 0 (top-level, sin outer)
    data += struct.pack('<i', 0)
    # object_name (FName = index + number)
    data += write_compact(tex_name_idx)
    data += write_compact(0)
    # object_flags: RF_Public | RF_Standalone | RF_SourceModified = 0x00040004
    data += struct.pack('<I', 0x00040004)
    # serial_size
    data += write_compact(serial_size)
    # serial_offset
    data += write_compact(serial_offset)

    return bytes(data)

# ── GUID aleatorio ────────────────────────────────────────────────────────────
import hashlib
def make_guid(seed):
    h = hashlib.md5(seed.encode()).digest()
    return h  # 16 bytes

# ── Ensamblar el UTX completo ─────────────────────────────────────────────────
def build_utx(img, pkg_name, tex_name):
    print(f"  Construyendo {pkg_name}.utx ...")

    # Lista de nombres (orden importa para indices)
    names = [
        "None",        # 0
        "Core",        # 1
        "Engine",      # 2
        "Class",       # 3
        "Package",     # 4
        "Texture",     # 5
        "USize",       # 6
        "VSize",       # 7
        "UBits",       # 8
        "VBits",       # 9
        "UClamp",      # 10
        "VClamp",      # 11
        "Format",      # 12
        pkg_name,      # 13  (CustomUI)
        tex_name,      # 14  (BannerZonaZero)
    ]

    n = {name: i for i, name in enumerate(names)}

    # 1. Construir secciones
    name_table   = build_name_table(names)
    import_table = build_import_table(names)
    tex_obj      = build_texture_object(img, names)

    # 2. Calcular offsets
    # Header fijo: 64 bytes
    HEADER_SIZE   = 64
    name_offset   = HEADER_SIZE
    # Tras name table: objeto de textura
    tex_obj_offset = name_offset + len(name_table)
    # Tras textura: import table
    import_offset = tex_obj_offset + len(tex_obj)
    # Tras import: export table
    export_offset = import_offset + len(import_table)

    # 3. Construir export entry ahora que sabemos el offset
    export_entry = build_export_entry(names, n[tex_name], len(tex_obj), tex_obj_offset)

    # 4. Header UE2 (64 bytes)
    guid = make_guid(pkg_name + tex_name)

    header = bytearray()
    header += struct.pack('<I', 0x9E2A83C1)   # signature
    header += struct.pack('<H', 123)           # version (igual a Crest.utx)
    header += struct.pack('<H', 37)            # licensee (L2 After Crows H5)
    header += struct.pack('<I', 0x00000001)    # package flags
    header += struct.pack('<I', len(names))    # name_count
    header += struct.pack('<I', name_offset)   # name_offset
    header += struct.pack('<I', 1)             # export_count
    header += struct.pack('<I', export_offset) # export_offset
    header += struct.pack('<I', 2)             # import_count
    header += struct.pack('<I', import_offset) # import_offset
    header += guid                             # GUID (16 bytes)
    header += struct.pack('<I', 1)             # generation_count
    header += struct.pack('<I', 1)             # gen_export_count
    header += struct.pack('<I', len(names))    # gen_name_count

    assert len(header) == HEADER_SIZE, f"Header size {len(header)} != {HEADER_SIZE}"

    utx = bytes(header) + name_table + tex_obj + import_table + export_entry
    return utx

# ── Main ──────────────────────────────────────────────────────────────────────
def encrypt_utx(raw_utx):
    """
    Encripta el UTX con el formato del cliente L2 After Crows High Five:
    - Header 'Lineage2Ver121' en UTF-16-LE (28 bytes)
    - Resto del archivo XOR con 0x38
    """
    HEADER  = "Lineage2Ver121".encode("utf-16-le")   # 28 bytes
    XOR_KEY = 0x38
    return HEADER + bytes(b ^ XOR_KEY for b in raw_utx)


def main():
    print("[*] Cargando logo ...")
    img = load_and_resize_logo(LOGO_IN)

    print("[*] Generando UTX ...")
    utx_data = build_utx(img, PKG_NAME, TEX_NAME)

    # Encriptar con formato del cliente
    enc_data = encrypt_utx(utx_data)
    print(f"[*] Encriptando (Lineage2Ver121 + XOR 0x38) ...")

    # Guardar en cliente (encriptado)
    os.makedirs(CLIENT_DIR, exist_ok=True)
    with open(UTX_OUT, 'wb') as f:
        f.write(enc_data)
    print(f"[OK] {UTX_OUT}  ({len(enc_data)//1024} KB)")

    # Guardar copia local (encriptada, para distribuir en patch)
    with open(UTX_BACKUP, 'wb') as f:
        f.write(enc_data)
    print(f"[OK] Copia: {UTX_BACKUP}")

    print(f"\nReferencia en HTML:")
    print(f'  <img src="{PKG_NAME}.{TEX_NAME}" width=512 height=256>')
    print(f"\nDistribuir con el cliente:")
    print(f"  systextures/{UTX_NAME}")

if __name__ == "__main__":
    main()
