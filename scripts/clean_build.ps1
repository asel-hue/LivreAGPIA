# Script PowerShell pour nettoyer le dossier build
Write-Host "Nettoyage du dossier build..." -ForegroundColor Yellow

$buildPath = "app\build"
if (Test-Path $buildPath) {
    Write-Host "Suppression du dossier build..." -ForegroundColor Yellow
    try {
        Remove-Item -Path $buildPath -Recurse -Force -ErrorAction Stop
        Write-Host "Dossier build supprime avec succes!" -ForegroundColor Green
    } catch {
        Write-Host "Erreur lors de la suppression: $_" -ForegroundColor Red
        Write-Host "Veuillez fermer Android Studio et reessayer." -ForegroundColor Yellow
    }
} else {
    Write-Host "Le dossier build n'existe pas." -ForegroundColor Green
}

$gradlePath = ".gradle"
if (Test-Path $gradlePath) {
    Write-Host "Suppression du dossier .gradle..." -ForegroundColor Yellow
    try {
        Remove-Item -Path $gradlePath -Recurse -Force -ErrorAction Stop
        Write-Host "Dossier .gradle supprime avec succes!" -ForegroundColor Green
    } catch {
        Write-Host "Impossible de supprimer .gradle: $_" -ForegroundColor Yellow
    }
}

Write-Host "`nNettoyage termine! Vous pouvez maintenant recompiler dans Android Studio." -ForegroundColor Green


