"""
deploy_dressme.py - Sube scripts DressMe al VPS y reinicia el servidor.
NOTA: NO parchea el JAR. El DressMe funciona 100% via scripts usando item.setTransmogId().
"""
import sys, os, time, base64
import paramiko

HOST = "217.216.91.50"
USER = "root"
PASS = "iMNkCRT6ev0xoxXAlG1PVX2"

BASE    = os.path.join(os.path.dirname(__file__), "..")
SCRIPTS = os.path.join(BASE, "game", "data", "scripts")


def connect():
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, username=USER, password=PASS, timeout=30,
                   allow_agent=False, look_for_keys=False,
                   banner_timeout=30, auth_timeout=30)
    client.get_transport().set_keepalive(30)
    return client


def run_cmd(cmd, timeout=60):
    for attempt in range(3):
        try:
            client = connect()
            _, stdout, stderr = client.exec_command(cmd, timeout=timeout, get_pty=True)
            out  = stdout.read().decode('utf-8', errors='replace')
            err  = stderr.read().decode('utf-8', errors='replace')
            code = stdout.channel.recv_exit_status()
            client.close()
            return out, err, code
        except Exception as ex:
            print(f"  [retry {attempt+1}/3] {ex}")
            time.sleep(3)
    raise RuntimeError(f"Command failed: {cmd[:80]}")


def upload_file_via_ssh(local_path, remote_path):
    with open(local_path, 'rb') as f:
        data = f.read()
    b64 = base64.b64encode(data).decode('ascii')
    remote_dir = os.path.dirname(remote_path).replace('\\', '/')
    run_cmd(f"mkdir -p {remote_dir}")
    cmd = f"printf '%s' '{b64}' | base64 -d > {remote_path}"
    o, e, c = run_cmd(cmd)
    if c != 0:
        raise RuntimeError(f"Upload failed for {remote_path}: {e}")


if __name__ == "__main__":
    FILES = [
        (os.path.join(SCRIPTS, "custom/DressMe/DressMeData.java"),
         "/opt/l2j/game/data/scripts/custom/DressMe/DressMeData.java"),
        (os.path.join(SCRIPTS, "custom/DressMe/DressMeManager.java"),
         "/opt/l2j/game/data/scripts/custom/DressMe/DressMeManager.java"),
        (os.path.join(SCRIPTS, "handlers/chat/commands/voiced/DressMe.java"),
         "/opt/l2j/game/data/scripts/handlers/chat/commands/voiced/DressMe.java"),
        (os.path.join(SCRIPTS, "handlers/MasterHandler.java"),
         "/opt/l2j/game/data/scripts/handlers/MasterHandler.java"),
    ]

    print("=== Uploading DressMe scripts ===")
    run_cmd("mkdir -p /opt/l2j/game/data/scripts/custom/DressMe")

    for local, remote in FILES:
        fname = os.path.basename(local)
        print(f"  {fname}...", end=" ", flush=True)
        upload_file_via_ssh(local, remote)
        print("OK")
        time.sleep(1)

    print("\n=== Restarting GameServer ===")
    run_cmd("systemctl restart l2j-game")
    print("Waiting for startup...")
    time.sleep(55)
    o, _, _ = run_cmd("grep -E 'VoicedCommand|Server loaded' /opt/l2j/game/log/java0.log | tail -3")
    print(o.strip())
    print("\nDone.")
