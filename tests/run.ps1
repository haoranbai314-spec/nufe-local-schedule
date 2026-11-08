param([Parameter(Mandatory=$true)][string]$Jdk)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$classes=Join-Path $root 'build/host-tests'
New-Item -ItemType Directory -Force $classes | Out-Null
$names=@('LessonAlarm','UpdateTransfer','WidgetPosition','NotificationState','WidgetBands')
$files=@()
foreach($name in $names){
 $files+=Join-Path $root "src/cn/local/nufeschedule/$name.java"
 $files+=Join-Path $PSScriptRoot "$($name)Test.java"
}
& (Join-Path $Jdk 'bin/javac.exe') -encoding UTF-8 -d $classes @files
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
foreach($name in $names){
 & (Join-Path $Jdk 'bin/java.exe') -cp $classes "cn.local.nufeschedule.$($name)Test"
 if($LASTEXITCODE -ne 0){throw "$name regression failed"}
}

