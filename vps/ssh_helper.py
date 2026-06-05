"""
ssh_helper.py - Ejecuta comandos en el VPS via Paramiko
Uso: python ssh_helper.py "comando"
"""
import sys, paramiko, time

HOST = "217.216.91.50"
USER = "root"
PASS = "iMNkCRT6ev0xoxXAlG1PVX2"

def run(command, timeout=120):
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, username=USER, password=PASS, timeout=15, allow_agent=False, look_for_keys=False)
    stdin, stdout, stderr = client.exec_command(command, timeout=timeout, get_pty=True)
    out = stdout.read().decode('utf-8', errors='replace')
    err = stderr.read().decode('utf-8', errors='replace')
    code = stdout.channel.recv_exit_status()
    client.close()
    return out, err, code

def upload(local, remote):
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, username=USER, password=PASS, timeout=15, allow_agent=False, look_for_keys=False)
    sftp = client.open_sftp()
    sftp.put(local, remote)
    sftp.close()
    client.close()

if __name__ == "__main__":
    cmd = " ".join(sys.argv[1:])
    o, e, c = run(cmd)
    sys.stdout.buffer.write(o.encode('utf-8', errors='replace'))
    sys.stdout.buffer.write(b'\n')
    if e: sys.stderr.buffer.write(("STDERR: "+e).encode('utf-8', errors='replace'))
    sys.exit(c)
