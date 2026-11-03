param(
 [string]$Jdk = 'C:\Users\x\Documents\Codex\2026-07-10\new-chat\work\android-tools\jdk\jdk-17.0.19+10',
 [string]$Sdk = 'C:\Users\x\Documents\Codex\2026-07-10\new-chat\work\android-tools\sdk',
 [string]$Output = '',
 [string]$ExpectedSignerSha256 = ''
)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
if (!$Output) { $Output = Join-Path $projectRoot 'build\nufe-local.apk' }
$buildRoot = Join-Path $projectRoot 'build'
foreach ($folder in @('gen','classes','dex')) { New-Item -ItemType Directory -Force (Join-Path $buildRoot $folder) | Out-Null }
$toolsRoot = Join-Path $Sdk 'build-tools\35.0.0'
$androidJar = Join-Path $Sdk 'platforms\android-35\android.jar'
function Run-Step([string]$executable, [string[]]$arguments) {
 & $executable @arguments
 if ($LASTEXITCODE -ne 0) { throw "Build failed: $executable (exit $LASTEXITCODE)" }
}
Run-Step (Join-Path $toolsRoot 'aapt.exe') @('package','-f','-m','-J',(Join-Path $buildRoot 'gen'),'-M',(Join-Path $projectRoot 'AndroidManifest.xml'),'-S',(Join-Path $projectRoot 'res'),'-A',(Join-Path $projectRoot 'assets'),'-I',$androidJar,'-F',(Join-Path $buildRoot 'base.apk'))
$javaSources = @(Get-ChildItem (Join-Path $projectRoot 'src'),(Join-Path $buildRoot 'gen') -Filter '*.java' -Recurse | ForEach-Object {$_.FullName})
Run-Step (Join-Path $Jdk 'bin\javac.exe') (@('-encoding','UTF-8','-source','8','-target','8','-bootclasspath',($androidJar+';'+(Join-Path $toolsRoot 'core-lambda-stubs.jar')),'-d',(Join-Path $buildRoot 'classes')) + $javaSources)
Run-Step (Join-Path $Jdk 'bin\jar.exe') @('cf',(Join-Path $buildRoot 'classes.jar'),'-C',(Join-Path $buildRoot 'classes'),'.')
Run-Step (Join-Path $Jdk 'bin\java.exe') @('-cp',(Join-Path $toolsRoot 'lib\d8.jar'),'com.android.tools.r8.D8','--lib',$androidJar,'--min-api','26','--output',(Join-Path $buildRoot 'dex'),(Join-Path $buildRoot 'classes.jar'))
Run-Step (Join-Path $Jdk 'bin\jar.exe') @('uf',(Join-Path $buildRoot 'base.apk'),'-C',(Join-Path $buildRoot 'dex'),'classes.dex')
Run-Step (Join-Path $toolsRoot 'zipalign.exe') @('-f','4',(Join-Path $buildRoot 'base.apk'),(Join-Path $buildRoot 'aligned.apk'))
$keyFile = Join-Path $projectRoot 'local-signing.p12'
if (!(Test-Path -LiteralPath $keyFile)) {
 Run-Step (Join-Path $Jdk 'bin\keytool.exe') @('-genkeypair','-keystore',$keyFile,'-storetype','PKCS12','-storepass','local-build-only','-keypass','local-build-only','-alias','local','-keyalg','RSA','-keysize','2048','-validity','3650','-dname','CN=Local Timetable Development')
}
New-Item -ItemType Directory -Force (Split-Path $Output) | Out-Null
Run-Step (Join-Path $Jdk 'bin\java.exe') @('-jar',(Join-Path $toolsRoot 'lib\apksigner.jar'),'sign','--ks',$keyFile,'--ks-pass','pass:local-build-only','--out',$Output,(Join-Path $buildRoot 'aligned.apk'))
Run-Step (Join-Path $Jdk 'bin\java.exe') @('-jar',(Join-Path $toolsRoot 'lib\apksigner.jar'),'verify','--verbose',$Output)
if ($ExpectedSignerSha256) {
 $certOutput=& (Join-Path $Jdk 'bin/java.exe') -jar (Join-Path $toolsRoot 'lib/apksigner.jar') verify --print-certs $Output
 if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
 $match=[regex]::Match(($certOutput -join [Environment]::NewLine),'Signer #1 certificate SHA-256 digest: *([0-9a-fA-F]{64})')
 if (!$match.Success -or $match.Groups[1].Value.ToLowerInvariant() -ne $ExpectedSignerSha256.ToLowerInvariant()) { throw 'Release signing certificate differs from the original app. Publishing blocked.' }
 Write-Output ('Release signer verified: '+$match.Groups[1].Value)
}
Write-Output "Built: $Output"


