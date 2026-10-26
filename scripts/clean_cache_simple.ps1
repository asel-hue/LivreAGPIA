# Script simple pour nettoyer le cache Gradle
$cachePath = "$env:USERPROFILE\.gradle\caches\transforms-3"

Write-Host "Suppression du cache Gradle..." -ForegroundColor Yellow

if (Test-Path $cachePath) {
    try {
        Remove-Item -Path $cachePath -Recurse -Force
        Write-Host "Cache supprime avec succes!" -ForegroundColor Green
    } catch {
        Write-Host "Erreur: $_" -ForegroundColor Red
        Write-Host "Essayez de supprimer manuellement le dossier:" -ForegroundColor Yellow
        Write-Host $cachePath -ForegroundColor White
    }
} else {
    Write-Host "Le cache n'existe pas deja." -ForegroundColor Green
}


