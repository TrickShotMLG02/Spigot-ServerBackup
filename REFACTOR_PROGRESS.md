# Platform refactor progress

Branch: `refactor/platform-core`.

## Checklist

- [x] Inventory existing features, platform dependencies and target runtime requirements.
- [x] Create a buildable Core, Bukkit, Paper, Bootstrap and Distribution Maven reactor.
- [ ] Introduce platform contracts and migrate all business logic into Core.
- [ ] Implement Bukkit and Paper adapters, bootstrap and runtime selection.
- [ ] Assemble and inspect the single distribution jar.
- [ ] Add meaningful core, architecture and adapter tests.
- [ ] Run isolated server integration tests where obtainable.
- [ ] Review feature preservation and update documentation; run a clean build.

## Inventory so far

The current plugin has backup creation, archive and extraction, dynamic region tracking,
timer and retention, local listing/search/removal, shutdown, FTP, SFTP, SMB and Dropbox,
update checking, notifications, configuration, translations and bStats. Its commands,
configuration, scheduling, storage integrations and backup code use Bukkit directly.
The existing plugin descriptor targets API 1.18 and the build compiles against Spigot 1.20.1.
Paper's documentation lists 26.3 API artifacts and requires Java 25 for Paper 26.1+:
https://docs.papermc.io/paper/dev/project-setup/ and https://docs.papermc.io/paper/getting-started/.

Feature map: `Configuration` and `Messages` load and persist the original YAML files;
`Executor` and the `Command*` classes implement the `/backup` subcommands and tab completion;
`Backup`, `ZipManager`, `Timer`, and `DynamicBackup` handle creation, archives, schedules,
retention, and region metadata; the storage managers implement FTP, SFTP, SMB and Dropbox;
`JoinListener` and `OperationHandler` handle update notices and download; `BStats` provides
metrics. Bukkit dependencies are present in the bootstrap, command and event adapters,
configuration persistence, scheduler and world integration. No NMS or CraftBukkit imports
were found in the plugin sources. Core's enforcer bans server APIs and checks transitives.

## Current state

The pre-existing untracked IDE/build files are unrelated and must be preserved. The legacy
implementation is still mostly in Bukkit. Bootstrap now selects a common Bukkit or Paper
adapter using the server name or a Paper capability marker. Paper currently reuses the
Bukkit scheduler implementation; runtime detection and class loading need server tests.
Core contains the message formatter, archive creation and extraction, schedule matching,
retention selection, backup creation/removal and archive workflow orchestration behind injected ports, and
SMB upload, listing and download, Dropbox authorization and upload, FTP/SFTP transport,
and configuration/message defaults.
Bukkit managers
adapt settings and messages. Dropbox retains chunked uploads and retry attempts; live
Dropbox integration has not been verified. Fake HTTP tests now cover small and chunked
uploads, progress and remote failures.
FTP, FTPS and SFTP network integrations have each passed against isolated local fixtures.
Remote transfer rewrites and Dropbox chunk/retry behaviour need live integration tests;
they must not be treated as production-verified based on compilation alone.
Archive extraction rejects ZIP path traversal; extracted services are used by legacy workflows.
Archive creation now propagates file errors rather than reporting a partial archive as successful.
Forty-six tests pass across Core, Bukkit, Bootstrap and Distribution; two opt-in FTP/SFTP tests are skipped in
the standard build and have passed against separate local fixtures. Core's Maven enforcer rejects transitive Bukkit, Spigot, Paper,
and Minecraft dependencies. The Bukkit Backup class now implements the backup service port;
other legacy workflows still require migration. Plugin shutdown now cancels only its own scheduled tasks instead of
all plugins' pending tasks. Reloading configuration cancels only the timer, and shutdown
requests are dispatched on the server thread. The multi-module `verify` succeeds on Java 21. This does not verify server
compatibility by itself; the server smoke tests described below have now run.
Dynamic region key computation and timer decisions are now in Core. The event listener
captures locations and writes region metadata on the server thread. Incremental distribution
builds force recreation of the unshaded jar so stale plugin metadata is not retained.
Bootstrap class loading is tested with Paper API classes denied. This does not replace a
server startup test. Bukkit-side upload notifications are dispatched to the server thread.
The distribution verification test checks the entry point, bundled adapters, absence of
server APIs, and Java 17-compatible non-versioned bytecode on every clean build.
The plugin, configuration, operation registry and scheduler are now instance-owned and
passed to Bukkit adapters; the previous global plugin singleton has been removed. Core
also owns settings/message snapshots, validation, Dropbox authorization defaults and the
remote storage contract. FTP, FTPS and SFTP fixture checks passed after the storage API
changes, including preservation of a local file when a remote file is missing.
Static inspection against the downloaded Paper 26.3 server and libraries found no missing
Bukkit/Bungee class or method references. The distribution's non-versioned class files
have major versions 50, 51, 52 or 60, compatible with Java 17. These are static checks,
not evidence of successful plugin enable or complete compatibility.
Multiple configured uploads now finish before optional local deletion; a failed provider
keeps the archive and does not prevent attempts to upload to the remaining providers.
Command routing and aliases now live in Core; individual command implementations still
reside in Bukkit. Local listing/search pagination and single-file copying now use Core;
the task registry, completion decisions and remote listing pagination are also in Core.
Remote completion uses an asynchronously refreshed cache. An isolated Java 25 JRE and Paper 26.3 build 262 were downloaded to
the system temporary directory, and the server jar's SHA-256 matches Paper's published value.
An isolated Paper 26.3 startup under Java 25 discovered ServerBackup, then stopped at
the EULA prompt before plugin enable; no agreement was recorded. This is not a plugin
runtime compatibility test. Do not accept an agreement on the user's behalf.
Official Spigot BuildTools produced a 1.20.1 server in an isolated temporary directory.
Its first startup also stopped at the EULA prompt before loading the plugin.
Isolated FTP, FTPS and SFTP fixtures passed upload, list, download and deletion integration tests;
the SFTP check also rejected a mismatched host fingerprint. SMB and Dropbox live
integrations are still unverified. Update checks now
use Core and stage the downloaded jar in Bukkit's update directory. The platform saves
loaded worlds on the server thread before starting the corresponding backup.
Archive command path validation is in Core; a Bukkit YAML test checks preservation of
existing configuration values and message overrides. Failed archive creation removes
its partial output. The adapter now waits up to 20 seconds for its worker pool and
interrupts remaining work on disable; blocking network I/O may outlast that timeout.
An attempt to provision an isolated Impacket SMB fixture failed on Windows with
`OSError: [Errno 22] Invalid argument` while accessing its Python files; no SMB server
test was performed. The user subsequently accepted the EULA for both prepared server tests.

## Next action

On 2026-11-25 the user explicitly accepted the Minecraft EULA for the isolated tests.
Paper 26.3 build 262 on Java 25 and Spigot 1.20.1 on Java 17 each discovered and enabled
the plugin, selected the expected adapter, executed `backup create world -full`, and
disabled cleanly. Both resulting ZIP files passed Python's `ZipFile.testzip()`; the
Paper archive contained 56 entries and the Spigot archive 15. No plugin errors were
found in the latest logs. These are representative smoke tests, not exhaustive feature
or platform-version verification.

Review the remaining Bukkit command, update, configuration and lifecycle adapters for
business logic or unsafe thread access; migrate any remaining policy to Core. Expand the completed server smoke tests as needed, and finish the architecture review,
documentation and clean distribution checks. Validate SMB and Dropbox when suitable
isolated services/credentials are available.

Retention now excludes the legacy `Files` directory from count-based deletion; the change is covered by a Core test and `mvn clean verify` passes.
Unique retention now identifies the complete world name and compares full and dynamic
backup timestamps consistently. Regression tests cover distinct world names and mixed
backup types; the clean Maven verification passes.
Archive extraction now rejects a symbolic link in the destination or its ancestors;
the symlink regression test and the clean Maven build passed on this machine.
Count-based retention now considers only names matching the plugin's backup naming
convention, protecting unrelated files and ordering full/dynamic backups by timestamp.
Regression tests and `mvn clean verify` pass. This intentionally changes legacy behavior
that could select unrelated files in the backup directory for deletion.
