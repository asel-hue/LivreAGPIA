#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script pour extraire le contenu des fichiers .txt de l'ancienne version
et générer le code Kotlin pour BookContent.kt avec sections
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
    """Nettoie le texte en supprimant les balises HTML et en normalisant"""
    # Supprimer les balises HTML
    text = re.sub(r'<[^>]+>', '', text)
    # Supprimer les balises </p> restantes
    text = text.replace('</p>', '')
    # Normaliser les espaces multiples
    text = re.sub(r'[ \t]+', ' ', text)
    # Nettoyer les sauts de ligne multiples mais garder les doubles
    text = re.sub(r'\n{3,}', '\n\n', text)
    return text.strip()

def parse_sections_simple(text):
    """Parse le texte en sections basé sur les lignes vides et les patterns de titres"""
    sections = []
    lines = text.split('\n')
    
    current_title = None
    current_lines = []
    
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        
        if not line:
            i += 1
            continue
        
        # Détecter un titre potentiel (ligne courte, souvent suivie d'une ligne vide ou d'un contenu)
        is_potential_title = False
        
        # Patterns de titres
        title_patterns = [
            r'^Psaume \d+',
            r'^Évangile',
            r'^Epître',
            r'^Prière',
            r'^Louange',
            r'^De la ',
            r'^Acte de',
            r'^Introduction',
            r'^Conclusion',
            r'^Absolution',
            r'^Seconde ',
            r'^Premier ',
            r'^Le Trisagion',
            r'^Les ',
            r'^Saint',
            r'^Rend',
            r'^Nous ',
            r'^Ô ',
            r'^Mon ',
            r'^Tu ',
            r'^Comme ',
            r'^Lorsque ',
            r'^Gloire',
            r'^Puis on dit',
            r'^Louons',
            r'^Visite',
            r'^A nos',
        ]
        
        # Vérifier si c'est un titre
        for pattern in title_patterns:
            if re.match(pattern, line, re.IGNORECASE):
                is_potential_title = True
                break
        
        # Vérifier aussi si la ligne est courte et suivie d'une ligne vide ou d'un contenu long
        if not is_potential_title and len(line) < 80:
            # Regarder la ligne suivante
            if i + 1 < len(lines):
                next_line = lines[i + 1].strip()
                if not next_line or len(next_line) > 50:
                    is_potential_title = True
        
        if is_potential_title and current_title is not None:
            # Sauvegarder la section précédente
            content = '\n'.join(current_lines).strip()
            if content:
                sections.append({
                    'title': current_title,
                    'content': content
                })
            current_title = line
            current_lines = []
        elif is_potential_title:
            # Nouveau titre
            current_title = line
            current_lines = []
        else:
            # Ajouter au contenu
            current_lines.append(line)
        
        i += 1
    
    # Ajouter la dernière section
    if current_title:
        content = '\n'.join(current_lines).strip()
        if content:
            sections.append({
                'title': current_title,
                'content': content
            })
    
    # Si aucune section n'a été détectée, créer une section unique avec le premier paragraphe comme titre
    if not sections:
        first_line = lines[0].strip() if lines else ""
        rest = '\n'.join(lines[1:]).strip() if len(lines) > 1 else ""
        sections.append({
            'title': first_line if len(first_line) < 100 else "",
            'content': rest if rest else text
        })
    
    return sections

def escape_kotlin_string(text):
    """Échappe les caractères spéciaux pour Kotlin"""
    if not text:
        return ""
    text = text.replace('\\', '\\\\')
    text = text.replace('"', '\\"')
    text = text.replace('$', '\\$')
    text = text.replace('\n', '\\n')
    text = text.replace('\r', '')
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
            print(f"Fichier non trouve: {file_path}")
            continue
        
        print(f"Traitement de {filename}...")
        content = read_txt_file(file_path)
        content = clean_text(content)
        sections = parse_sections_simple(content)
        
        print(f"  -> {len(sections)} sections detectees")
        
        kotlin_code += f"""            Chapter(
                number = {chapter_num},
                title = "{escape_kotlin_string(chapter_title)}",
                sections = listOf(
"""
        
        for section in sections:
            section_title = escape_kotlin_string(section['title'])
            section_content = escape_kotlin_string(section['content'])
            
            kotlin_code += f"""                    Section(
                        title = "{section_title}",
                        content = "{section_content}"
                    ),
"""
        
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


