# Moonlight

Minecraft client wrapper built with Gradle and bundled via ShadowJar.

## Build

```bash
bash scripts/build.sh
```

Outputs:
- `build/libs/Moonlight-1.8.9.jar`
- `build/libs/Moonlight-1.8.9-all.jar`

Clean:

```bash
bash scripts/clean.sh
```

## Launcher support

This project is exported as a standard runnable JAR, so it can be used from nearly any launcher that supports a custom client or custom Java launch command.

Recommended default file for most launchers:

```
build/libs/Moonlight-1.8.9-all.jar
```

### Generic launch config

If your launcher allows a custom launch command, use a pattern like this:

```bash
java -jar Moonlight-1.8.9-all.jar [your extra args]
```

If your launcher instead uses a classpath / main class launch, use:

- **Main class:** `Start`
- **Classpath:** `build/libs/Moonlight-1.8.9-all.jar`
- **Java args:** whatever your launcher normally uses for a 1.8.9 client

### Common launcher setups

- **Prism Launcher**
  - Add a new instance/profile for this client version.
  - Use the bundled JAR as the client file, or import it as a custom modpack/client if your launcher supports that.
  - If the launcher requires a separate game directory, point it at an empty folder dedicated to Moonlight.

- **MultiMC / Prism-style custom instances**
  - Create a custom instance.
  - Set the Minecraft version to match the client’s expected version (`1.8.9` in this project).
  - Add the exported JAR as the client/mod entry point your launcher expects.

- **ATLauncher / GDLauncher / similar**
  - Import as a custom client or manual JAR entry.
  - Use the same version and Java settings the launcher recommends for 1.8.9.

- **Custom launcher / batch launch**
  - Keep the launch command simple and explicit.
  - If you need to pass additional arguments, append them after the JAR name.

### Notes

- The `*-all.jar` build is usually the better choice for launchers because it already bundles dependencies.
- If a launcher expects a native library path, JVM flags, or a game directory, configure those in the launcher, not by editing the project source.
- This repo does not include launcher-specific metadata packs or installer packages. If you want a launcher-ready package in a specific format, say which one and I can add a small packaging step.

## Repository

Issues and suggestions:
https://github.com/Bzdhyp/Moonlight-v2/issues
