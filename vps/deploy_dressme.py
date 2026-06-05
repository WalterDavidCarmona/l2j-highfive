"""
deploy_dressme.py - DressMe system deploy script
Sube, compila y parcha el sistema DressMe en el VPS.
"""
import sys, os, time, base64
import paramiko

HOST = "217.216.91.50"
USER = "root"
PASS = "iMNkCRT6ev0xoxXAlG1PVX2"

BASE   = os.path.join(os.path.dirname(__file__), "..")
PATCH  = os.path.join(BASE, "dressme_patch", "src")
SCRIPTS = os.path.join(BASE, "game", "data", "scripts")
JAVA_HOME = "/usr/lib/jvm/temurin-25-jdk-amd64"

def connect():
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, username=USER, password=PASS, timeout=30,
                   allow_agent=False, look_for_keys=False,
                   banner_timeout=30, auth_timeout=30)
    transport = client.get_transport()
    transport.set_keepalive(30)
    return client

def run_cmd(cmd, timeout=120):
    for attempt in range(3):
        try:
            client = connect()
            stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout, get_pty=True)
            out = stdout.read().decode('utf-8', errors='replace')
            err = stderr.read().decode('utf-8', errors='replace')
            code = stdout.channel.recv_exit_status()
            client.close()
            return out, err, code
        except Exception as ex:
            print(f"  [retry {attempt+1}/3] {ex}")
            time.sleep(3)
    raise RuntimeError(f"Command failed after 3 retries: {cmd[:80]}")

def upload_file_via_ssh(local_path, remote_path):
    """Upload file by reading locally, encoding as base64, and writing remotely via SSH."""
    with open(local_path, 'rb') as f:
        data = f.read()
    b64 = base64.b64encode(data).decode('ascii')
    # Write in chunks to avoid arg too long
    chunk_size = 60000
    # First create the remote directory
    remote_dir = os.path.dirname(remote_path).replace('\\', '/')
    run_cmd(f"mkdir -p {remote_dir}")
    # Write using base64 decode
    cmd = f"echo '{b64}' | base64 -d > {remote_path}"
    if len(b64) > chunk_size:
        # Write in parts
        cmd = f"printf '%s' '{b64}' | base64 -d > {remote_path}"
    o, e, c = run_cmd(cmd)
    if c != 0:
        raise RuntimeError(f"Upload failed for {remote_path}: {e}")

def step(msg):
    print(f"\n{'='*60}\n  {msg}\n{'='*60}")

def check(out, err, code, label):
    if out.strip():
        print(out.strip())
    if err.strip() and 'warning' not in err.lower():
        print("STDERR:", err.strip())
    if code != 0:
        print(f"[ERROR] {label} failed (exit {code})")
        sys.exit(1)
    print(f"[OK] {label}")

# ── Files to upload ───────────────────────────────────────────────
# NOTE: CharInfo.java and UserInfo.java are NOT patched (causes dark screen).
# DressMe interception goes through Inventory.getPaperdollItemDisplayId via javassist.
FILES = [
    (
        os.path.join(PATCH, "org/l2jmobius/gameserver/custom/dressme/DressMeData.java"),
        "/opt/dressme/src/org/l2jmobius/gameserver/custom/dressme/DressMeData.java"
    ),
    (
        os.path.join(PATCH, "org/l2jmobius/gameserver/custom/dressme/DressMeManager.java"),
        "/opt/dressme/src/org/l2jmobius/gameserver/custom/dressme/DressMeManager.java"
    ),
    (
        os.path.join(SCRIPTS, "handlers/chat/commands/voiced/DressMe.java"),
        "/opt/l2j/game/data/scripts/handlers/chat/commands/voiced/DressMe.java"
    ),
    (
        os.path.join(SCRIPTS, "handlers/MasterHandler.java"),
        "/opt/l2j/game/data/scripts/handlers/MasterHandler.java"
    ),
]

# ── 1. Create remote directories ──────────────────────────────────
step("Creating remote directories")
o, e, c = run_cmd(
    "mkdir -p "
    "/opt/dressme/src/org/l2jmobius/gameserver/custom/dressme "
    "/opt/dressme/src/org/l2jmobius/gameserver/network/serverpackets "
    "/opt/dressme/out "
    "/opt/l2j/game/data/scripts/handlers/chat/commands/voiced"
)
check(o, e, c, "mkdir")

# ── 2. Upload files ───────────────────────────────────────────────
step("Uploading Java source files")
for local, remote in FILES:
    fname = os.path.basename(local)
    print(f"  Uploading {fname}...", end=" ")
    upload_file_via_ssh(local, remote)
    print("OK")
    time.sleep(1)

# ── 3. Verify uploaded files ──────────────────────────────────────
step("Verifying uploaded files")
o, e, c = run_cmd("find /opt/dressme/src /opt/l2j/game/data/scripts/handlers/chat/commands/voiced/DressMe.java -name '*.java' 2>/dev/null | sort")
print(o.strip())

# ── 4. Compile ────────────────────────────────────────────────────
step("Compiling with Temurin JDK 25")
JAVAC   = f"{JAVA_HOME}/bin/javac"
JAR_CP  = "/opt/l2j/libs/GameServer.jar"
SRC     = "/opt/dressme/src"
OUT     = "/opt/dressme/out"

compile_cmd = (
    f"{JAVAC} --release 21 -cp {JAR_CP} "
    f"-sourcepath {SRC} "
    f"-d {OUT} "
    f"{SRC}/org/l2jmobius/gameserver/custom/dressme/DressMeData.java "
    f"{SRC}/org/l2jmobius/gameserver/custom/dressme/DressMeManager.java "
    f"2>&1"
)
o, e, c = run_cmd(compile_cmd, timeout=120)
combined = (o + e).strip()
if combined:
    print(combined)
if "error:" in combined.lower():
    print("[ERROR] Compilation failed - see errors above")
    sys.exit(1)
print("[OK] Compilation successful")

# ── 5. Verify compiled classes ────────────────────────────────────
step("Verifying compiled classes")
o, e, c = run_cmd(f"find {OUT} -name '*.class' | sort")
print(o.strip() or "  (no classes found - check compilation)")

# ── 6. Backup JAR ────────────────────────────────────────────────
step("Backing up GameServer.jar")
o, e, c = run_cmd("cp /opt/l2j/libs/GameServer.jar /opt/l2j/libs/GameServer.jar.bak_dressme && echo 'Backup OK'")
check(o, e, c, "backup")

# ── 7. Patch JAR ─────────────────────────────────────────────────
step("Patching GameServer.jar")
JAR = f"{JAVA_HOME}/bin/jar"
patch_cmd = (
    f"cd {OUT} && "
    f"{JAR} uf /opt/l2j/libs/GameServer.jar "
    f"org/l2jmobius/gameserver/custom/dressme/DressMeData.class "
    f"org/l2jmobius/gameserver/custom/dressme/DressMeManager.class "
    f"&& echo 'JAR core patch OK' && "
    # Javassist patch for Inventory.getPaperdollItemDisplayId
    f"cd /opt/dressme/patcher && "
    f"{JAVA_HOME}/bin/javac --release 21 -cp /opt/tools/javassist.jar PatchInventory.java -d . && "
    f"rm -rf patched_classes && mkdir -p patched_classes && "
    f"{JAVA_HOME}/bin/java -cp .:/opt/tools/javassist.jar:/opt/l2j/libs/GameServer.jar:{OUT} "
    f"PatchInventory /opt/l2j/libs/GameServer.jar && "
    f"cd patched_classes && "
    f"{JAR} uf /opt/l2j/libs/GameServer.jar "
    f"org/l2jmobius/gameserver/model/itemcontainer/Inventory.class "
    f"&& echo 'Inventory javassist patch OK'"
)
o, e, c = run_cmd(patch_cmd, timeout=60)
check(o, e, c, "jar patch")

print("\n" + "="*60)
print("  DressMe deployed successfully!")
print("  -> Restart GameServer to apply changes.")
print("  -> DB table 'character_dressme' is auto-created on startup.")
print("="*60)
