# Sophisticated Storage Fixes

Fixes flickering or incorrect item displays on Sophisticated Storage barrels, especially when a storage area crosses a chunk boundary.

The items displayed on the front of your barrels stay consistent as you move around. It does not change the items stored inside them.

## Installation

Download the JAR from [Releases](https://github.com/ferismoon/sophisticated-storage-fixes/releases/latest), add it to your Minecraft `mods` folder, and restart the game. Install it on your client; servers do not need it. No configuration is needed.

When updating, remove the previous fix JAR so only one version is installed.

## Requirements

- Minecraft 1.20.1
- Fabric Loader 0.16.9 or newer
- Java 17 or newer
- Sophisticated Storage (Fabric): published releases from 1.20.1-1.0.10.1.100 through 1.20.1-1.3.5.11.142
- Sophisticated Core: published releases from 1.20.1-1.0.8.1.119 through 1.20.1-1.2.7.15.166

Use a Core version that also meets your installed Storage version's requirements. These ranges apply to the unofficial Fabric ports, not the Forge versions.

## Compatibility

The fix checks your installed versions and the relevant mod files when Minecraft starts. If either mod is missing, outside the supported releases, or has unexpected rendering code, the fix disables itself and writes a `Barrel fix DISABLED` warning to the console and `logs/latest.log`. Barrel displays may still flicker while the fix is disabled.

This check does not bypass Minecraft, Fabric, or the other mods' own requirements. It also cannot prevent unrelated crashes or conflicts with other rendering patches.
