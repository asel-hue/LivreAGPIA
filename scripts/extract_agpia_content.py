#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script pour extraire le contenu de l'AGPIA depuis les fichiers HTML
et le convertir en format Kotlin pour l'application Android
"""

import re
import html
from pathlib import Path

def clean_text(text):
    """Nettoie le texte HTML en texte brut"""
    if not text:
        return ""
    # Décoder les entités HTML
    text = html.unescape(text)
    # Remplacer les retours à la ligne multiples par des espaces simples
    text = re.sub(r'\s+', ' ', text)
    # Supprimer les espaces en début et fin
    text = text.strip()
    return text

def extract_text_from_tag(content, tag_name):
    """Extrait le texte d'une balise HTML"""
    pattern = f'<{tag_name}[^>]*>(.*?)</{tag_name}>'
    matches = re.findall(pattern, content, re.DOTALL | re.IGNORECASE)
    return [clean_text(match) for match in matches if match.strip()]

def extract_content_from_html(html_file):
    """Extrait le contenu d'un fichier HTML de l'AGPIA"""
    with open(html_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Extraire le contenu de la div principale
    main_match = re.search(r'<div\s+class=["\']main["\'][^>]*>(.*?)</div>', content, re.DOTALL | re.IGNORECASE)
    if not main_match:
        return None
    
    main_content = main_match.group(1)
    
    # Extraire le titre principal (h2 ou h3)
    title = ""
    h2_match = re.search(r'<h2[^>]*>(.*?)</h2>', main_content, re.DOTALL | re.IGNORECASE)
    if h2_match:
        title = clean_text(h2_match.group(1))
    else:
        h3_match = re.search(r'<h3[^>]*>(.*?)</h3>', main_content, re.DOTALL | re.IGNORECASE)
        if h3_match:
            title = clean_text(h3_match.group(1))
    
    # Extraire tous les paragraphes
    paragraphs = extract_text_from_tag(main_content, 'p')
    
    # Extraire les psaumes (dans les divs avec class psalm-text)
    psalm_pattern = r'<div\s+class=["\']psalm-text["\'][^>]*>(.*?)</div>'
    psalm_matches = re.findall(psalm_pattern, main_content, re.DOTALL | re.IGNORECASE)
    for psalm_text in psalm_matches:
        text = clean_text(psalm_text)
        if text and text not in paragraphs:
            paragraphs.append(text)
    
    # Extraire les titres de section (h3, h4)
    h3_titles = extract_text_from_tag(main_content, 'h3')
    h4_titles = extract_text_from_tag(main_content, 'h4')
    
    # Construire le texte complet en préservant la structure
    full_text_parts = []
    
    # Ajouter le titre principal
    if title:
        full_text_parts.append(title)
        full_text_parts.append("")
    
    # Parcourir le contenu pour préserver l'ordre
    lines = main_content.split('\n')
    current_section = []
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
        
        # Détecter les titres
        h3_match = re.search(r'<h3[^>]*>(.*?)</h3>', line, re.IGNORECASE)
        h4_match = re.search(r'<h4[^>]*>(.*?)</h4>', line, re.IGNORECASE)
        
        if h3_match:
            if current_section:
                full_text_parts.append('\n'.join(current_section))
                full_text_parts.append("")
            current_section = []
            full_text_parts.append(clean_text(h3_match.group(1)))
            full_text_parts.append("")
        elif h4_match:
            if current_section:
                full_text_parts.append('\n'.join(current_section))
                full_text_parts.append("")
            current_section = []
            full_text_parts.append(clean_text(h4_match.group(1)))
            full_text_parts.append("")
        else:
            # Extraire le texte des paragraphes
            p_match = re.search(r'<p[^>]*>(.*?)</p>', line, re.DOTALL | re.IGNORECASE)
            if p_match:
                text = clean_text(p_match.group(1))
                if text:
                    current_section.append(text)
            else:
                # Extraire le texte des divs psalm-text
                psalm_match = re.search(r'<div\s+class=["\']psalm-text["\'][^>]*>(.*?)</div>', line, re.DOTALL | re.IGNORECASE)
                if psalm_match:
                    text = clean_text(psalm_match.group(1))
                    if text:
                        current_section.append(text)
    
    if current_section:
        full_text_parts.append('\n'.join(current_section))
    
    full_text = '\n\n'.join([p for p in full_text_parts if p.strip()])
    
    return {
        'title': title or f"Prière de l'heure {html_file.stem.replace('hour', '')}",
        'content': full_text
    }

def escape_kotlin_string(text):
    """Échappe une chaîne pour Kotlin"""
    # Remplacer les backslashes d'abord
    text = text.replace('\\', '\\\\')
    # Remplacer les guillemets
    text = text.replace('"', '\\"')
    # Remplacer les $ (caractère spécial Kotlin dans les strings)
    text = text.replace('$', '\\$')
    return text

def generate_kotlin_code(chapters_data):
    """Génère le code Kotlin à partir des données des chapitres"""
    
    kotlin_code = """package com.agpia.livre.data

/**
 * Modèle de données pour le contenu du livre AGPIA
 * 
 * Contenu extrait et vérifié depuis les fichiers HTML de l'ancienne version
 * Mis à jour pour conformité avec le texte original
 */
data class Chapter(
    val number: Int,
    val title: String,
    val verses: List<Verse>
)

data class Verse(
    val number: Int,
    val text: String
)

object BookRepository {
    fun getChapters(): List<Chapter> {
        return listOf(
"""
    
    for i, chapter_data in enumerate(chapters_data, 1):
        title = chapter_data['title']
        content = chapter_data['content']
        
        # Diviser le contenu en "versets" (paragraphes séparés par double saut de ligne)
        paragraphs = [p.strip() for p in content.split('\n\n') if p.strip()]
        
        # Si pas de paragraphes, diviser par simple saut de ligne
        if not paragraphs:
            paragraphs = [p.strip() for p in content.split('\n') if p.strip()]
        
        # Si toujours vide, utiliser le contenu entier
        if not paragraphs:
            paragraphs = [content] if content else ["Contenu non disponible"]
        
        kotlin_code += f"""            Chapter(
                number = {i},
                title = "{escape_kotlin_string(title)}",
                verses = listOf(
"""
        
        for j, paragraph in enumerate(paragraphs, 1):
            # Échapper les caractères spéciaux pour Kotlin
            escaped_text = escape_kotlin_string(paragraph)
            # Utiliser des triple guillemets pour les textes longs
            if len(paragraph) > 300 or '\n' in paragraph:
                # Pour les textes très longs, utiliser des triple guillemets
                kotlin_code += f"""                    Verse({j}, \"\"\"{paragraph}\"\"\"),
"""
            else:
                kotlin_code += f"""                    Verse({j}, "{escaped_text}"),
"""
        
        kotlin_code += """                )
            )
"""
        if i < len(chapters_data):
            kotlin_code += "\n"
    
    kotlin_code += """        )
    }
    
    fun getChapter(chapterNumber: Int): Chapter? {
        return getChapters().find { it.number == chapterNumber }
    }
    
    fun getTotalChapters(): Int {
        return getChapters().size
    }
}
"""
    
    return kotlin_code

def main():
    import sys
    import io
    
    # Forcer l'encodage UTF-8 pour la sortie
    if sys.stdout.encoding != 'utf-8':
        sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    
    # Chemin vers les fichiers HTML
    assets_dir = Path('Agpia/Agpia/Agpia_1.0_151120/assets')
    
    if not assets_dir.exists():
        print(f"ERREUR: Le repertoire {assets_dir} n'existe pas")
        return
    
    # Liste des fichiers HTML dans l'ordre
    html_files = [
        'hour1.html',  # Introduction
        'hour2.html',  # Priere de l'aube
        'hour3.html',  # Troisieme heure
        'hour4.html',  # Sixieme heure
        'hour5.html',  # Neuvieme heure
        'hour6.html',  # Onzieme heure
        'hour7.html',  # Douzieme heure
        'hour8.html',  # Fermeture du voile
        'hour9.html',  # Minuit
        'hour10.html', # Autres prieres
    ]
    
    chapters_data = []
    
    print("Extraction du contenu de l'AGPIA...\n")
    
    for html_file in html_files:
        file_path = assets_dir / html_file
        if not file_path.exists():
            print(f"  ATTENTION: Fichier non trouve: {html_file}")
            continue
        
        print(f"  Traitement de {html_file}...")
        try:
            content = extract_content_from_html(file_path)
            if content and content['content']:
                chapters_data.append(content)
                print(f"     OK: {content['title']} ({len(content['content'])} caracteres)")
            else:
                print(f"     ERREUR: Aucun contenu extrait")
        except Exception as e:
            print(f"     ERREUR: {e}")
            import traceback
            traceback.print_exc()
    
    if not chapters_data:
        print("\nERREUR: Aucun contenu n'a pu etre extrait")
        return
    
    print(f"\nOK: {len(chapters_data)} chapitres extraits\n")
    
    # Generer le code Kotlin
    print("Generation du code Kotlin...")
    kotlin_code = generate_kotlin_code(chapters_data)
    
    # Ecrire le fichier
    output_file = Path('app/src/main/java/com/agpia/livre/data/BookContent.kt')
    output_file.parent.mkdir(parents=True, exist_ok=True)
    
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(kotlin_code)
    
    print(f"OK: Fichier genere: {output_file}")
    print(f"OK: {len(chapters_data)} chapitres convertis")
    
    # Afficher un resume
    total_verses = sum(len(ch['content'].split('\n\n')) for ch in chapters_data)
    print(f"OK: Environ {total_verses} paragraphes/versets extraits")
    print(f"\nATTENTION: Verifiez le fichier genere avant de compiler l'application!")
    print(f"   Assurez-vous que le contenu correspond au texte original du livre AGPIA")

if __name__ == '__main__':
    try:
        main()
    except Exception as e:
        print(f"ERREUR: {e}")
        import traceback
        traceback.print_exc()
