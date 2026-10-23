"""Concise pinned-source call inventory for visual behavior review (no execution)."""
from pathlib import Path
import argparse,hashlib,json,re
ROOT=Path(__file__).resolve().parents[1]
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--source',type=Path,required=True);args=parser.parse_args()
    source=args.source.resolve()
    if source.drive.lower()!='h:':raise ValueError('Reference sources must remain on H:')
    rows=[]
    for path in sorted((source/'effect').rglob('*.java')):
        if path.name=='BaseMetallurgyEffect.java' or 'Old' in path.name:continue
        text=path.read_text(encoding='utf-8')
        calls=[{'line':text.count('\n',0,m.start())+1,'call':' '.join(m.group(0).split())} for m in re.finditer(r'\bspawnParticle\((.*?);',text,re.S)]
        if calls:rows.append({'source':path.relative_to(source).as_posix(),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'calls':calls})
    out={'sourceVersion':'1.4.1','sourceRevision':'ef94cb94cfc08ccebe8a0df307ce6b390dcd4704','scope':'Original call expressions, including vanilla particle calls and reviewed retired candidates; not a completion or runtime claim.','files':rows}
    (ROOT/'docs/particle-source-calls.json').write_text(json.dumps(out,indent=2)+'\n',encoding='utf-8')
    print(f'Inventoried {sum(len(row["calls"]) for row in rows)} particle call expressions in {len(rows)} source files.')
if __name__=='__main__':main()
