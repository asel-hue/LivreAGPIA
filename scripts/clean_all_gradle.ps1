# Script pour nettoyer complètement le cache Gradle
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Nettoyage complet du cache Gradle" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Arrêter tous les processus Java/Gradle d'abord
Write-Host "Arret des processus Java/Gradle..." -ForegroundColor Yellow
Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
    $_.Path -notlike "*cursor*" -and $_.Path -notlike "*redhat*"
} | Stop-Process -Force -ErrorAction SilentlyContinue

Get-Process -Name "gradle*" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

Write-Host "Attente de 3 secondes..." -ForegroundColor Yellow
Start-Sleep -Seconds 3
Write-Host ""

# Chemin du cache Gradle
$gradleCachePath = "$env:USERPROFILE\.gradle\caches"

if (Test-Path $gradleCachePath) {
    Write-Host "Suppression du cache Gradle complet..." -ForegroundColor Yellow
    Write-Host "Chemin: $gradleCachePath" -ForegroundColor Gray
    Write-Host ""
    
    try {
        # Supprimer transforms-3
        $transformsPath = Join-Path $gradleCachePath "transforms-3"
        if (Test-Path $transformsPath) {
            Write-Host "Suppression de transforms-3..." -ForegroundColor Gray
            Remove-Item -Path $transformsPath -Recurse -Force -ErrorAction Stop
            Write-Host "✓ transforms-3 supprime" -ForegroundColor Green
        }
        
        # Supprimer aussi le dossier jdks si présent
        $jdksPath = Join-Path $gradleCachePath "jdks"
        if (Test-Path $jdksPath) {
            Write-Host "Suppression de jdks..." -ForegroundColor Gray
            Remove-Item -Path $jdksPath -Recurse -Force -ErrorAction Stop
            Write-Host "✓ jdks supprime" -ForegroundColor Green
        }
        
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "Nettoyage termine avec succes!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        
    } catch {
        Write-Host ""
        Write-Host "ERREUR: $_" -ForegroundColor Red
        Write-Host ""
        Write-Host "Certains fichiers sont peut-etre encore verrouilles." -ForegroundColor Yellow
        Write-Host "Essayez de redemarrer l'ordinateur puis reessayez." -ForegroundColor Yellow
    }
} else {
    Write-Host "Le cache Gradle n'existe pas." -ForegroundColor Green
}

Write-Host ""
Write-Host "Prochaines etapes:" -ForegroundColor Yellow
Write-Host "1. Ouvrir Android Studio" -ForegroundColor White
Write-Host "2. File -> Sync Project with Gradle Files" -ForegroundColor White
Write-Host "3. Build -> Rebuild Project" -ForegroundColor White


