-$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location -LiteralPath $scriptDir

$platforms = @("win", "linux", "macos")
$selected = 0

try {
    [Console]::CursorVisible = $false

    while ($true) {
        Clear-Host
        Write-Host "选择构建平台（↑/↓，回车确认）："
        Write-Host

        for ($index = 0; $index -lt $platforms.Count; $index++) {
            if ($index -eq $selected) {
                Write-Host "  > $($platforms[$index])"
            } else {
                Write-Host "    $($platforms[$index])"
            }
        }

        $key = [Console]::ReadKey($true)
        switch ($key.Key) {
            UpArrow {
                $selected = ($selected - 1 + $platforms.Count) % $platforms.Count
            }
            DownArrow {
                $selected = ($selected + 1) % $platforms.Count
            }
            Enter {
                break
            }
            Escape {
                Write-Host "`n已取消。"
                exit 130
            }
        }

        if ($key.Key -eq [ConsoleKey]::Enter) {
            break
        }
    }
    $platform = $platforms[$selected]
    $target = switch ($platform) {
        win   { @{ GOOS = "windows"; Extension = ".exe" } }
        linux { @{ GOOS = "linux"; Extension = "" } }
        macos { @{ GOOS = "darwin"; Extension = "" } }
    }

    $goarch = if ($env:GOARCH) { $env:GOARCH } else { "amd64" }
    $outputDir = Join-Path $scriptDir "build\$platform"
    $outputFile = Join-Path $outputDir "server$($target.Extension)"
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

    Write-Host "`n正在构建 $($target.GOOS)/$goarch..."
    $env:GOOS = $target.GOOS
    $env:GOARCH = $goarch
    $env:CGO_ENABLED = "0"
    & go build -trimpath -o $outputFile .
    if ($LASTEXITCODE -ne 0) {
        throw "Go 构建失败，退出码：$LASTEXITCODE"
    }

    Write-Host "构建完成：$outputFile"
} finally {
    [Console]::CursorVisible = $true
    Pop-Location
}
