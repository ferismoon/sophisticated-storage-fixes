param(
    [string]$Libraries = 'I:/PrismLauncher/libraries',
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

$deps = @($Mixin, $core) + @(Get-ChildItem $libraries -Recurse -File | Where-Object { $_.Name -match '^(fabric-loader-0\.19\.5|asm(-tree)?-9\.10\.1|slf4j-api-2\.0\.1|log4j-(api|core|slf4j2-impl)-2\.19\.0)\.jar$' } | ForEach-Object FullName)
$cp = $deps -join ';'
$classes = Join-Path $PSScriptRoot 'build/classes-0.2.0'
$tests = Join-Path $PSScriptRoot 'build/tests-0.2.0'
New-Item -ItemType Directory -Force -Path $classes,$tests | Out-Null
$sources = @(Get-ChildItem "$PSScriptRoot/src/main/java" -Recurse -Filter '*.java' | ForEach-Object FullName)
& javac --release 17 -proc:none -cp "$cp" -d $classes @sources
if ($LASTEXITCODE) { throw 'Compilation failed' }
& javac --release 17 -proc:none -cp "$classes;$cp" -d $tests @(Get-ChildItem "$PSScriptRoot/src/test/java" -Filter '*.java' | ForEach-Object FullName)
if ($LASTEXITCODE) { throw 'Test compilation failed' }
& java -cp "$tests;$classes;$cp" ConcurrencyTest
if ($LASTEXITCODE) { throw 'Concurrency test failed' }
if (Test-Path "$PSScriptRoot/../tools/barrel-compat/storage-fixtures.tsv") {
    & java "-Dlog4j.configurationFile=$PSScriptRoot/src/test/resources/log4j2.xml" -cp "$tests;$classes;$cp" local.barrelfix.CompatibilityTest "$PSScriptRoot/../tools/barrel-compat"
    if ($LASTEXITCODE) { throw 'Compatibility tests failed' }
}
& jar --create --file "$PSScriptRoot/build/SophisticatedStorage_Fixes-0.2.0.jar" -C $classes . -C "$PSScriptRoot/src/main/resources" .
if ($LASTEXITCODE) { throw 'Packaging failed' }
Get-FileHash "$PSScriptRoot/build/SophisticatedStorage_Fixes-0.2.0.jar"
