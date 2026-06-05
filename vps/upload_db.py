"""Upload DB dump to VPS"""
import paramiko, os

HOST="217.216.91.50"; USER="root"; PASS="iMNkCRT6ev0xoxXAlG1PVX2"
import os
local = os.path.join(os.environ['TEMP'], 'l2j_db.sql')
remote = "/tmp/l2j_db.sql"

size = os.path.getsize(local)
print(f"Subiendo DB dump {size/1024:.0f} KB...")
client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username=USER, password=PASS, timeout=15, allow_agent=False, look_for_keys=False)
sftp = client.open_sftp()
sftp.put(local, remote)
sftp.close()
client.close()
print("DB dump subido OK")
