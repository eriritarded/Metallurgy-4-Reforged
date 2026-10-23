"""Export maintained source and documentation as an ordinary deliverable."""
from pathlib import Path
import hashlib,json,zipfile
ROOT=Path(__file__).resolve().parents[1]

def main():
    destination=ROOT/'artifacts';manifest=destination/'manifest.json'
    data=json.loads(manifest.read_text(encoding='utf-8'));out=destination/'Metallurgy-Reforged-1.4.1-port.1-source-project.zip'
    excluded={'artifacts','.gradle','build','__pycache__','.git'};files=[]
    for path in ROOT.rglob('*'):
        relative=path.relative_to(ROOT)
        if path.is_file() and not any(part in excluded for part in relative.parts):files.append(path)
    with zipfile.ZipFile(out,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6,strict_timestamps=False) as archive:
        for path in sorted(files):archive.write(path,'MetallurgyModern/'+path.relative_to(ROOT).as_posix())
    with out.open('rb') as stream:sha=hashlib.file_digest(stream,'sha256').hexdigest()
    data['artifacts']=[entry for entry in data['artifacts'] if entry['kind']!='source-project']
    data['artifacts'].append({'file':out.name,'kind':'source-project','files':len(files),'size':out.stat().st_size,'sha256':sha})
    manifest.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    (destination/'SHA256SUMS.txt').write_text(''.join(entry['sha256']+'  '+entry['file']+'\n' for entry in data['artifacts']),encoding='utf-8')
    print(out.name,len(files),'files',out.stat().st_size,'bytes',sha)
if __name__=='__main__':main()
