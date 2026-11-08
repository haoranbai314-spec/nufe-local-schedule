param(
 [Parameter(Mandatory=$true)][string]$Repository,
 [Parameter(Mandatory=$true)][string]$NotesFile
)
$ErrorActionPreference='Stop'
if ($Repository -notmatch '^[A-Za-z0-9-]+/[A-Za-z0-9_.-]+$') { throw 'Use owner/repository.' }
if (!(Test-Path -LiteralPath $NotesFile)) { throw 'Release notes file not found.' }
& gh auth status
if ($LASTEXITCODE -ne 0) { throw 'Please run gh auth login first.' }
[xml]$manifest=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'AndroidManifest.xml') -Raw
$version=$manifest.manifest.versionName
$staging=Join-Path $PSScriptRoot ('build/release-'+$version)
New-Item -ItemType Directory -Path $staging -Force | Out-Null
$apk=Join-Path $staging 'nufe-local.apk'
$expectedSigner=(Get-Content -LiteralPath (Join-Path $PSScriptRoot 'release-signing.sha256') -Raw).Trim()
& (Join-Path $PSScriptRoot 'build.ps1') -Output $apk -ExpectedSignerSha256 $expectedSigner
if (!$?) { throw 'Build failed.' }
# Publish editable GPL source, never the private signing key, cache, or local data.
$source=Join-Path $staging 'nufe-local-source.zip'
if (Test-Path -LiteralPath $source) { Remove-Item -LiteralPath $source }
$files=@('src','res','assets','tests','AndroidManifest.xml','build.ps1','Publish-Update.ps1','release-signing.sha256','README.txt','README.md','UPDATES.txt','LICENSE') | ForEach-Object { Join-Path $PSScriptRoot $_ }
Compress-Archive -LiteralPath $files -DestinationPath $source
# API-served backup download, independent from release asset redirects.
function Invoke-GitJson($endpoint,$payload) {
 $jsonFile=Join-Path $staging 'github-request.json'
 $json=$payload | ConvertTo-Json -Depth 8 -Compress
 [IO.File]::WriteAllText($jsonFile,$json,(New-Object Text.UTF8Encoding($false)))
 $result=& gh api --method POST $endpoint --input $jsonFile
 if ($LASTEXITCODE -ne 0) { throw 'Backup upload failed.' }
 return ($result | ConvertFrom-Json)
}
$blob=Invoke-GitJson "repos/$Repository/git/blobs" @{encoding='base64';content=[Convert]::ToBase64String([IO.File]::ReadAllBytes($apk))}
$tree=Invoke-GitJson "repos/$Repository/git/trees" @{tree=@(@{path='nufe-local.apk';mode='100644';type='blob';sha=$blob.sha})}
$commit=Invoke-GitJson "repos/$Repository/git/commits" @{message="Latest download: $version";tree=$tree.sha}
$branch=& gh api "repos/$Repository/branches" --jq '.[].name'
if ($branch -contains 'download') {
 & gh api --method PATCH "repos/$Repository/git/refs/heads/download" -f ("sha="+$commit.sha) -F force=true --silent
 if ($LASTEXITCODE -ne 0) { throw 'Backup reference update failed.' }
} else {
 $null=Invoke-GitJson "repos/$Repository/git/refs" @{ref='refs/heads/download';sha=$commit.sha}
}
& gh release create ('v'+$version) $apk $source --repo $Repository --title ('南财课表·本地 '+$version) --notes-file $NotesFile --latest
if ($LASTEXITCODE -ne 0) { throw 'Publishing failed. Check release tag and GitHub connection.' }





