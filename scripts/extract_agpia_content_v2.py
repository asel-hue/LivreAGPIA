#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script amélioré pour extraire le contenu complet de l'AGPIA depuis les fichiers HTML
"""

import re
import html
from pathlib import Path

def remove_html_tags(text):
    """Supprime toutes les balises HTML d'un texte"""
    # Supprimer les balises HTML
    text = re.sub(r'<[^>]+>', '', text)
    # Décoder les entités HTML
    text = html.unescape(text)
    return text

def clean_text(text):
    """Nettoie le texte"""
    if not text:
        return ""
    # Supprimer les balises HTML
    text = remove_html_tags(text)
    # Remplacer les <br> et <br/> par des retours à la ligne
    text = re.sub(r'<br\s*/?>', '\n', text, flags=re.IGNORECASE)
    # Remplacer les retours à la ligne multiples par doubles
    text = re.sub(r'\n\s*\n\s*\n+', '\n\n', text)
    # Nettoyer les espaces multiples
    text = re.sub(r'[ \t]+', ' ', text)
    # Supprimer les espaces en début et fin de ligne
    lines = [line.strip() for line in text.split('\n')]
    text = '\n'.join(lines)
    # Supprimer les espaces en début et fin
    text = text.strip()
    return text

def extract_content_from_html(html_file):
    """Extrait le contenu complet d'un fichier HTML de l'AGPIA"""
    with open(html_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Extraire le contenu de la div principale
    main_match = re.search(r'<div\s+class=["\']main["\'][^>]*>(.*?)</div>', content, re.DOTALL | re.IGNORECASE)
    if not main_match:
        return None
    
    main_content = main_match.group(1)
    
    # Extraire le titre principal (h2 ou premier h3)
    title = ""
    h2_match = re.search(r'<h2[^>]*>(.*?)</h2>', main_content, re.DOTALL | re.IGNORECASE)
    if h2_match:
        title = clean_text(h2_match.group(1))
    else:
        # Chercher le premier h3 qui n'est pas en italique
        h3_matches = re.finditer(r'<h3[^>]*>(.*?)</h3>', main_content, re.DOTALL | re.IGNORECASE)
        for match in h3_matches:
            h3_text = clean_text(match.group(1))
            if h3_text and not h3_text.startswith('Introduction'):
                title = h3_text
                break
        if not title:
            # Utiliser le premier h3 même s'il est en italique
            first_h3 = re.search(r'<h3[^>]*>(.*?)</h3>', main_content, re.DOTALL | re.IGNORECASE)
            if first_h3:
                title = clean_text(first_h3.group(1))
    
    # Si pas de titre trouvé, utiliser le nom du fichier
    if not title:
        hour_num = html_file.stem.replace('hour', '')
        title = f"Prière de l'heure {hour_num}"
    
    # Extraire tout le contenu textuel en préservant la structure
    # On va extraire section par section
    
    # Supprimer les scripts et styles
    main_content = re.sub(r'<script[^>]*>.*?</script>', '', main_content, flags=re.DOTALL | re.IGNORECASE)
    main_content = re.sub(r'<style[^>]*>.*?</style>', '', main_content, flags=re.DOTALL | re.IGNORECASE)
    
    # Extraire les sections dans l'ordre
    sections = []
    
    # Diviser par les balises de structure (h2, h3, h4)
    parts = re.split(r'(<h[234][^>]*>.*?</h[234]>)', main_content, flags=re.DOTALL | re.IGNORECASE)
    
    current_section_title = None
    current_section_content = []
    
    for part in parts:
        if not part.strip():
            continue
        
        # Vérifier si c'est un titre
        title_match = re.match(r'<h([234])[^>]*>(.*?)</h\1>', part, re.DOTALL | re.IGNORECASE)
        if title_match:
            # Sauvegarder la section précédente
            if current_section_title or current_section_content:
                section_text = '\n\n'.join(current_section_content) if current_section_content else ''
                if section_text.strip():
                    sections.append({
                        'title': current_section_title,
                        'content': section_text
                    })
            
            # Nouvelle section
            current_section_title = clean_text(title_match.group(2))
            current_section_content = []
        else:
            # Extraire le texte de ce bloc
            # Supprimer les balises de boutons et autres éléments interactifs
            part = re.sub(r'<button[^>]*>.*?</button>', '', part, flags=re.DOTALL | re.IGNORECASE)
            part = re.sub(r'<span[^>]*>', '', part, flags=re.IGNORECASE)
            part = re.sub(r'</span>', '', part, flags=re.IGNORECASE)
            part = re.sub(r'<center[^>]*>', '', part, flags=re.IGNORECASE)
            part = re.sub(r'</center>', '', part, flags=re.IGNORECASE)
            
            # Extraire les paragraphes
            paragraphs = re.findall(r'<p[^>]*>(.*?)</p>', part, re.DOTALL | re.IGNORECASE)
            for para in paragraphs:
                para_text = clean_text(para)
                if para_text and para_text not in current_section_content:
                    current_section_content.append(para_text)
            
            # Extraire aussi le texte direct (sans balises p)
            direct_text = re.sub(r'<[^>]+>', '', part)
            direct_text = clean_text(direct_text)
            if direct_text and direct_text not in current_section_content and len(direct_text) > 10:
                current_section_content.append(direct_text)
    
    # Ajouter la dernière section
    if current_section_title or current_section_content:
        section_text = '\n\n'.join(current_section_content) if current_section_content else ''
        if section_text.strip():
            sections.append({
                'title': current_section_title,
                'content': section_text
            })
    
    # Construire le texte complet
    full_text_parts = []
    for section in sections:
        if section['title']:
            full_text_parts.append(section['title'])
        if section['content']:
            full_text_parts.append(section['content'])
        full_text_parts.append('')  # Séparateur entre sections
    
    full_text = '\n\n'.join([p for p in full_text_parts if p.strip()])
    
    return {
        'title': title,
        'content': full_text,
        'sections': sections
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
        paragraphs = [p.strip() for p in content.split('\n\n') if p.strip() and len(p.strip()) > 5]
        
        # Si pas assez de paragraphes, diviser par simple saut de ligne
        if len(paragraphs) < 2:
            paragraphs = [p.strip() for p in content.split('\n') if p.strip() and len(p.strip()) > 5]
        
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
            if len(paragraph) > 200 or '\n' in paragraph:
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
    
    print("Extraction du contenu de l'AGPIA (version amelioree)...\n")
    
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
                num_paragraphs = len([p for p in content['content'].split('\n\n') if p.strip()])
                print(f"     OK: {content['title']} ({len(content['content'])} caracteres, ~{num_paragraphs} paragraphes)")
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
    total_verses = sum(len([p for p in ch['content'].split('\n\n') if p.strip()]) for ch in chapters_data)
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


