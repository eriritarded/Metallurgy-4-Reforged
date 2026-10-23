"""Maintained recipe acquisition conditions; no registry IDs are removed."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]

def main():
    total=0
    for target in ('forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2'):
        old=target.startswith('forge-')
        root=ROOT/target/'src/main/resources/data/metallurgy'/('recipes' if old else 'recipe')
        for path in root.rglob('*.json'):
            data=json.loads(path.read_text(encoding='utf-8'))
            result=data.get('result',{})
            results=result if isinstance(result,list) else [result]
            contents=[]
            for value in results:
                content=value if isinstance(value,str) else value.get('item',value.get('id','')) if isinstance(value,dict) else ''
                if isinstance(content,str) and content.startswith('metallurgy:'):contents.append(content)
            if not contents:continue
            key='conditions' if old else 'neoforge:conditions'
            conditions=[c for c in data.get(key,[]) if c.get('type')!='metallurgy:content_enabled']
            conditions.extend({'type':'metallurgy:content_enabled','content':content} for content in sorted(set(contents)))
            data[key]=conditions
            text=json.dumps(data,indent=2,ensure_ascii=False)+'\n'
            if path.read_text(encoding='utf-8')!=text:path.write_text(text,encoding='utf-8')
            total+=1
    print(f'Wrote native content availability conditions for {total} target recipe files')

if __name__=='__main__':main()
