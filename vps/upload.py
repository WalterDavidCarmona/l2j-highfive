"""Upload l2j project ZIP to VPS with progress"""
import paramiko, os, sys, time

HOST = "217.216.91.50"
USER = "root"
PASS = "iMNkCRT6ev0xoxXAlG1PVX2"

local = r"C:\Users\david\AppData\Local\Temp\l2j_project.zip"
remote = "/tmp/l2j_project.zip"

size = os.path.getsize(local)
print(f"Subiendo {size/1024/1024:.1f} MB al VPS...")

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username=USER, password=PASS, timeout=30,
               allow_agent=False, look_for_keys=False)

sftp = client.open_sftp()
start = time.time()
transferred = [0]

def progress(done, total):
    transferred[0] = done
    pct = done/total*100
    elapsed = time.time()-start
    speed = done/elapsed/1024/1024 if elapsed>0 else 0
    bar = "#"*int(pct/5) + "-"*(20-int(pct/5))
    print(f"\r  [{bar}] {pct:.0f}% | {done/1024/1024:.1f}/{total/1024/1024:.1f} MB | {speed:.1f} MB/s", end="", flush=True)

sftp.put(local, remote, callback=progress)
sftp.close()
client.close()
elapsed = time.time()-start
print(f"\nSubida completada en {elapsed:.0f}s ({size/elapsed/1024/1024:.1f} MB/s)")
