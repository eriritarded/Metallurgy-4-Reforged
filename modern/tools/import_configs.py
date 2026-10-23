"""Retain original config fields/defaults/metadata without linking removed Forge1.12 APIs."""
from pathlib import Path
import argparse,hashlib,json,re
ROOT=Path(__file__).resolve().parents[1]
NAMES=['GeneralConfig','RegistrationConfig','EffectsRosterConfig','EffectTweaksConfig','GadgetsConfig','WorldGenerationConfig']
def remove_block(text,needle):
    start=text.index(needle);brace=text.index('{',start);depth=1;end=brace+1
    while depth:
        depth+=(text[end]=='{')-(text[end]=='}');end+=1
    return text[:start]+text[end:]
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--source',required=True,type=Path);args=parser.parse_args()
    source=args.source.resolve();baseline=json.loads((ROOT/'docs/source-baseline.json').read_text(encoding='utf-8-sig'))
    expected=Path(baseline['scratch']).resolve()/'upstream/src/main/java/it/hurts/metallurgy_reforged/config'
    if source!=expected:raise ValueError('Use pinned source in the owned H: directory')
    dest=ROOT/'common/src/main/java/it/hurts/metallurgy_reforged/config';dest.mkdir(parents=True,exist_ok=True)
    records={}
    for name in NAMES:
        path=source/(name+'.java');raw=path.read_bytes();text=raw.decode('utf-8').replace('\r\n','\n')
        if name=='GeneralConfig':
            text=remove_block(text,'public static class ChangeListener')
            text=text.replace('public class GeneralConfig {','''public class GeneralConfig {

    @Config.Name("Tetra Integration")
    @Config.Comment("Enable native Tetra materials when a supported Tetra version is installed")
    @Config.RequiresMcRestart
    public static boolean tetraIntegration = true;''')
        if name=='WorldGenerationConfig':text=remove_block(text,'@Deprecated\n\tpublic static void proofCheckBiomes')
        text=re.sub(r'^import net\.[^\n]+\n','',text,flags=re.M)
        text=text.replace('Metallurgy.MODID','"metallurgy"')
        text=text.replace('//Handles Config Synchronization','// Runtime loading is handled by ConfigFiles; original field names remain stable.')
        text=text.replace('public static Integer[] tarLakeDimensionWhiteList = {0};','public static String[] tarLakeDimensionWhiteList = {"minecraft:overworld"};')
        (dest/(name+'.java')).write_text(text,encoding='utf-8')
        records[name]={'sourceSha256':hashlib.sha256(raw).hexdigest(),'implementation':'common/src/main/java/it/hurts/metallurgy_reforged/config/'+name+'.java','adaptation':'Original mutable fields/defaults and metadata retained; native modern JSON loader. Worldgen dimension IDs use resource identifiers. Legacy UI listener/obsolete biome proof-check omitted.'}
    (ROOT/'docs/config-source-baseline.json').write_text(json.dumps({'sourceCommit':'ef94cb94cfc08ccebe8a0df307ce6b390dcd4704','files':records},indent=2)+'\n')
    print('Adapted six original configuration definitions; consumer wiring is tracked separately.')
if __name__=='__main__':main()
