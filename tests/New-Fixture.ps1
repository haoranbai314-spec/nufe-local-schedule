param([string]$Output='fixture.json',[datetime]$Now=([TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTime]::UtcNow,'China Standard Time')))

if($now.Hour -ge 22){throw '请在上海时间 22:00 前生成，避免样例跨日。'}
$day=if([int]$now.DayOfWeek -eq 0){7}else{[int]$now.DayOfWeek}
$monday=$now.Date.AddDays(1-$day).ToString('yyyy-MM-dd')
$activities=@()
foreach($offset in @(10,35,60)){
 $start=$now.AddMinutes($offset)
 $activities+=@{courseName="锁屏回归-$offset";weekday=$day;startUnit=1;endUnit=1;startTime=$start.ToString('HH:mm');endTime=$start.AddMinutes(15).ToString('HH:mm');room='测试教室';weekIndexes=@(1)}
}
@{semester_name='锁屏独立显示回归';week1_monday=$monday;activities=$activities} | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 $Output
Write-Output "Generated $Output (Asia/Shanghai)"
