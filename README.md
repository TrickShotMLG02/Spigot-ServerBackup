# ServerBackup

ServerBackup creates local server and world archives and can upload them to FTP/FTPS,
SFTP, SMB and Dropbox. It retains the existing `/backup` commands, `backup.*`
permissions, `plugins/ServerBackup` data files, YAML configuration keys and ZIP backup
naming conventions.

## Build

Use JDK 17 or newer and Maven 3.9 or newer:

```sh
mvn clean verify
```

The deployable artifact is `Distribution/target/ServerBackup-2.13.0.jar`. Place that
single JAR in the server's `plugins` directory. Server API dependencies are provided
by the server and are not bundled. The shared code is compiled for Java 16; the
existing Minecraft 1.18+ server baseline requires Java 17 or newer. Paper 26.1 and
newer require Java 25. Use the Java version required by your server.

The distribution has a Bukkit-compatible entry point and selects the Bukkit/Spigot or
Paper adapter at startup. Paper uses the common Bukkit implementation for the APIs
currently needed. Isolated smoke tests on Paper 26.3 and Spigot 1.20.1 enabled the
plugin and created valid world backup archives. See `REFACTOR_PROGRESS.md` for the
scope of these tests and the remaining verification.

## Modules

- `Core`: platform-independent backup, archive, retention, scheduling decisions,
  command routing, configuration defaults, storage transports and service contracts.
  It has no Bukkit, Spigot, Paper or Minecraft API dependencies; Maven Enforcer checks
  direct and transitive dependencies.
- `Bukkit`: Bukkit/Spigot configuration and event bindings, command adapters, messaging,
  scheduling, world-save integration and storage notifications.
- `Paper`: the Paper adapter, which currently reuses Bukkit functionality.
- `Bootstrap`: the Bukkit-compatible entry point and runtime adapter selection.
- `Distribution`: assembles the one shaded JAR, merges service descriptors and excludes
  server APIs and dependency signature files.

To add a platform, implement the contracts used by the relevant Core services, supply
an entry point and inject its adapter. Keep platform API types out of Core. For future
Bukkit/Paper compatibility changes, add isolated implementations to the appropriate
adapter and select by capability in Bootstrap; avoid version checks in Core. A genuinely
new plugin system may require its own entry point and distribution format.

Before archiving a loaded world, the Bukkit adapter requests a world save on the server
thread. This is not a filesystem snapshot: files can still change while an archive is
being written. When multiple remote destinations are enabled, local deletion is deferred
until all configured uploads report success.

## Tests

`mvn clean verify` runs Core, Bukkit configuration/lifecycle and Bootstrap tests. The
optional FTP and SFTP integration tests require isolated local fixtures and environment
variables; they are skipped by default. See `REFACTOR_PROGRESS.md` for performed checks
and remaining verification, including server startup and external storage services.

Original project: https://www.spigotmc.org/resources/server-backup-ingame-dropbox-ftp-backup-1-18-1-20-multithreaded.79320/
