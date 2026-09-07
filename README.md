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

This project is exported as a standard runnable JAR, so it can be used from nearly any Minecraft launcher that supports a custom client, custom JAR import, or custom Java launch command.

Recommended default file for most launchers:

```
build/libs/Moonlight-1.8.9-all.jar
```

For launcher import, this repository also includes simple JSON metadata:

- `json/launcher.json` - generic launcher metadata
- `json/launcher-config.json` - fallback import settings

### Generic launch config

If your launcher allows a custom launch command, use a pattern like this:

```bash
java -jar Moonlight-1.8.9-all.jar [your extra args]
```

If your launcher instead uses a classpath / main class launch, use:

- **Main class:** `Start`
- **Classpath / JAR:** `build/libs/Moonlight-1.8.9-all.jar`
- **Java args:** whatever your launcher normally uses for a 1.8.9 client

If a launcher asks for client metadata, point it at `json/launcher.json` and fall back to `json/launcher-config.json` when needed.

### Common launcher setups

- **Prism Launcher**
  - Add a new instance/profile for this client version.
  - Use the bundled JAR as the client file, or import it as a custom client if the launcher supports that.
  - If the launcher requires a separate game directory, point it at an empty folder dedicated to Moonlight.

- **MultiMC / Prism-style custom instances**
  - Create a custom instance.
  - Set the Minecraft version to match the client’s expected version (`1.8.9` in this project).
  - Add the exported JAR as the client entry point your launcher expects.

- **ATLauncher / GDLauncher / similar**
  - Import as a custom client or manual JAR entry.
  - Use the same version and Java settings the launcher recommends for 1.8.9.

- **CurseForge / bundled modpack launchers**
  - If your launcher supports custom imports, use the all-in-one JAR and the version metadata above.
  - This project does not include launcher-specific mod metadata unless you ask for a specific format.

- **Mobile / Android launchers**
  - Most mobile Minecraft launchers expect a standard Minecraft app setup, not a desktop Java classpath launch.
  - If a mobile launcher supports a custom client, use the all-in-one JAR as the client entry point and keep the client version set to `1.8.9` if the launcher asks.
  - If the launcher expects a mobile app package or a specific client format, this repository does not provide that automatically.

- **Custom launcher / batch launch**
  - Keep the launch command simple and explicit.
  - If you need to pass additional arguments, append them after the JAR name.

### Notes

- The `*-all.jar` build is usually the better choice for launchers because it already bundles dependencies.
- If a launcher expects a native library path, JVM flags, or a game directory, configure those in the launcher, not by editing the project source.
- This repo includes generic launcher JSON metadata so the client can be discovered or imported in more launcher setups.
- For desktop launchers, this is usually enough. For mobile launchers, compatibility depends on what the launcher itself supports.
- If a launcher asks for a specific manifest format, tell me which one and I can add a small packaging step or metadata file for it.

## Repository

Issues and suggestions:
https://github.com/Bzdhyp/Moonlight-v2/issues

## Server mode

This project can run in server mode when started with the server entry point.

```bash
java -cp build/libs/Moonlight-1.8.9-all.jar net.minecraft.server.Main [your server args]
```

If your launcher or hosting setup requires a different server entry class or extra JVM flags, configure those outside the repository source.

Technical notes:

- This repository is built as a Gradle + ShadowJar project.
- The main desktop launch entry point is `Start`.
- The all-in-one JAR is the recommended artifact for launcher import and server packaging.
- Mobile launcher support depends on what the launcher itself accepts. If a specific mobile client package is required, that must be provided or generated separately.
