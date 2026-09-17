param(
    [string]$Mods = 'I:\PrismLauncher\instances\Forced Relocation\minecraft\mods',
    [string]$Mixin = 'I:\PrismLauncher\libraries\net\fabricmc\sponge-mixin\0.17.0+mixin.0.8.7\sponge-mixin-0.17.0+mixin.0.8.7.jar'
)
$ErrorActionPreference = 'Stop'
$core = Join-Path $Mods 'sophisticatedcore-1.20.1-1.2.7.15.166.jar'
$storage = Join-Path $Mods 'sophisticatedstorage-1.20.1-1.3.5.11.142.jar'
$targetCode = @(& javap -p -c -classpath $storage net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelBakedModelBase)
if ($LASTEXITCODE) { throw 'Cannot inspect target class' }
$writes = @($targetCode | Select-String 'putfield.*// Field modelData:')
$reads = @($targetCode | Select-String 'getfield.*// Field modelData:')
if ($writes.Count -ne 3 -or $reads.Count -ne 1) { throw 'Target field access pattern changed; review patch before building' }
Write-Output 'Verified target has exactly three modelData writes and one read.'
$classes = Join-Path $PSScriptRoot 'build/classes'
$tests = Join-Path $PSScriptRoot 'build/tests'
New-Item -ItemType Directory -Force -Path $classes,$tests | Out-Null
$sources = @(Get-ChildItem "$PSScriptRoot/src/main/java" -Recurse -Filter '*.java' | ForEach-Object FullName)
& javac --release 17 -proc:none -cp "$Mixin;$core" -d $classes @sources
if ($LASTEXITCODE) { throw 'Compilation failed' }
& javac --release 17 -proc:none -cp "$classes;$Mixin;$core" -d $tests "$PSScriptRoot/src/test/java/ConcurrencyTest.java"
if ($LASTEXITCODE) { throw 'Test compilation failed' }
& java -cp "$tests;$classes;$Mixin;$core" ConcurrencyTest
if ($LASTEXITCODE) { throw 'Concurrency test failed' }
& jar --create --file "$PSScriptRoot/build/SophisticatedStorage_Fixes-0.1.0.jar" -C $classes . -C "$PSScriptRoot/src/main/resources" .
if ($LASTEXITCODE) { throw 'Packaging failed' }
Get-FileHash "$PSScriptRoot/build/SophisticatedStorage_Fixes-0.1.0.jar"
