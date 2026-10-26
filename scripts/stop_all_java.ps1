# Script pour arrêter tous les processus Java (sauf ceux de Cursor)
Write-Host "Arret des processus Java (Android Studio/Gradle)..." -ForegroundColor Yellow
Write-Host ""

$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
    # Exclure les processus de Cursor
    $_.Path -notlike "*cursor*" -and $_.Path -notlike "*redhat*"
}

if ($javaProcesses) {
    Write-Host "Processus Java trouves (Android Studio/Gradle):" -ForegroundColor Yellow
    $javaProcesses | ForEach-Object {
        Write-Host "  - PID: $($_.Id) - $($_.Path)" -ForegroundColor Gray
    }
    Write-Host ""
    
    $javaProcesses | Stop-Process -Force
    Write-Host "✓ Processus Java arretes" -ForegroundColor Green
    Write-Host ""
    Write-Host "Attente de 5 secondes pour liberer les fichiers..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
} else {
    Write-Host "Aucun processus Java (Android Studio/Gradle) trouve" -ForegroundColor Green
}

Write-Host ""
Write-Host "Vous pouvez maintenant executer: .\clean_gradle_cache.ps1" -ForegroundColor Cyan


