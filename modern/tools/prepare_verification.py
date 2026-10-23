"""Generate the maintained empty structure fixture; never creates a game world."""
from pathlib import Path
import gzip
import struct

ROOT=Path(__file__).resolve().parents[1]


def text(value):
    data=value.encode('utf-8')
    return struct.pack('>H',len(data))+data


def named(kind,name,payload):
    return bytes([kind])+text(name)+payload


def main():
    data=bytes([10])+text('')
    data+=named(3,'DataVersion',struct.pack('>i',3465))
    data+=named(9,'size',bytes([3])+struct.pack('>iiii',3,16,8,16))
    data+=named(9,'palette',bytes([10])+struct.pack('>i',1)+named(8,'Name',text('minecraft:air'))+bytes([0]))
    data+=named(9,'blocks',bytes([10])+struct.pack('>i',0))
    data+=named(9,'entities',bytes([10])+struct.pack('>i',0))+bytes([0])
    path=ROOT/'verification/forge-1.20.1/resources/data/metallurgy/structures/empty.nbt'
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(gzip.compress(data,mtime=0))
    for target in ['forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2']:
        p=ROOT/target/'build.gradle'
        t=p.read_text(encoding='utf-8')
        marker='// Explicit test-only source set; never part of an ordinary build.'
        if marker not in t:
            t+='\n'+marker+"\nif(project.hasProperty('verification')) {\n    sourceSets.main.java.srcDir('../verification/"+target+"/java')\n    sourceSets.main.resources.srcDir('../verification/"+target+"/resources')\n}\n"
            p.write_text(t,encoding='utf-8')


if __name__=='__main__':main()
