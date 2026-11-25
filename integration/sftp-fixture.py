import base64, hashlib, os, pathlib, socket, threading, time, tempfile, paramiko
root = pathlib.Path(tempfile.gettempdir()) / 'serverbackup-refactor-tools' / 'sftp-root'
root.mkdir(parents=True, exist_ok=True)
key = paramiko.RSAKey.generate(2048)
print('FINGERPRINT=SHA256:' + base64.b64encode(hashlib.sha256(key.asbytes()).digest()).decode().rstrip('='), flush=True)
class Auth(paramiko.ServerInterface):
 def check_auth_password(self, username, password):
  return paramiko.AUTH_SUCCESSFUL if username == 'testuser' and password == 'testpass' else paramiko.AUTH_FAILED
 def get_allowed_auths(self, username): return 'password'
 def check_channel_request(self, kind, chanid): return paramiko.OPEN_SUCCEEDED if kind == 'session' else paramiko.OPEN_FAILED_ADMINISTRATIVELY_PROHIBITED
class Sftp(paramiko.SFTPServerInterface):
 def path(self, path):
  p = (root / path.lstrip('/')).resolve()
  if p != root and root not in p.parents: raise PermissionError()
  return p
 def list_folder(self, path):
  try:
   out=[]
   for p in self.path(path).iterdir():
    a=paramiko.SFTPAttributes.from_stat(p.stat()); a.filename=p.name; out.append(a)
   return out
  except OSError as e: return paramiko.SFTPServer.convert_errno(e.errno)
 def stat(self, path):
  try: return paramiko.SFTPAttributes.from_stat(self.path(path).stat())
  except OSError as e: return paramiko.SFTPServer.convert_errno(e.errno)
 lstat = stat
 def open(self, path, flags, attr):
  try:
   fd=os.open(self.path(path), flags | getattr(os, 'O_BINARY', 0), 0o600)
   mode='r+b' if flags & os.O_RDWR else ('wb' if flags & os.O_WRONLY else 'rb')
   f=os.fdopen(fd,mode); h=paramiko.SFTPHandle(flags); h.readfile=f; h.writefile=f; return h
  except OSError as e: return paramiko.SFTPServer.convert_errno(e.errno)
 def remove(self, path):
  try: self.path(path).unlink(); return paramiko.SFTP_OK
  except OSError as e: return paramiko.SFTPServer.convert_errno(e.errno)
def handle(sock):
 t=paramiko.Transport(sock); t.add_server_key(key); t.set_subsystem_handler('sftp',paramiko.SFTPServer,Sftp)
 try:
  t.start_server(server=Auth())
  while t.is_active(): time.sleep(.1)
 finally: t.close()
s=socket.socket(); s.bind(('127.0.0.1', 21218)); s.listen(8)
while True:
 sock,_=s.accept(); threading.Thread(target=handle,args=(sock,),daemon=True).start()
