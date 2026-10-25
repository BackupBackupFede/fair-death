<#
.SYNOPSIS
    Runs the in-game behaviour check on every loader x Minecraft line, and prints one line each.

.DESCRIPTION
    A green check here is the only cheap evidence a mixin still applies: Mixin resolves its targets
    at runtime, so compiling proves nothing. NeoForge's runServer ignores stdin, which is why the
    test halts the server itself.

    A world never loads in an older Minecraft, so each line gets its own level-name.

.EXAMPLE
    .\selftest.ps1                       # every loader, every line of MATRIX
    .\selftest.ps1 -Loaders fabric       # one loader
#>
[CmdletBinding()]
param(
    [string[]]$Loaders  = @('fabric', 'neoforge'),
    [string[]]$Versions,
    [string]  $ModId,
    [int]     $TimeoutSeconds = 420
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

if (-not $ModId) {
    $ModId = (Select-String -Path (Join-Path $root 'gradle.properties') -Pattern '^mod_id=(.+)$').Matches.Groups[1].Value
}
if (-not $Versions) {
    $Versions = Select-String -Path (Join-Path $root 'build.gradle') -Pattern '^\s*"([0-9][^"]*)"\s*:\s*\[' -AllMatches |
        ForEach-Object { $_.Matches } | ForEach-Object { $_.Groups[1].Value }
}

$envName = ($ModId.ToUpper() -replace '[^A-Z0-9]', '') + '_SELFTEST'
Write-Host "Self-test: $($Loaders -join ', ') x $($Versions -join ', ')  (env $envName)" -ForegroundColor Cyan

# Rewrites level-name in server.properties. Read and written as UTF-8 WITHOUT a BOM, line by line:
# PowerShell 5.1's Get-Content / Set-Content -Encoding utf8 added a BOM, read it back as ANSI and
# re-encoded it, so a garbage line doubled on every run (2 GB after a few dozen runs, then the server
# ran out of memory before starting). Lines that are neither a comment nor key=value are dropped,
# which also repairs a file already damaged that way.
function Set-LevelName([string]$Path, [string]$Name) {
    $keep = New-Object System.Collections.Generic.List[string]
    $reader = New-Object System.IO.StreamReader($Path, [System.Text.Encoding]::UTF8)
    try {
        while ($null -ne ($line = $reader.ReadLine())) {
            if ($line.Length -gt 1000 -or $line -notmatch '^(#.*|[a-z0-9.-]+=.*)$') { continue }
            if ($line -match '^level-name=') { $line = "level-name=$Name" }
            $keep.Add($line)
        }
    } finally { $reader.Dispose() }
    if (-not ($keep | Where-Object { $_ -match '^level-name=' })) { $keep.Add("level-name=$Name") }
    [System.IO.File]::WriteAllLines($Path, $keep, (New-Object System.Text.UTF8Encoding($false)))
}

$failed = $false
foreach ($version in $Versions) {
    foreach ($loader in $Loaders) {
        $runDir = Join-Path $root "$loader\run"
        $props  = Join-Path $runDir 'server.properties'
        # A fresh clone has no run/ at all, and a server without eula=true stops before the check.
        if (-not (Test-Path $runDir)) { New-Item -ItemType Directory -Path $runDir | Out-Null }
        $eula = Join-Path $runDir 'eula.txt'
        if (-not (Test-Path $eula)) { Set-Content -Path $eula -Value 'eula=true' -Encoding ascii }
        if (Test-Path $props) {
            Set-LevelName $props "world-$version"
        }

        $log = Join-Path $env:TEMP "selftest-$ModId-$loader-$version.log"
        [Environment]::SetEnvironmentVariable($envName, 'true')
        # The server stops itself once the check has run. If a mixin never applied, nothing stops it
        # and the run would hang forever - hence the kill switch. NeoForge's runServer also ignores
        # stdin, so a piped "stop" is not an option.
        $proc = Start-Process -FilePath (Join-Path $root 'gradlew.bat') `
            -ArgumentList ":${loader}:runServer", "-Pminecraft_version=$version", "--console=plain", "-q" `
            -WorkingDirectory $root -NoNewWindow -PassThru `
            -RedirectStandardOutput $log -RedirectStandardError "$log.err"
        if (-not $proc.WaitForExit($TimeoutSeconds * 1000)) {
            Write-Host "  $loader $version : TIMEOUT after ${TimeoutSeconds}s - the server never halted, so the check never ran" -ForegroundColor Red
            try { $proc.Kill($true) } catch { }
            [Environment]::SetEnvironmentVariable($envName, $null)
            $failed = $true
            continue
        }
        [Environment]::SetEnvironmentVariable($envName, $null)

        $result = Select-String -Path $log -Pattern '\[SELFTEST\] (RESULT|.*FAILED)' | ForEach-Object { $_.Line }
        if (-not $result) {
            Write-Host "  $loader $version : NO RESULT - read $log" -ForegroundColor Red
            $failed = $true
        } elseif ($result -match 'PASS') {
            Write-Host "  $loader $version : PASS" -ForegroundColor Green
        } else {
            Write-Host "  $loader $version : $result" -ForegroundColor Red
            $failed = $true
        }
    }
}

if ($failed) { exit 1 }
