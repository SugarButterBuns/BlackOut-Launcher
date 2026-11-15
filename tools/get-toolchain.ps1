# Downloads what launcher\build.ps1 needs into .\toolchain (about 300 MB download, 600 MB unpacked):
#   - Eclipse Temurin JDK 17                      (github.com/adoptium)
#   - Android SDK build-tools 34.0.0 for Windows  (dl.google.com)
#   - Android SDK platform 34                     (dl.google.com)
# The two Android packages are covered by the Android SDK License:
#   https://developer.android.com/studio/terms
# Pass -AcceptAndroidSdkLicense to confirm you have read and accept it.
param([switch]$AcceptAndroidSdkLicense)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
if (-not $AcceptAndroidSdkLicense) {
    throw 'Read https://developer.android.com/studio/terms, then run again with -AcceptAndroidSdkLicense'
}

$tc = Join-Path (Split-Path $PSScriptRoot) 'toolchain'
New-Item -ItemType Directory -Force $tc | Out-Null

$jdk = Invoke-RestMethod 'https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse' |
       Where-Object { $_.binary.package.name -like '*.zip' } | Select-Object -First 1
$files = @(
    @{ url = $jdk.binary.package.link; name = 'jdk.zip'; algo = 'SHA256'; hash = $jdk.binary.package.checksum },
    @{ url = 'https://dl.google.com/android/repository/build-tools_r34-windows.zip'; name = 'build-tools.zip'; algo = 'SHA1'; hash = '62cfde1b6fcc3ad12a4d2ba1b537e752768bfd47' },
    @{ url = 'https://dl.google.com/android/repository/platform-34-ext7_r03.zip'; name = 'platform.zip'; algo = 'SHA1'; hash = '1f2e9478d6a7601425ceaa553311dc43191f103d' }
)
foreach ($f in $files) {
    $zip = Join-Path $tc $f.name
    "Downloading $($f.url)"
    Invoke-WebRequest $f.url -OutFile $zip -UseBasicParsing
    $h = (Get-FileHash $zip -Algorithm $f.algo).Hash.ToLower()
    if ($h -ne $f.hash.ToLower()) { Remove-Item $zip; throw "Checksum mismatch for $($f.name)" }
    Expand-Archive $zip -DestinationPath $tc -Force
    Remove-Item $zip
}
"Toolchain ready in $tc"
