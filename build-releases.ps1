$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force -Path 'dist' | Out-Null
mvn clean package -Pfree
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Copy-Item 'target/TuTienPlugin-free-7.7.0.jar' 'dist/' -Force
Write-Host 'FREE JAR created in dist/:'
Get-ChildItem 'dist/TuTienPlugin-free-7.7.0.jar' | Format-Table Name, Length
