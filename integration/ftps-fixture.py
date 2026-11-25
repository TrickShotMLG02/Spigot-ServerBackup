import datetime, os, pathlib, tempfile
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID
from pyftpdlib.authorizers import DummyAuthorizer
from pyftpdlib.handlers import TLS_FTPHandler
from pyftpdlib.servers import FTPServer
base=pathlib.Path(tempfile.gettempdir())/'serverbackup-refactor-tools'
base.mkdir(parents=True, exist_ok=True)
root=base/'ftps-root'; root.mkdir(exist_ok=True)
key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
name=x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,'localhost')])
now=datetime.datetime.now(datetime.timezone.utc)
cert=x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-datetime.timedelta(minutes=1)).not_valid_after(now+datetime.timedelta(days=1)).sign(key,hashes.SHA256())
pem=base/'ftps-fixture.pem'; pem.write_bytes(key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.TraditionalOpenSSL,serialization.NoEncryption())+cert.public_bytes(serialization.Encoding.PEM))
a=DummyAuthorizer(); a.add_user('testuser','testpass',str(root),perm='elradfmwMT')
TLS_FTPHandler.certfile=str(pem); TLS_FTPHandler.authorizer=a; TLS_FTPHandler.tls_control_required=True; TLS_FTPHandler.tls_data_required=True
FTPServer(('127.0.0.1',21219),TLS_FTPHandler).serve_forever()
