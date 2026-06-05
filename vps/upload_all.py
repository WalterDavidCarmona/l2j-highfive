"""
upload_all.py - Sube todo el proyecto L2J al VPS via SFTP
Excluye: .git, .class, geodata, logs, .claude, backup
"""
import paramiko, os, sys, stat, time

HOST = "217.216.91.50"
USER = "root"
PASS = "iMNkCRT6ev0xoxXAlG1PVX2"
LOCAL_ROOT = r"C:\Users\david\OneDrive\Desktop\l2j"
REMOTE_ROOT = "/opt/l2j"

SKIP_DIRS = {'.git', '.claude', 'backup', 'log', '__pycache__', 'geodata'}
SKIP_EXT  = {'.class', '.lck', '.pyc'}
SKIP_FILES = {'ssh_helper.py', 'upload.py', 'upload_all.py', 'upload_db.py', 'upload_jars.py'}

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username=USER, password=PASS, timeout=30, allow_agent=False, look_for_keys=False)
sftp = client.open_sftp()

created_dirs = set()
def ensure_remote_dir(path):
    if path in created_dirs or path == '/':
        return
    ensure_remote_dir(os.path.dirname(path))
    try:
        sftp.stat(path)
    except FileNotFoundError:
        sftp.mkdir(path)
    created_dirs.add(path)

total_files = 0
total_bytes = 0
errors = []
start = time.time()

for dirpath, dirnames, filenames in os.walk(LOCAL_ROOT):
    # Filtrar directorios
    dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]

    rel_dir = os.path.relpath(dirpath, LOCAL_ROOT).replace('\\', '/')
    if rel_dir == '.':
        rel_dir = ''

    for fname in filenames:
        _, ext = os.path.splitext(fname)
        if ext in SKIP_EXT or fname in SKIP_FILES:
            continue

        local_path = os.path.join(dirpath, fname)
        file_size = os.path.getsize(local_path)

        if rel_dir:
            remote_dir = REMOTE_ROOT + '/' + rel_dir
            remote_path = remote_dir + '/' + fname
        else:
            remote_dir = REMOTE_ROOT
            remote_path = REMOTE_ROOT + '/' + fname

        try:
            ensure_remote_dir(remote_dir)
            sftp.put(local_path, remote_path)
            total_files += 1
            total_bytes += file_size
            elapsed = time.time() - start
            speed = total_bytes / elapsed / 1024 / 1024 if elapsed > 0 else 0
            short = (rel_dir + '/' + fname) if rel_dir else fname
            if len(short) > 60:
                short = '...' + short[-57:]
            print(f"\r  [{total_files:4d}] {total_bytes/1024/1024:6.1f} MB | {speed:.1f} MB/s | {short:<60}", end='', flush=True)
        except Exception as e:
            errors.append(f"{remote_path}: {e}")

sftp.close()

# Fix permisos
stdin, stdout, stderr = client.exec_command("chown -R l2j:l2j /opt/l2j && chmod +x /opt/l2j/scripts/*.sh 2>/dev/null")
stdout.read()
client.close()

elapsed = time.time() - start
print(f"\n\n{'='*60}")
print(f"  Archivos subidos: {total_files}")
print(f"  Total:            {total_bytes/1024/1024:.1f} MB")
print(f"  Tiempo:           {elapsed:.0f}s ({total_bytes/elapsed/1024/1024:.2f} MB/s)")
if errors:
    print(f"  Errores:          {len(errors)}")
    for e in errors[:5]:
        print(f"    {e}")
print(f"{'='*60}")
