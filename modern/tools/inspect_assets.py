"""Static resource inventory only: no game, test framework or native codec execution."""
from pathlib import Path
import argparse, hashlib, json
ROOT=Path(__file__).resolve().parents[1]

def inspect(target):
    files={};overlaps=[]
    for base in [ROOT/'common/src/main/resources',ROOT/target/'src/main/resources']:
        for path in base.rglob('*'):
            if not path.is_file():continue
            name=path.relative_to(base).as_posix()
            if name in files:overlaps.append(name)
            files[name]=path
    missing=[];json_count=0
    def own(value,kind,source):
        if not isinstance(value,str) or not value.startswith('metallurgy:'):return
        name=value.split(':',1)[1]
        destination='assets/metallurgy/'+kind+'/'+name+('.png' if kind=='textures' else '.json')
        if destination not in files:missing.append({'source':source,'reference':value,'expected':destination})
    def walk(value,source):
        if isinstance(value,list):
            for child in value:walk(child,source)
        elif isinstance(value,dict):
            for key,child in value.items():
                if key=='textures' and isinstance(child,dict):
                    for texture in child.values():own(texture,'textures',source)
                elif key=='parent':own(child,'models',source)
                elif key=='model' and isinstance(child,str):own(child,'models',source)
                walk(child,source)
    for name,path in files.items():
        if not name.endswith(('.json','.mcmeta')):continue
        data=json.loads(path.read_text(encoding='utf-8'));json_count+=1
        if name.startswith('assets/') and any(part in name for part in ['/models/','/blockstates/','/items/']):walk(data,name)
    atlas=json.loads(files['assets/minecraft/atlases/blocks.json'].read_text(encoding='utf-8'))
    for entry in atlas['sources']:
        if entry.get('type')=='minecraft:single':own(entry.get('resource'),'textures','assets/minecraft/atlases/blocks.json')
    return {'target':target,'files':len(files),'jsonOrMetadataParsed':json_count,'overlappingPaths':overlaps,'missingOwnModelOrTextureReferences':missing,'blockAtlasSources':len(atlas['sources']),
            'scope':'JSON syntax, explicit Metallurgy model/texture reference existence, duplicate paths and atlas inventory only. Does not decode native schemas, render, launch or establish gameplay.'}

def main():
    result={'reviewed':'2026-10-16','targets':[inspect(t) for t in ['forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2']]}
    (ROOT/'docs/static-asset-inventory.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    for row in result['targets']:print(row['target'],row['files'],'files;',len(row['overlappingPaths']),'overlaps;',len(row['missingOwnModelOrTextureReferences']),'missing references;',row['blockAtlasSources'],'atlas sources')
if __name__=='__main__':main()
