"""Resolve reachable native item/block model texture slots without loading Minecraft classes."""
from pathlib import Path
from functools import lru_cache
from datetime import datetime, timezone
import argparse, hashlib, json, zipfile

ROOT = Path(__file__).resolve().parents[1]

def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()

def inspect(own, native):
    def identifier(value):
        return value if ':' in value else 'minecraft:' + value
    def model_path(key):
        namespace, name = identifier(key).split(':', 1)
        return 'assets/' + namespace + '/models/' + name + '.json'
    models = {name:json.loads(data) for name,data in own.items() if '/models/' in name and name.endswith('.json')}
    native_models = {name:json.loads(native.read(name)) for name in native.namelist() if name.startswith('assets/minecraft/models/') and name.endswith('.json')}
    files = set(own) | set(native.namelist())
    failures, roots = [], set()
    def discover(data):
        if isinstance(data, dict):
            for key, value in data.items():
                if key in ('model', 'base') and isinstance(value, str):
                    roots.add(identifier(value))
                else:
                    discover(value)
        elif isinstance(data, list):
            for value in data:
                discover(value)
    for name, data in own.items():
        if name.endswith('.json') and ('/blockstates/' in name or '/items/' in name):
            discover(json.loads(data))
    @lru_cache(maxsize=None)
    def merged(key, chain=()):
        key = identifier(key)
        if key in chain:
            raise ValueError('Model parent cycle: ' + ' -> '.join((*chain, key)))
        # This is a native virtual model marker, not a JSON/PNG resource.
        if key == 'minecraft:builtin/generated':
            return {'particle':'#layer0'}, [], True
        path = model_path(key)
        data = models.get(path, native_models.get(path))
        if data is None:
            raise ValueError('Missing model: ' + key)
        textures, elements, generated = ({}, [], False)
        if 'parent' in data:
            textures, elements, generated = merged(identifier(data['parent']), (*chain, key))
        textures = {**textures, **data.get('textures', {})}
        return textures, data.get('elements', elements), generated
    texture_uses = 0
    unique_textures = set()
    own_roots = sorted(key for key in roots if key.startswith(('metallurgy:', 'metallurgychests:')))
    for root in own_roots:
        try:
            textures, elements, generated = merged(root)
        except ValueError as error:
            failures.append({'root':root, 'error':str(error)})
            continue
        references = ['#particle']
        references.extend(face.get('texture', '#missing') for element in elements for face in element.get('faces', {}).values())
        if generated:
            references.extend('#' + key for key in textures if key.startswith('layer'))
            if 'layer0' not in textures:
                references.append('#layer0')
        for reference in references:
            texture_uses += 1
            trail, value = [], reference
            while isinstance(value, str) and value.startswith('#'):
                slot = value[1:]
                if slot in trail or slot not in textures:
                    value = None
                    break
                trail.append(slot)
                value = textures[slot]
            if not isinstance(value, str):
                failures.append({'root':root, 'reference':reference, 'slots':trail, 'error':'Unresolved texture slot'})
                continue
            namespace, path = identifier(value).split(':', 1)
            png = 'assets/' + namespace + '/textures/' + path + '.png'
            unique_textures.add(identifier(value))
            if png not in files:
                failures.append({'root':root, 'reference':reference, 'texture':value, 'error':'Missing texture file'})
    grouped = {}
    for failure in failures:
        key = json.dumps(failure, sort_keys=True)
        if key not in grouped:
            grouped[key] = {**failure, 'occurrences':0}
        grouped[key]['occurrences'] += 1
    return dict(reachableOwnModelRoots=len(own_roots), resolvedTextureUses=texture_uses,
                uniqueTextures=len(unique_textures), failures=list(grouped.values()),
                abstractTemplatesAreNotRoots=not any(key in roots for key in ('metallurgy:item/extended','metallurgy:item/extended_handheld')))

def main():
    parser = argparse.ArgumentParser()
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument('--target')
    source.add_argument('--artifact', type=Path)
    parser.add_argument('--native-jar', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.artifact:
        with zipfile.ZipFile(args.artifact) as archive:
            own = {name:archive.read(name) for name in archive.namelist() if name.startswith(('assets/metallurgy/','assets/metallurgychests/'))}
    else:
        own = {}
        for base in (ROOT/'common/src/main/resources', ROOT/args.target/'src/main/resources'):
            for folder in ('assets/metallurgy','assets/metallurgychests'):
                for path in (base/folder).rglob('*'):
                    if path.is_file():
                        name = path.relative_to(base).as_posix()
                        if name in own:
                            raise ValueError('Overlapping resource path: ' + name)
                        own[name] = path.read_bytes()
    with zipfile.ZipFile(args.native_jar) as native:
        report = inspect(own, native)
    report.update(recordedAt=datetime.now(timezone.utc).isoformat(), target=args.target,
                  artifact=str(args.artifact) if args.artifact else None,
                  artifactSha256=digest(args.artifact) if args.artifact else None,
                  nativeJar=str(args.native_jar), nativeJarSha256=digest(args.native_jar),
                  method='Static JSON parent/texture-slot/file resolution only; no native codec or rendering execution',
                  gameLaunched=False, testsRun=False)
    args.output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))
    if report['failures']:
        raise SystemExit(1)

if __name__ == '__main__':
    main()
