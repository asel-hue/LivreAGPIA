# Script pour arrêter tous les processus Gradle et Java
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Arret des processus Gradle et Java" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Arrêter les processus Gradle
Write-Host "Recherche des processus Gradle..." -ForegroundColor Yellow
$gradleProcesses = Get-Process -Name "gradle*" -ErrorAction SilentlyContinue
if ($gradleProcesses) {
    Write-Host "Processus Gradle trouves:" -ForegroundColor Yellow
    $gradleProcesses | ForEach-Object {
        Write-Host "  - $($_.ProcessName) (PID: $($_.Id))" -ForegroundColor Gray
    }
    Write-Host ""
    $confirmation = Read-Host "Voulez-vous arreter ces processus? (O/N)"
    if ($confirmation -eq "O" -or $confirmation -eq "o" -or $confirmation -eq "Y" -or $confirmation -eq "y") {
        $gradleProcesses | Stop-Process -Force
        Write-Host "✓ Processus Gradle arretes" -ForegroundColor Green
    }
} else {
    Write-Host "Aucun processus Gradle trouve" -ForegroundColor Green
}

Write-Host ""

# Arrêter les processus Java (mais pas tous, seulement ceux liés à Gradle)
Write-Host "Recherche des processus Java (Gradle)..." -ForegroundColor Yellow
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
    $_.Path -like "*gradle*" -or $_.CommandLine -like "*gradle*"
}
if ($javaProcesses) {
    Write-Host "Processus Java (Gradle) trouves:" -ForegroundColor Yellow
    $javaProcesses | ForEach-Object {
        Write-Host "  - Java (PID: $($_.Id))" -ForegroundColor Gray
    }
    Write-Host ""
    $confirmation = Read-Host "Voulez-vous arreter ces processus Java? (O/N)"
    if ($confirmation -eq "O" -or $confirmation -eq "o" -or $confirmation -eq "y" -or $confirmation -eq "Y") {
        $javaProcesses | Stop-Process -Force
        Write-Host "✓ Processus Java arretes" -ForegroundColor Green
    }
} else {
    Write-Host "Aucun processus Java (Gradle) trouve" -ForegroundColor Green
}

Write-Host ""
Write-Host "Attente de 3 secondes pour liberer les fichiers..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Vous pouvez maintenant nettoyer le cache" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Prochaines etapes:" -ForegroundColor Yellow
Write-Host "1. Executer: .\clean_gradle_cache.ps1" -ForegroundColor White
Write-Host "2. OU supprimer manuellement le dossier transforms-3" -ForegroundColor White
Write-Host "3. Rouvrir Android Studio" -ForegroundColor White
Write-Host "4. File -> Sync Project with Gradle Files" -ForegroundColor White

Write-Host ""
Write-Host "Appuyez sur une touche pour quitter..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")


