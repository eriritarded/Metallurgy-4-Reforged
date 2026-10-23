"""Recover only the original addon recipes and licensed pixel-art dependencies."""
from pathlib import Path
import argparse,json,zipfile,hashlib
ROOT=Path(__file__).resolve().parents[1]

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--recipes',type=Path,required=True);parser.add_argument('--jar',type=Path,required=True);args=parser.parse_args()
    if args.jar.resolve().drive.lower()!='h:' or args.recipes.resolve().drive.lower()!='h:':raise ValueError('Reference inputs must be on H:')
    if hashlib.sha256(args.jar.read_bytes()).hexdigest()!='d7f376edce9581006b4115277c5015b8e7ac3e396171938217705ae7b6cc1d18':raise ValueError('Wrong source dependency archive')
    rows=[]
    for path in sorted(args.recipes.rglob('*/spartanweaponry/**/*.json')):
        rows.append({'source':path.relative_to(args.recipes).as_posix(),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'recipe':json.loads(path.read_text(encoding='utf-8'))})
    if len(rows)!=897:raise ValueError('Expected all897 original recipes')
    (ROOT/'content/spartan-legacy-recipes.json').write_text(json.dumps(rows,indent=2)+'\n',encoding='utf-8')
    models=ROOT/'content/legacy-spartan-models'
    assets=[]
    with zipfile.ZipFile(args.jar) as archive:
        def resolve(value,seen=None):
            seen=set() if seen is None else seen
            parent=value.get('parent','');textures={}
            if parent.startswith('spartanweaponry:'):
                key='assets/spartanweaponry/models/'+parent.split(':',1)[1]+'.json'
                if key in seen:raise ValueError('Cyclic original model '+key)
                seen.add(key);textures.update(resolve(json.loads(archive.read(key)),seen))
            textures.update(value.get('textures',{}));return textures
        resolved={p.stem:resolve(json.loads(p.read_text(encoding='utf-8'))) for p in models.glob('*.json')}
        (ROOT/'content/spartan-legacy-models.json').write_text(json.dumps(resolved,indent=2)+'\n',encoding='utf-8')
        needed={value.split(':',1)[1]+'.png' for textures in resolved.values() for layer,value in textures.items() if layer.startswith('layer') and value.startswith('spartanweaponry:')}
        for name in sorted(needed):
            source='assets/spartanweaponry/textures/'+name;raw=archive.read(source)
            dest=ROOT/'forge-1.20.1/src/main/resources/assets/metallurgy/textures/spartanweaponry'/name
            dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(raw)
            assets.append({'source':source,'output':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(raw).hexdigest()})
    record={'sourceDependency':'curse.maven:spartanweaponry_1.5.3-278141:4467344','sha256':hashlib.sha256(args.jar.read_bytes()).hexdigest(),
            'license':'Apache-2.0','licenseSource':'ObliviousSpartan/SpartanWeaponry@af87ea162cc043f1ea4236e5da5e723c600001ed/LICENSE',
            'pixelArtModified':False,'assets':assets,'recipeCount':len(rows)}
    (ROOT/'docs/spartan-art-baseline.json').write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
    print('Imported897 original recipe definitions and'+str(len(assets))+' byte-identical legacy layered pixel-art files')

if __name__=='__main__':main()
