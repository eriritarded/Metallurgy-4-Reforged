"""Convert the pinned original content into maintained port inputs.

Copies original artwork without modification; converts resource structure and
records provenance. Does not compile, install or launch a game.
"""
from pathlib import Path
import argparse
import collections
import hashlib
import json
import re
import shutil

ROOT = Path(__file__).resolve().parents[1]


def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--reference', required=True, type=Path)
    args = parser.parse_args()
    ref = args.reference.resolve()
    if ref.drive.upper() != 'H:':
        raise ValueError('Extracted reference must be on H:')
    source = ref / 'upstream/src/main/resources/assets/metallurgy'
    common = ROOT / 'common/src/main/resources'
    own = common / 'assets/metallurgy'
    own.mkdir(parents=True, exist_ok=True)
    materials = [m for m in json.loads((source / 'materials.json').read_text(encoding='utf-8')) if isinstance(m, dict)]
    dump(own / 'materials.json', materials)
    for name in ['textures', 'sounds', 'models']:
        shutil.copytree(source / name, own / name, dirs_exist_ok=True)
    shutil.copy2(source / 'sounds.json', own / 'sounds.json')
    # Legacy item registration explicitly selected nested model locations.
    # Modern item IDs select a model at models/item/<registry-path>.json.
    for path in (source / 'models/item').rglob('*.json'):
        if path.parent != source / 'models/item':
            shutil.copy2(path, own / 'models/item' / path.name)
    chest_source = ref / 'chests/src/main/resources/assets/metallurgychests'
    for name in ['textures']:
        shutil.copytree(chest_source / name, common / 'assets/metallurgychests' / name, dirs_exist_ok=True)
    chests = dict(brass=54, silver=72, electrum=81, rubracium=90, platinum=108)
    model_entries = {p.stem: json.loads(p.read_text(encoding='utf-8')) for p in (source / 'models/item').rglob('*.json')}
    blocks = []
    for path in (source / 'blockstates').glob('*.json'):
        data = json.loads(path.read_text(encoding='utf-8'))
        name = path.stem
        if name.startswith('molten_'):
            continue
        if 'forge_marker' in data:
            defaults = data.get('defaults', {})
            variants = data.get('variants', defaults.get('variants', {}))
            normal = variants.get('normal', variants.get('inventory', {}))
            if isinstance(normal, list):
                normal = normal[0]
            parent = normal.get('model', defaults.get('model', 'cube_all'))
            if ':' not in parent:
                parent = 'minecraft:block/' + parent
            elif parent.startswith('metallurgy:') and '/' not in parent.split(':', 1)[1]:
                parent = parent.replace(':', ':block/')
            model = dict(parent=parent, textures={**defaults.get('textures', {}), **normal.get('textures', {})})
            dump(own / 'models/block' / (name + '.json'), model)
            data = {'variants': {'': {'model': 'metallurgy:block/' + name}}}
        else:
            def fix_models(v):
                if isinstance(v, dict):
                    for k, x in v.items():
                        if k == 'model' and isinstance(x, str) and x.startswith('metallurgy:') and not x.startswith('metallurgy:block/'):
                            v[k] = x.replace(':', ':block/')
                        else:
                            fix_models(x)
                elif isinstance(v, list):
                    for x in v:
                        fix_models(x)
            fix_models(data)
        dump(own / 'blockstates' / path.name, data)
        if name not in model_entries:
            dump(own / 'models/item' / path.name, {'parent': 'metallurgy:block/' + name})
        blocks.append(name)
    # Chest models/renderers and the target-specific data schema are generated
    # with the actual implementation, not substituted with generic cube art.
    languages = {}
    for path in (source / 'lang').glob('*.lang'):
        try:
            text = path.read_text(encoding='utf-8-sig')
        except UnicodeDecodeError:
            # Legacy Spanish is Windows-1252, with original provenance retained.
            if path.stem not in ['es_es', 'es_mx']:
                raise
            text = path.read_text(encoding='cp1252')
        lang = {}
        for line in text.splitlines():
            if not line.strip() or line.lstrip().startswith('#') or '=' not in line:
                continue
            key, value = line.split('=', 1)
            if key.endswith('.name') and key.startswith(('item.', 'tile.')):
                key = key[:-5].replace('tile.', 'block.', 1)
            lang[key] = value
        for metal in chests:
            lang['block.metallurgy.' + metal + '_chest'] = metal.title() + ' Chest'
        lang['itemGroup.metallurgy'] = 'Metallurgy'
        dump(own / 'lang' / (path.stem + '.json'), lang)
        languages[path.stem] = len(lang)
    catalog = dict(materials=materials, blocks=sorted(blocks), chests=chests,
                   worldgenOreMaterials=[m['name'] for m in materials if m['ore_harvest_level'] >= 0],
                   originalModelIds=sorted(model_entries),
                   status='Resource conversion inputs; runtime implementation and parity tracked separately')
    dump(ROOT / 'content/legacy-catalog.json', catalog)
    # Full inventory retains helper/integration classes as explicitly unverified
    # until each behavior is accounted for by an actual implementation.
    rows = []
    for base, prefix in [(ref / 'upstream/src/main/java', 'metallurgy'), (ref / 'chests/src/main/java', 'metallurgychests')]:
        for path in base.rglob('*.java'):
            rows.append(dict(source=prefix + '/' + path.relative_to(base).as_posix(),
                             sha256=hashlib.sha256(path.read_bytes()).hexdigest(), status='unverified',
                             targets={t: dict(status='unverified', implementation=[], evidence=[]) for t in ['forge-1.20.1', 'neoforge-1.21.1', 'neoforge-26.1.2']}))
    dump(ROOT / 'docs/coverage.json', dict(complete=False, files=rows))
    dump(ROOT / 'evidence/resource-import.json', dict(materials=len(materials), blocks=len(blocks), chestTiers=chests,
        languages=languages, originalTextures={p.relative_to(common).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
        for p in common.rglob('*.png')}, gameLaunched=False))
    print(json.dumps(dict(materials=len(materials), blocks=len(blocks), originalTextureFiles=len(list(common.rglob('*.png'))), sourceCoverageRows=len(rows))))


if __name__ == '__main__':
    main()
