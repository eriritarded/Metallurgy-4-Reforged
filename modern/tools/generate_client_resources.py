"""Native atlas declarations and two authored flat UI sprites; original art is untouched.

The white frame is tinted at render time, including custom material colors.
This uses explicit pixel coordinates, not generated artwork or a game launch.
"""
from pathlib import Path
import json,struct,zlib
ROOT=Path(__file__).resolve().parents[1]
def dump(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def png(path,pixels):
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    raw=b''.join(b'\0'+bytes(c for pixel in row for c in pixel) for row in pixels)
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',32,32,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
def legacy_model_atlases():
    """Native26 separates item/block atlases; keep the licensed legacy resource IDs."""
    base=ROOT/'neoforge-26.1.2/src/main/resources/assets/minecraft/atlases'
    for name,folders in [('blocks',('blocks',)),('items',('items','blocks'))]:
        atlas=base/(name+'.json')
        data=json.loads(atlas.read_text(encoding='utf-8')) if atlas.exists() else {'sources':[]}
        for folder in folders:
            entry={'type':'minecraft:directory','source':folder,'prefix':folder+'/'}
            if entry not in data['sources']:data['sources'].append(entry)
        dump(atlas,data)
def main():
    common=ROOT/'common/src/main/resources'
    for target in ('forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2'):
        atlas=ROOT/target/'src/main/resources/assets/minecraft/atlases/blocks.json'
        data=json.loads(atlas.read_text(encoding='utf-8')) if atlas.exists() else {'sources':[]}
        data['sources']=[v for v in data['sources'] if not v.get('resource','').startswith('metallurgy:particles/ore_particle_')]
        data['sources'] += [{'type':'minecraft:single','resource':'metallurgy:particles/ore_particle_'+str(i)} for i in range(1,11)]
        dump(atlas,data)
        config=ROOT/target/'src/main/resources/metallurgy.mixins.json';mixins=json.loads(config.read_text(encoding='utf-8'));mixins['client']=list(dict.fromkeys(mixins.get('client',[])+['CorpseRenderMixin']));dump(config,mixins)
    legacy_model_atlases()
    base=ROOT/'neoforge-26.1.2/src/main/resources/assets/metallurgy/textures/gui/sprites/tooltip'
    for name in ('background','frame'):
        pixels=[[(0,0,0,0) for x in range(32)] for y in range(32)]
        if name=='background':
            for y in range(9,23):
                for x in range(9,23):
                    if (x,y) not in [(9,9),(9,22),(22,9),(22,22)]:pixels[y][x]=(16,0,16,240)
        else:
            for k in range(11,21):
                for x,y in [(k,10),(k,21),(10,k),(21,k)]:pixels[y][x]=(255,255,255,255)
        path=base/('metal_'+name+'.png');png(path,pixels)
        dump(path.with_suffix('.png.mcmeta'),{'gui':{'scaling':{'type':'nine_slice','width':32,'height':32,'border':12}}})
    mixin=ROOT/'neoforge-26.1.2/src/main/resources/metallurgy.mixins.json'
    data=json.loads(mixin.read_text(encoding='utf-8'));data['client']=list(dict.fromkeys(data.get('client',[])+['TooltipFrameMixin','BlockModelRenderAccess']));dump(mixin,data)
    print('Declared the original10 ore frames and native26 runtime-tinted tooltip UI')
if __name__=='__main__':main()
