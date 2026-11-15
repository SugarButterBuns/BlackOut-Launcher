# Builds BlackOut Launcher into launcher\out\BlackOutLauncher.apk.
#
# Needs: JDK 17, Android build-tools 34.0.0 and the Android 14 (API 34) platform.
# By default it looks in ..\toolchain (what tools\get-toolchain.ps1 creates).
# If you already have an Android SDK, point at it instead:
#   .\build.ps1 -Jdk "C:\path\to\jdk-17" -BuildTools "$env:ANDROID_HOME\build-tools\34.0.0" -AndroidJar "$env:ANDROID_HOME\platforms\android-34\android.jar"
param(
    [string]$Jdk,
    [string]$BuildTools,
    [string]$AndroidJar
)
# Tools print notes on stderr; failures are caught with $LASTEXITCODE checks instead.
$ErrorActionPreference = 'Continue'
$here = $PSScriptRoot
$tc   = Join-Path (Split-Path $here) 'toolchain'

if (-not $Jdk)        { $Jdk = (Get-ChildItem -Directory (Join-Path $tc 'jdk-*') -ErrorAction SilentlyContinue | Select-Object -First 1).FullName }
if (-not $Jdk)        { $Jdk = $env:JAVA_HOME }
if (-not $BuildTools) { $BuildTools = Join-Path $tc 'android-14' }                 # build-tools 34.0.0 unzips to "android-14"
if (-not $AndroidJar) { $AndroidJar = Join-Path $tc 'android-34\android.jar' }
foreach ($p in @("$Jdk\bin\javac.exe", "$BuildTools\aapt2.exe", $AndroidJar)) {
    if (-not (Test-Path $p)) { throw "Missing $p - run tools\get-toolchain.ps1 or pass -Jdk/-BuildTools/-AndroidJar" }
}
$env:JAVA_HOME = $Jdk
$env:PATH = "$Jdk\bin;$env:PATH"

$out = Join-Path $here 'out'
if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Force "$out\classes", "$out\gen", "$out\dex" | Out-Null

# 1. Resources + manifest
& "$BuildTools\aapt2.exe" compile --dir "$here\res" -o "$out\res.zip"; if ($LASTEXITCODE) { throw 'aapt2 compile failed' }
& "$BuildTools\aapt2.exe" link -I $AndroidJar --manifest "$here\AndroidManifest.xml" -o "$out\unsigned.apk" `
    --java "$out\gen" --min-sdk-version 28 --target-sdk-version 34 --version-code 1 --version-name 1.0 "$out\res.zip"
if ($LASTEXITCODE) { throw 'aapt2 link failed' }

# 2. Java -> classes -> dex
$src = Get-ChildItem -Recurse "$here\src", "$out\gen" -Filter *.java | ForEach-Object { $_.FullName }
& javac --release 8 -classpath $AndroidJar -nowarn -d "$out\classes" @src
if ($LASTEXITCODE) { throw 'javac failed' }
$classes = Get-ChildItem -Recurse "$out\classes" -Filter *.class | ForEach-Object { $_.FullName }
& "$BuildTools\d8.bat" --release --min-api 28 --lib $AndroidJar --output "$out\dex" @classes
if ($LASTEXITCODE) { throw 'd8 failed' }

# 3. Add classes.dex to the APK
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [System.IO.Compression.ZipFile]::Open("$out\unsigned.apk", 'Update')
[void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($z, "$out\dex\classes.dex", 'classes.dex')
$z.Dispose()

# 4. Align + sign. Your own key is created on the first build and reused after that,
#    so updates install over the old version. Keep launcher\signing\ private and backed up.
& "$BuildTools\zipalign.exe" -p -f 4 "$out\unsigned.apk" "$out\aligned.apk"; if ($LASTEXITCODE) { throw 'zipalign failed' }
$ks = Join-Path $here 'signing\blackhome.jks'
$pwFile = Join-Path $here 'signing\password.txt'
if (-not (Test-Path $ks)) {
    New-Item -ItemType Directory -Force (Split-Path $ks) | Out-Null
    $pw = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 24 | ForEach-Object { [char]$_ })
    [System.IO.File]::WriteAllText($pwFile, $pw)
    & keytool -genkeypair -keystore $ks -alias blackhome -keyalg RSA -keysize 2048 -validity 10000 `
        -storepass $pw -keypass $pw -dname 'CN=BlackOut Launcher' 2>&1 | Out-Null
    if ($LASTEXITCODE) { throw 'keytool failed' }
}
$pw = Get-Content -Raw $pwFile
& "$BuildTools\apksigner.bat" sign --ks $ks --ks-pass "pass:$pw" --out "$out\BlackOutLauncher.apk" "$out\aligned.apk"
if ($LASTEXITCODE) { throw 'apksigner failed' }
& "$BuildTools\apksigner.bat" verify "$out\BlackOutLauncher.apk"; if ($LASTEXITCODE) { throw 'verify failed' }
"Built: $out\BlackOutLauncher.apk ({0:N0} KB)" -f ((Get-Item "$out\BlackOutLauncher.apk").Length / 1KB)
