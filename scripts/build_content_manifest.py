#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
build_content_manifest.py — valide le contenu du livre et (ré)écrit app/src/main/assets/book/manifest.json.

À lancer AVANT CHAQUE `git push` qui modifie du contenu (app/src/main/assets/book/<lang>/chapter*.json) :

    python scripts/build_content_manifest.py            # valide, recalcule sha256/tailles, contentVersion += 1, updatedAt = aujourd'hui
    python scripts/build_content_manifest.py --no-bump  # idem sans incrémenter contentVersion (ex. correction de typo déjà publiée ?)
    python scripts/build_content_manifest.py --check    # valide seulement (aucune écriture) ; code retour 1 si le contenu
                                                        # est invalide ou si le manifeste n'est plus à jour (hash/taille/fichiers)

Ce que le script vérifie, pour chaque dossier de langue <lang>/ :
  - fichiers chapter1.json … chapterN.json en UTF-8 sans BOM, JSON valide ;
  - schéma strict : {"number","title","sections":[{"id","title","content"[, "kind"[, "groupEnd"]]}]} ;
    "kind" ∈ {"group","psalm"} ; "groupEnd" (entier, id d'une section du chapitre, > id) uniquement si kind == "group" ;
  - "number" = numéro du fichier, ids contigus 1..n, titres non vides ("content" peut être vide : ex. Kyrie) ;
  - entre toutes les langues : même nombre de chapitres, mêmes comptes de sections, mêmes ids, mêmes kind / groupEnd.

Manifeste produit (schemaVersion 1) :
  { "schemaVersion", "contentVersion", "updatedAt", "chapterCount", "languages": [...], "files": { "fr/chapter1.json": {"sha256", "size"}, ... } }
La liste "languages" existante est CONSERVÉE telle quelle (ne pas la régénérer à la main dans le script) ; si un nouveau
dossier de langue apparaît sans entrée, une entrée minimale est ajoutée (à compléter : name, nativeName, rtl, uiLocale)
et un avertissement est affiché. "uiLocale" = locale d'interface à appliquer quand la langue est la langue principale.

Python 3 standard uniquement (aucune dépendance). Chemins relatifs à la racine du dépôt, où que soit lancé le script.
"""
import argparse
import datetime
import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BOOK_DIR = ROOT / 'app' / 'src' / 'main' / 'assets' / 'book'
MANIFEST = BOOK_DIR / 'manifest.json'
SCHEMA_VERSION = 1
CHAPTER_RE = re.compile(r'^chapter(\d+)\.json$')
KINDS = {'group', 'psalm'}
DEFAULT_LANGUAGES = [
    {'code': 'fr', 'name': 'Français', 'nativeName': 'Français', 'rtl': False, 'uiLocale': 'fr'},
    {'code': 'ar', 'name': 'Arabe', 'nativeName': 'العربية', 'rtl': True, 'uiLocale': 'ar'},
]


class ContentError(Exception):
    pass


def load_chapter(path: Path, expected_number: int) -> dict:
    raw = path.read_bytes()
    if raw.startswith(b'\xef\xbb\xbf'):
        raise ContentError(f'{path.name} : BOM UTF-8 interdit')
    try:
        data = json.loads(raw.decode('utf-8'))
    except (UnicodeDecodeError, json.JSONDecodeError) as e:
        raise ContentError(f'{path.name} : JSON/UTF-8 invalide ({e})')
    if not isinstance(data, dict) or set(data) != {'number', 'title', 'sections'}:
        raise ContentError(f'{path.name} : clés attendues number/title/sections, trouvé {sorted(data) if isinstance(data, dict) else type(data).__name__}')
    if data['number'] != expected_number:
        raise ContentError(f'{path.name} : "number" = {data["number"]!r}, attendu {expected_number}')
    if not isinstance(data['title'], str) or not data['title'].strip():
        raise ContentError(f'{path.name} : "title" vide')
    secs = data['sections']
    if not isinstance(secs, list) or not secs:
        raise ContentError(f'{path.name} : "sections" doit être une liste non vide')
    ids = set()
    for k, s in enumerate(secs, 1):
        where = f'{path.name} section #{k}'
        if not isinstance(s, dict):
            raise ContentError(f'{where} : objet attendu')
        extra = set(s) - {'id', 'title', 'content', 'kind', 'groupEnd'}
        missing = {'id', 'title', 'content'} - set(s)
        if extra or missing:
            raise ContentError(f'{where} : clés en trop {sorted(extra)} / manquantes {sorted(missing)}')
        if s['id'] != k:
            raise ContentError(f'{where} : id {s["id"]!r}, attendu {k} (ids contigus 1..n)')
        if not isinstance(s['title'], str) or not s['title'].strip():
            raise ContentError(f'{where} : titre vide')
        if not isinstance(s['content'], str):
            raise ContentError(f'{where} : "content" doit être une chaîne')
        if 'kind' in s and s['kind'] not in KINDS:
            raise ContentError(f'{where} : kind {s["kind"]!r} inconnu (attendu {sorted(KINDS)})')
        if 'groupEnd' in s:
            if s.get('kind') != 'group':
                raise ContentError(f'{where} : "groupEnd" réservé aux sections kind="group"')
            if not isinstance(s['groupEnd'], int) or isinstance(s['groupEnd'], bool) or not (k < s['groupEnd'] <= len(secs)):
                raise ContentError(f'{where} : groupEnd {s["groupEnd"]!r} doit être un id du chapitre > {k}')
        ids.add(k)
    return data


def structure(data: dict):
    """Signature structurelle d'un chapitre, à comparer entre langues."""
    return [(s['id'], s.get('kind'), s.get('groupEnd')) for s in data['sections']]


def scan_languages() -> dict:
    """{lang: {num: Path}} pour chaque dossier de langue contenant des chapter*.json."""
    langs = {}
    for d in sorted(p for p in BOOK_DIR.iterdir() if p.is_dir()):
        files = {}
        for f in d.iterdir():
            m = CHAPTER_RE.match(f.name)
            if m:
                files[int(m.group(1))] = f
        if files:
            langs[d.name] = files
    if not langs:
        raise ContentError(f'aucun dossier de langue avec chapter*.json dans {BOOK_DIR}')
    return langs


def validate(langs: dict) -> int:
    """Valide tout le contenu ; retourne le nombre de chapitres."""
    counts = {lang: sorted(files) for lang, files in langs.items()}
    ref_lang = sorted(langs)[0]
    ref_nums = counts[ref_lang]
    if ref_nums != list(range(1, len(ref_nums) + 1)):
        raise ContentError(f'{ref_lang}/ : numéros de chapitres non contigus {ref_nums}')
    for lang, nums in counts.items():
        if nums != ref_nums:
            raise ContentError(f'{lang}/ a les chapitres {nums}, {ref_lang}/ a {ref_nums}')
    for num in ref_nums:
        ref = None
        for lang in sorted(langs):
            data = load_chapter(langs[lang][num], num)
            sig = structure(data)
            if ref is None:
                ref = (lang, sig)
            elif sig != ref[1]:
                diff = next((i for i, (a, b) in enumerate(zip(ref[1], sig)) if a != b), min(len(sig), len(ref[1])))
                raise ContentError(f'chapter{num}.json : structure {lang}/ ≠ {ref[0]}/ (sections {len(sig)} vs {len(ref[1])}, '
                                   f'première différence à l\'index {diff}: {sig[diff] if diff < len(sig) else None} vs {ref[1][diff] if diff < len(ref[1]) else None})')
    return len(ref_nums)


def file_entries(langs: dict) -> dict:
    entries = {}
    for lang in sorted(langs):
        for num in sorted(langs[lang]):
            p = langs[lang][num]
            raw = p.read_bytes()
            entries[f'{lang}/chapter{num}.json'] = {'sha256': hashlib.sha256(raw).hexdigest(), 'size': len(raw)}
    return entries


def read_manifest():
    if not MANIFEST.exists():
        return None
    raw = MANIFEST.read_bytes()
    if raw.startswith(b'\xef\xbb\xbf'):
        raise ContentError('manifest.json : BOM UTF-8 interdit')
    return json.loads(raw.decode('utf-8'))


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description='Valide le contenu du livre et met à jour manifest.json (voir en-tête du fichier).')
    ap.add_argument('--check', action='store_true', help='valider seulement, sans écrire (code 1 si invalide ou manifeste obsolète)')
    ap.add_argument('--no-bump', action='store_true', help="ne pas incrémenter contentVersion")
    args = ap.parse_args(argv)

    try:
        langs = scan_languages()
        chapter_count = validate(langs)
        entries = file_entries(langs)
        old = read_manifest()
    except ContentError as e:
        print(f'ERREUR : {e}', file=sys.stderr)
        return 1

    for lang in sorted(langs):
        print(f'  {lang}/ : {len(langs[lang])} chapitres, '
              f'{sum(len(load_chapter(p, n)["sections"]) for n, p in langs[lang].items())} sections')

    languages = list(old['languages']) if old and isinstance(old.get('languages'), list) else list(DEFAULT_LANGUAGES)
    known = {l.get('code') for l in languages}
    for lang in sorted(langs):
        if lang not in known:
            languages.append({'code': lang, 'name': lang, 'nativeName': lang, 'rtl': False, 'uiLocale': 'fr'})
            print(f'AVERTISSEMENT : nouveau dossier de langue "{lang}/" — entrée minimale ajoutée dans "languages", '
                  f'à compléter (name, nativeName, rtl, uiLocale).', file=sys.stderr)
    for l in languages:
        if l.get('code') not in langs:
            print(f'AVERTISSEMENT : la langue "{l.get("code")}" du manifeste n\'a aucun dossier de contenu.', file=sys.stderr)

    if args.check:
        if old is None:
            print('ERREUR : manifest.json absent (lancer sans --check pour le créer).', file=sys.stderr)
            return 1
        stale = []
        if old.get('files') != entries:
            for k in sorted(set(old.get('files', {})) | set(entries)):
                if old.get('files', {}).get(k) != entries.get(k):
                    stale.append(k)
        if old.get('chapterCount') != chapter_count or old.get('schemaVersion') != SCHEMA_VERSION:
            stale.append('chapterCount/schemaVersion')
        if stale:
            print('ERREUR : manifest.json obsolète pour : ' + ', '.join(stale) + ' — relancer sans --check.', file=sys.stderr)
            return 1
        print(f'OK : contenu valide, manifest.json à jour (contentVersion {old.get("contentVersion")}, {len(entries)} fichiers).')
        return 0

    if old is None:
        version = 1
    else:
        version = int(old.get('contentVersion', 0)) + (0 if args.no_bump else 1)
    manifest = {
        'schemaVersion': SCHEMA_VERSION,
        'contentVersion': version,
        'updatedAt': datetime.date.today().isoformat(),
        'chapterCount': chapter_count,
        'languages': languages,
        'files': entries,
    }
    with MANIFEST.open('w', encoding='utf-8', newline='\n') as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)
        f.write('\n')
    print(f'manifest.json écrit : contentVersion {version}, updatedAt {manifest["updatedAt"]}, '
          f'{chapter_count} chapitres, {len(entries)} fichiers.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
