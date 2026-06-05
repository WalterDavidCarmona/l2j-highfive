"""Upload JARs and critical files to VPS directly"""
import paramiko, os

HOST="217.216.91.50"; USER="root"; PASS="iMNkCRT6ev0xoxXAlG1PVX2"
L2J = r"C:\Users\david\OneDrive\Desktop\l2j"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username=USER, password=PASS, timeout=30, allow_agent=False, look_for_keys=False)
sftp = client.open_sftp()

def mkdirs(path):
    parts = path.split('/')
    cur = ''
    for p in parts:
        if not p: continue
        cur += '/' + p
        try: sftp.mkdir(cur)
        except: pass

def upload_file(local, remote):
    size = os.path.getsize(local)
    print(f"  {os.path.basename(local)} ({size/1024/1024:.1f} MB)...", end='', flush=True)
    sftp.put(local, remote)
    print(" OK")

# JARs
print("=== Subiendo JARs ===")
mkdirs('/opt/l2j/libs')
for jar in ['GameServer.jar','LoginServer.jar','HikariCP-7.0.2.jar',
            'mysql-connector-j-9.5.0.jar','slf4j-api-2.0.17.jar','slf4j-simple-2.0.17.jar']:
    local = os.path.join(L2J, 'libs', jar)
    if os.path.exists(local):
        upload_file(local, f'/opt/l2j/libs/{jar}')

# Login config
print("=== Login config ===")
mkdirs('/opt/l2j/login/config')
login_cfg = os.path.join(L2J, 'login', 'config')
if os.path.exists(login_cfg):
    for f in os.listdir(login_cfg):
        upload_file(os.path.join(login_cfg, f), f'/opt/l2j/login/config/{f}')

# Game config
print("=== Game config ===")
mkdirs('/opt/l2j/game/config')
game_cfg = os.path.join(L2J, 'game', 'config')
for entry in os.listdir(game_cfg):
    ep = os.path.join(game_cfg, entry)
    if os.path.isfile(ep):
        upload_file(ep, f'/opt/l2j/game/config/{entry}')

print("=== Subida de archivos criticos completada ===")
sftp.close()
client.close()
