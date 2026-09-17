# SophisticatedStorage_Fixes 0.1.0

Client-only add-on for Minecraft 1.20.1, Sophisticated Storage Fabric
`1.20.1-1.3.5.11.142`, and Sophisticated Core `1.20.1-1.2.7.15.166`.
The exact versions are required deliberately: the patch uses verified runtime
intermediary method names and fails loudly if the expected field accesses change.

## Problem and change

The user reproduced swapped/flickering display items when a barrel group crosses
a chunk boundary, even with only Fabric API, Forge Config API Port, and the three
Sophisticated mods. The same group within one chunk stays stable. Inventory
contents remain correct.

`BarrelBakedModelBase.emitBlockQuads` assigns the current barrel's `ModelData` to
a shared instance field. The three-argument `getQuads` reads that field later.
Concurrent chunk workers can overwrite the handoff between these operations.
This add-on redirects all three writes (including item reset and the setter) and
the single read to a per-model ThreadLocal. Null writes remove that thread's value.
It does not change inventories, networking, recipes, or barrel connections.

Source inspected: https://github.com/Salandora/SophisticatedStorage/blob/6870eca4cc618d14c85cb76a64071982f005b06c/src/main/java/net/p3pp3rf1y/sophisticatedstorage/client/render/BarrelBakedModelBase.java#L195

## Build and validation

Run `./build.ps1` using a JDK with Java 17 compilation support. Optional `-Mods`
and `-Mixin` parameters select the installed dependencies. The build uses local
jars and does not download dependencies. Output: `build/SophisticatedStorage_Fixes-0.1.0.jar`.

- Java 17 bytecode compilation and packaging pass.
- Target bytecode must contain exactly three modelData writes and one read.
- A deterministic test forces 10,000 competing chunk handoffs: the original
  shared field loses the first thread's data, while the actual redirect handlers
  retain the correct data for each thread. Null/item reset and main-thread
  isolation are checked too.
- These checks do NOT apply Mixin inside Minecraft or prove visual correctness.
  The user confirmed the fix in both the minimal TEST instance and the full Forced Relocation client. Other shared state or nested rendering could
  still need separate investigation.

## Test / rollback

Add the jar to TEST's mods folder and restart Minecraft. Use F3+G, build differently
filled barrels across a chunk boundary, move around, and extract/deposit items.
Also check barrels within one chunk and barrel items in the inventory. Reload
the world to check persistence of the visual result. Check latest.log for Mixin
errors if launch fails. Test in the full pack only after this reproduction passes.

To roll back, close Minecraft and remove only `SophisticatedStorage_Fixes-0.1.0.jar`.
No original mod jar or configuration is replaced. Existing worlds are not edited
by installation.

## Changelog

- 0.1.0: Initial experimental per-thread barrel model-data handoff fix.

The internal mod ID remains `barrel_chunk_fix` to preserve identity with the tested build. Only the display name and output filename changed for deployment. Do not install both filenames together.

## Repository layout

This folder is an independent Git repository. Suggested GitHub repository name: `sophisticated-storage-fixes`. Generated files under `build/` are excluded. Supply the `-Mods` and `-Mixin` build parameters when building on another computer; the defaults point to the original local installation.
