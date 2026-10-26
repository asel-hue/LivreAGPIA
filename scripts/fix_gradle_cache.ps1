# Script pour nettoyer le cache Gradle
Write-Host "Nettoyage du cache Gradle..." -ForegroundColor Yellow

$gradleCachePath = "$env:USERPROFILE\.gradle\caches"
if (Test-Path $gradleCachePath) {
    Write-Host "Suppression du cache Gradle..." -ForegroundColor Yellow
    Write-Host "Chemin: $gradleCachePath" -ForegroundColor Gray
    try {
        # Supprimer seulement le cache transforms qui cause le problème
        $transformsPath = Join-Path $gradleCachePath "transforms-3"
        if (Test-Path $transformsPath) {
            Remove-Item -Path $transformsPath -Recurse -Force -ErrorAction Stop
            Write-Host "Cache transforms supprime avec succes!" -ForegroundColor Green
        } else {
            Write-Host "Le dossier transforms-3 n'existe pas." -ForegroundColor Yellow
        }
    } catch {
        Write-Host "Erreur lors de la suppression: $_" -ForegroundColor Red
        Write-Host "Veuillez fermer Android Studio et reessayer." -ForegroundColor Yellow
    }
} else {
    Write-Host "Le cache Gradle n'existe pas." -ForegroundColor Green
}

Write-Host "`nNettoyage termine! Vous pouvez maintenant recompiler dans Android Studio." -ForegroundColor Green
Write-Host "Si le probleme persiste, essayez:" -ForegroundColor Yellow
Write-Host "1. File -> Invalidate Caches / Restart dans Android Studio" -ForegroundColor Yellow
Write-Host "2. File -> Sync Project with Gradle Files" -ForegroundColor Yellow


