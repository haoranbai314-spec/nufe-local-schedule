param([Parameter(Mandatory=$true)][string]$Jdk)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$classes=Join-Path $root 'build/host-tests'
New-Item -ItemType Directory -Force $classes | Out-Null
& (Join-Path $Jdk 'bin/javac.exe') -encoding UTF-8 -d $classes (Join-Path $root 'src/cn/local/nufeschedule/LessonAlarm.java') (Join-Path $PSScriptRoot 'LessonAlarmTest.java')
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& (Join-Path $Jdk 'bin/java.exe') -cp $classes cn.local.nufeschedule.LessonAlarmTest
if($LASTEXITCODE -ne 0){throw 'Regression tests failed'}
