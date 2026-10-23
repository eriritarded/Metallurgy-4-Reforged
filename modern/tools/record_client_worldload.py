"""Keep a concise result receipt, not disposable worlds, screenshots or raw logs."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path

PROJECT=Path(__file__).resolve().parents[1]
MAIN=Path("C:/Users/USER/AppData/Roaming/PrismLauncher/instances/Simply NeoForged/minecraft")
def sha(path):
    with path.open("rb") as handle: return hashlib.file_digest(handle,"sha256").hexdigest()
ap=argparse.ArgumentParser(); ap.add_argument("--scratch",type=Path,required=True); args=ap.parse_args()
root=args.scratch.resolve()
assert root.drive.upper()=="H:" and root.is_relative_to(Path("H:/CodexScratch/terraria_modding/.test-runtime").resolve())
launch=json.loads((root/"launch.json").read_text())
assert launch.get("closed") and launch["exitCode"]==0, "Owned test client has not cleanly exited"
events=[json.loads(line) for line in (root/"heartbeat.jsonl").read_text().splitlines() if line.strip()]
entered=next(row for row in events if row["state"]=="world_entered")
passed=next(row for row in events if row["state"]=="passed")
assert any(row["state"]=="world_closed" for row in events)
assert not any(row["state"]=="failed" for row in events)
assert passed["worldTicks"]>=1600 and passed["frames"]-entered["frames"]>=600
assert passed["serverTicks"]-entered["serverTicks"]>=1600
before=json.loads((root/"pack-before.json").read_text())
assert {p.name for p in (MAIN/"mods").iterdir() if p.is_file()}=={row["name"] for row in before}, "Main mod inventory changed during the check"
for row in before:
    assert sha(MAIN/"mods"/row["name"])==row["sha256"], "Main mod changed: "+row["name"]
    assert sha(root/"minecraft/mods"/row["name"])==row["sha256"], "Copied mod mismatch: "+row["name"]
production_name="Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.7.jar"
production=MAIN/"mods"/production_name
assert sha(production)==sha(PROJECT/"artifacts"/production_name)=="2c3619326d1af0ea54ff15e608b3984413ea790f57f256c596a128b44caedcef"
options={}
for line in (root/"minecraft/options.txt").read_text().splitlines():
    key,sep,value=line.partition(":")
    if sep: options[key]=value
assert options["fullscreen"]=="false" and float(options["soundCategory_master"])==0 and options["narrator"]=="0"
console=(root/"client-console.log").read_text(encoding="utf-8",errors="replace")
required_absent=["Invalid processing output","Invalid atlas id:","Error encoding packet clientbound/minecraft:recipe_content","Exception in thread \"Render thread\"","Crash report saved"]
assert not any(term in console for term in required_absent), "A targeted failure occurred despite the pass marker"
assert "METALLURGY_QA_ISOLATION" in console and "input=Default; inactive desktop" in console
assert "CLIENT in PROD" in console
worlds=list((root/"minecraft/saves").glob("*/level.dat"))
assert worlds, "No real saved world"
screenshots=list((root/"minecraft/screenshots").glob("world-loaded*.png"))
assert screenshots, "Native renderer screenshot was not written"
report={
    "recordedAt":datetime.now(timezone.utc).isoformat(),"result":"passed","minecraft":"26.1.2","neoforge":"26.1.2.122",
    "scope":"Full copied Simply NeoForged pack; native fresh-world creation, actual integrated-server connection, unpaused rendered world, production ore animateTick, native save/disconnect and clean exit",
    "version":"1.4.1-port.7","productionSha256":sha(production),"productionJarUnmodified":True,
    "scratch":str(root),"launch":launch,"entered":entered,"passed":passed,
    "worldFrames":passed["frames"]-entered["frames"],"worldServerTicks":passed["serverTicks"]-entered["serverTicks"],
    "nativeWorldFlowEvents":[row for row in events if row["state"] not in ["heartbeat"]],
    "realSavedWorlds":[{"folder":p.parent.name,"levelDatSha256":sha(p),"regionFiles":len(list(p.parent.rglob("*.mca")))} for p in worlds],
    "screenshot":{"name":screenshots[0].name,"sha256":sha(screenshots[0]),"size":screenshots[0].stat().st_size,"disposable":True},
    "mutedWindowedVerified":True,"narrationOffVerified":True,"pauseOnLostFocus":options["pauseOnLostFocus"],
    "oreParticlesEnabled":json.loads((root/"minecraft/config/metallurgy_reforged/general.json").read_text())["enableOreParticles"],
    "mainModFilesPreserved":len(before),"activePackJars":sum(row["name"].endswith(".jar") for row in before),
    "qaJarSeparate":True,"qaJarSha256":sha(root/"minecraft/mods/metallurgy-worldload-qa.jar"),
    "testSources":[{"file":p.relative_to(PROJECT).as_posix(),"sha256":sha(p)} for p in sorted((PROJECT/"verification/client-smoke").glob("*.java"))],
    "rawEvidenceHashes":[{"file":p.relative_to(root).as_posix(),"sha256":sha(p),"size":p.stat().st_size} for p in sorted(root.glob("*")) if p.is_file() and p.suffix in [".log",".jsonl"]],
    "targetedFailureStringsAbsent":required_absent,
    "harnessPreparationCorrections":["Production parent classpath must contain vanilla Minecraft, not patched dev content","Mixin package isolated from QA entrypoint","Explicitly accept the native experimental-world confirmation for the disposable modded world","Ore probe corrected to registered Adamantine/Atlarus/Eximite ores; the previous nonexistent Tartarite ore assertion was a QA failure, not a production mod error"],
    "limits":["One creative normal-preset world, seed701726122; not every seed or gameplay system","Separate QA mod drives native game APIs and cancels mouse capture","Offline test identity; no launcher credentials or external multiplayer tested","Other two requested target ports and optional dependency combinations are not covered","Raw test profiles/worlds/binaries/logs/screenshot are disposable and must be removed after this receipt"],
    "cleanupComplete":False
}
(PROJECT/"evidence/client-worldload-26.1.2.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
print(json.dumps({k:report[k] for k in ["result","worldFrames","worldServerTicks","activePackJars","productionSha256"]}))
