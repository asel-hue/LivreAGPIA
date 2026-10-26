# Script pour nettoyer complètement le cache Gradle
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Nettoyage du cache Gradle" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Chemin du cache Gradle
$gradleCachePath = "$env:USERPROFILE\.gradle\caches"

if (Test-Path $gradleCachePath) {
    Write-Host "Cache Gradle trouve: $gradleCachePath" -ForegroundColor Yellow
    Write-Host ""
    
    # Demander confirmation
    $confirmation = Read-Host "Voulez-vous supprimer le cache Gradle? (O/N)"
    if ($confirmation -eq "O" -or $confirmation -eq "o" -or $confirmation -eq "Y" -or $confirmation -eq "y") {
        Write-Host ""
        Write-Host "Suppression du cache Gradle en cours..." -ForegroundColor Yellow
        
        try {
            # Supprimer le dossier transforms-3 qui cause souvent des problèmes
            $transformsPath = Join-Path $gradleCachePath "transforms-3"
            if (Test-Path $transformsPath) {
                Write-Host "Suppression de transforms-3..." -ForegroundColor Gray
                Remove-Item -Path $transformsPath -Recurse -Force -ErrorAction Stop
                Write-Host "✓ transforms-3 supprime" -ForegroundColor Green
            }
            
            # Optionnel : supprimer tout le cache (plus radical)
            Write-Host ""
            $fullClean = Read-Host "Voulez-vous supprimer TOUT le cache Gradle? (plus radical, O/N)"
            if ($fullClean -eq "O" -or $fullClean -eq "o" -or $fullClean -eq "Y" -or $fullClean -eq "y") {
                Write-Host "Suppression de tout le cache Gradle..." -ForegroundColor Yellow
                Remove-Item -Path $gradleCachePath -Recurse -Force -ErrorAction Stop
                Write-Host "✓ Cache Gradle complet supprime" -ForegroundColor Green
            }
            
            Write-Host ""
            Write-Host "========================================" -ForegroundColor Green
            Write-Host "Nettoyage termine avec succes!" -ForegroundColor Green
            Write-Host "========================================" -ForegroundColor Green
            Write-Host ""
            Write-Host "Prochaines etapes:" -ForegroundColor Yellow
            Write-Host "1. Ouvrir Android Studio" -ForegroundColor White
            Write-Host "2. File -> Sync Project with Gradle Files" -ForegroundColor White
            Write-Host "3. Build -> Rebuild Project" -ForegroundColor White
            
        } catch {
            Write-Host ""
            Write-Host "ERREUR: $_" -ForegroundColor Red
            Write-Host ""
            Write-Host "Assurez-vous que:" -ForegroundColor Yellow
            Write-Host "- Android Studio est ferme" -ForegroundColor White
            Write-Host "- Aucun processus Gradle n'est en cours" -ForegroundColor White
            Write-Host "- Vous avez les droits d'administration si necessaire" -ForegroundColor White
        }
    } else {
        Write-Host "Operation annulee." -ForegroundColor Yellow
    }
} else {
    Write-Host "Le cache Gradle n'existe pas a: $gradleCachePath" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Appuyez sur une touche pour quitter..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")


