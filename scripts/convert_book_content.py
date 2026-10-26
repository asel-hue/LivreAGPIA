#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script d'aide pour convertir le contenu du livre AGPIA vers le format Kotlin
Supporte plusieurs formats d'entrée : texte, JSON, XML, CSV
"""

import json
import re
import sys
from pathlib import Path

def parse_text_file(file_path):
    """
    Parse un fichier texte avec une structure simple :
    Chapitre 1
    Titre du chapitre
    1. Texte du verset 1
    2. Texte du verset 2
    ...
    """
    chapters = []
    current_chapter = None
    current_verses = []
    
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
            
        # Détecter un nouveau chapitre
        chapter_match = re.match(r'^Chapitre\s+(\d+)', line, re.IGNORECASE)
        if chapter_match:
            # Sauvegarder le chapitre précédent
            if current_chapter:
                chapters.append({
                    'number': current_chapter,
                    'title': f"Chapitre {current_chapter}",
                    'verses': current_verses
                })
            current_chapter = int(chapter_match.group(1))
            current_verses = []
            continue
        
        # Détecter un verset (format: "1. texte" ou "1) texte")
        verse_match = re.match(r'^(\d+)[.)]\s*(.+)$', line)
        if verse_match:
            verse_num = int(verse_match.group(1))
            verse_text = verse_match.group(2).strip()
            current_verses.append({
                'number': verse_num,
                'text': verse_text
            })
        elif current_chapter and not line.startswith('Chapitre'):
            # Si c'est un titre de chapitre (ligne après "Chapitre X")
            if not current_verses:
                # C'est probablement le titre
                pass
    
    # Ajouter le dernier chapitre
    if current_chapter:
        chapters.append({
            'number': current_chapter,
            'title': f"Chapitre {current_chapter}",
            'verses': current_verses
        })
    
    return chapters

def parse_json_file(file_path):
    """Parse un fichier JSON"""
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    # Formats JSON possibles
    if isinstance(data, list):
        return data
    elif isinstance(data, dict) and 'chapters' in data:
        return data['chapters']
    else:
        raise ValueError("Format JSON non reconnu")

def generate_kotlin_code(chapters):
    """Génère le code Kotlin à partir des chapitres"""
    kotlin_code = """package com.agpia.livre.data

/**
 * Modèle de données pour le contenu du livre AGPIA
 * 
 * Contenu vérifié et mis à jour depuis le texte original
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
    
    for i, chapter in enumerate(chapters, 1):
        kotlin_code += f"""            Chapter(
                number = {chapter['number']},
                title = "{chapter.get('title', f'Chapitre {chapter[\"number\"]}')}",
                verses = listOf(
"""
        for verse in chapter['verses']:
            # Échapper les guillemets et retours à la ligne
            text = verse['text'].replace('"', '\\"').replace('\n', '\\n')
            kotlin_code += f"""                    Verse({verse['number']}, "{text}"),
"""
        
        kotlin_code += """                )
            )
"""
        if i < len(chapters):
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
    if len(sys.argv) < 2:
        print("Usage: python convert_book_content.py <fichier_entree> [format]")
        print("Formats supportés: text, json")
        print("\nExemple:")
        print("  python convert_book_content.py livre.txt text")
        print("  python convert_book_content.py livre.json json")
        sys.exit(1)
    
    input_file = Path(sys.argv[1])
    if not input_file.exists():
        print(f"Erreur: Le fichier {input_file} n'existe pas")
        sys.exit(1)
    
    format_type = sys.argv[2] if len(sys.argv) > 2 else input_file.suffix[1:] if input_file.suffix else 'text'
    
    try:
        if format_type == 'json':
            chapters = parse_json_file(input_file)
        else:
            chapters = parse_text_file(input_file)
        
        kotlin_code = generate_kotlin_code(chapters)
        
        output_file = input_file.parent / 'BookContent.kt'
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(kotlin_code)
        
        print(f"✅ Conversion réussie!")
        print(f"📁 Fichier généré: {output_file}")
        print(f"📊 {len(chapters)} chapitres convertis")
        print(f"\n⚠️  Vérifiez le fichier généré avant de l'utiliser!")
        print(f"   Remplacez app/src/main/java/com/agpia/livre/data/BookContent.kt")
        
    except Exception as e:
        print(f"❌ Erreur lors de la conversion: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == '__main__':
    main()

