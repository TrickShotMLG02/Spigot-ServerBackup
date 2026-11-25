# Optional transport integration tests

These fixtures bind only to `127.0.0.1` and create their files under the operating
system temporary directory `serverbackup-refactor-tools`. They use the test-only
credentials `testuser` / `testpass` and must not be exposed to a network. Install
`pyftpdlib`, `pyOpenSSL`, `cryptography` and `paramiko` into an isolated Python
virtual environment before starting them.

For FTP, start pyftpdlib on port 21217 with a writable temporary directory:

```sh
python -m pyftpdlib -i 127.0.0.1 -p 21217 -u testuser -P testpass -w -d /path/to/empty/ftp-root
```

Set `SERVERBACKUP_FTP_TEST_PORT=21217` and run
`mvn -pl Core -Dtest=FtpIntegrationTest test`. For FTPS, run
`python integration/ftps-fixture.py`, set the same variable to `21219`, and run the
same test. The FTPS fixture generates a temporary self-signed certificate and requires
TLS for control and data channels.

For SFTP, run `python integration/sftp-fixture.py`, set
`SERVERBACKUP_SFTP_FINGERPRINT` to the printed `SHA256:...` value, and run
`mvn -pl Core -Dtest=SftpIntegrationTest test`. The fixture listens on port 21218.
The test deliberately tries an incorrect fingerprint first; a rejected handshake can
produce a fixture-side error before the successful transfer tests.

Stop each fixture after its test. Neither fixture contacts an external service.
