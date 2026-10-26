#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script pour extraire le contenu des fichiers .txt de l'ancienne version
et générer le code Kotlin pour BookContent.kt
"""

import os
import re
from pathlib import Path

# Mapping des fichiers vers les chapitres dans l'ordre demandé
CHAPTER_MAPPING = [
    ("intro.txt", 1, "Introduction de chaque heure"),
    ("first.txt", 2, "Aube"),
    ("third.txt", 3, "Troisième heure"),
    ("sixth.txt", 4, "Sixième heure"),
    ("ninth.txt", 5, "Neuvième heure"),
    ("eleventh.txt", 6, "Onzième heure"),
    ("twelfth.txt", 7, "Douzième heure"),
    ("safe.txt", 8, "Fermeture du voile"),
    ("midnight.txt", 9, "Prière de minuit"),
    ("other.txt", 10, "Prières diverses"),
]

def clean_text(text):
    """Nettoie le texte en supprimant les balises HTML et en normalisant les espaces"""
    # Supprimer les balises HTML
    text = re.sub(r'<[^>]+>', '', text)
    # Supprimer les balises </p>
    text = text.replace('</p>', '')
    # Normaliser les espaces multiples
    text = re.sub(r'\s+', ' ', text)
    # Nettoyer les sauts de ligne multiples
    text = re.sub(r'\n\s*\n\s*\n+', '\n\n', text)
    return text.strip()

def parse_sections(text):
    """Parse le texte en sections (titre + contenu)"""
    sections = []
    lines = text.split('\n')
    
    current_section = None
    current_content = []
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
            
        # Détecter les titres de sections (lignes courtes, souvent en majuscules ou avec des caractères spéciaux)
        # Les titres sont généralement suivis d'un saut de ligne et d'un contenu
        if (len(line) < 100 and 
            (line.isupper() or 
             line.startswith('Psaume') or 
             line.startswith('Évangile') or
             line.startswith('Epître') or
             line.startswith('Prière') or
             line.startswith('Louange') or
             line.startswith('De la') or
             line.startswith('Acte') or
             line.startswith('Introduction') or
             line.startswith('Conclusion') or
             line.startswith('Absolution') or
             line.startswith('Seconde') or
             line.startswith('Premier') or
             line.startswith('Le ') or
             line.startswith('Les ') or
             line.startswith('Saint') or
             line.startswith('Rend') or
             line.startswith('Nous ') or
             line.startswith('Ô ') or
             line.startswith('Mon ') or
             line.startswith('Tu ') or
             line.startswith('Comme ') or
             line.startswith('Lorsque ') or
             line.startswith('Gloire') or
             ':' in line and len(line.split(':')) == 2)):
            
            # Sauvegarder la section précédente
            if current_section:
                sections.append({
                    'title': current_section,
                    'content': '\n'.join(current_content).strip()
                })
            
            # Nouvelle section
            current_section = line
            current_content = []
        else:
            # Ajouter au contenu
            current_content.append(line)
    
    # Ajouter la dernière section
    if current_section:
        sections.append({
            'title': current_section,
            'content': '\n'.join(current_content).strip()
        })
    
    # Si aucune section n'a été détectée, créer une section unique
    if not sections:
        sections.append({
            'title': '',
            'content': text
        })
    
    return sections

def escape_kotlin_string(text):
    """Échappe les caractères spéciaux pour Kotlin"""
    text = text.replace('\\', '\\\\')
    text = text.replace('"', '\\"')
    text = text.replace('$', '\\$')
    return text

def read_txt_file(file_path):
    """Lit un fichier .txt et retourne son contenu"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return f.read()
    except Exception as e:
        print(f"Erreur lors de la lecture de {file_path}: {e}")
        return ""

def generate_kotlin_code():
    """Génère le code Kotlin pour BookContent.kt"""
    base_path = Path("archive/Agpia/Agpia/Agpia_1.0_151120/res/drawable")
    
    kotlin_code = """package com.agpia.livre.data

/**
 * Modèle de données pour le contenu du livre AGPIA
 * 
 * Contenu extrait depuis les fichiers .txt de l'ancienne version
 */
data class Chapter(
    val number: Int,
    val title: String,
    val sections: List<Section>
)

data class Section(
    val title: String,
    val content: String
)

object BookRepository {
    fun getChapters(): List<Chapter> {
        return listOf(
"""
    
    for filename, chapter_num, chapter_title in CHAPTER_MAPPING:
        file_path = base_path / filename
        if not file_path.exists():
            print(f"⚠️  Fichier non trouvé: {file_path}")
            continue
        
        content = read_txt_file(file_path)
        content = clean_text(content)
        sections = parse_sections(content)
        
        kotlin_code += f"""            Chapter(
                number = {chapter_num},
                title = "{escape_kotlin_string(chapter_title)}",
                sections = listOf(
"""
        
        verse_num = 1
        for section in sections:
            section_title = escape_kotlin_string(section['title'])
            section_content = escape_kotlin_string(section['content'])
            
            kotlin_code += f"""                    Section(
                        title = "{section_title}",
                        content = "{section_content}"
                    ),
"""
            verse_num += 1
        
        kotlin_code += """                )
            ),
"""
    
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

if __name__ == "__main__":
    output_path = Path("app/src/main/java/com/agpia/livre/data/BookContent.kt")
    kotlin_code = generate_kotlin_code()
    
    # Créer le dossier si nécessaire
    output_path.parent.mkdir(parents=True, exist_ok=True)
    
    # Écrire le fichier
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(kotlin_code)
    
    print(f"Code Kotlin genere dans {output_path}")
    print(f"{len(CHAPTER_MAPPING)} chapitres traites")

