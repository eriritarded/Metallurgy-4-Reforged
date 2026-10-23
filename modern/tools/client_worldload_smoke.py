"""Authorized real-client fresh-world QA on a private inactive Windows desktop.

No Windows input injection, activation, desktop switching, launcher credentials or real saves.
All generated files and raw logs are owned by --scratch on H:. Never shipped in the mod.
"""
from __future__ import annotations
import argparse
import ctypes as c
from ctypes import wintypes as w
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import uuid
import zipfile

PROJECT = Path(__file__).resolve().parents[1]
PRISM = Path("C:/Users/USER/AppData/Roaming/PrismLauncher")
INSTANCE = PRISM / "instances/Simply NeoForged"
JAVA = Path("C:/Program Files/Java/jdk-25/bin")
SCRATCH_BASE = Path("H:/CodexScratch/terraria_modding/.test-runtime")

def sha(path):
    return hashlib.file_digest(path.open("rb"), "sha256").hexdigest()

def owned(path):
    path = path.resolve()
    if path.drive.upper() != "H:" or not path.is_relative_to(SCRATCH_BASE.resolve()) or path == SCRATCH_BASE.resolve():
        raise RuntimeError("Owned child of H: disposable root required")
    if not Path("H:/").exists() or shutil.disk_usage("H:/").free < 20*1024**3:
        raise RuntimeError("H: unavailable or less than 20 GiB free; no C: fallback")
    for part in [path, *path.parents]:
        if part == SCRATCH_BASE.parent: break
        if part.exists() and part.is_symlink(): raise RuntimeError("Reparse/symlink scratch not permitted")
    return path

def maven_path(name):
    group, artifact, version, *classifier = name.split(":")
    suffix = "-"+classifier[0] if classifier else ""
    return Path(group.replace(".", "/"))/artifact/version/(artifact+"-"+version+suffix+".jar")

def allowed(lib):
    if "rules" not in lib: return True
    allow = False
    for rule in lib["rules"]:
        system = rule.get("os", {}).get("name", "windows")
        if system == "windows": allow = rule["action"] == "allow"
    return allow

def libraries():
    installer = PRISM/"libraries/net/neoforged/neoforge/26.1.2.122/neoforge-26.1.2.122-installer.jar"
    with zipfile.ZipFile(installer) as z: native = json.loads(z.read("version.json"))
    selected = {}
    for metadata in [json.loads((PRISM/"meta/net.minecraft/26.1.2.json").read_text()),
                     json.loads((PRISM/"meta/org.lwjgl3/3.4.1.json").read_text()), native]:
        for lib in metadata["libraries"]:
            if not allowed(lib): continue
            name = lib["name"]
            if "natives-windows-x86" in name or "natives-windows-arm64" in name: continue
            key = tuple(name.split(":")[:2]+name.split(":")[3:])
            path = PRISM/"libraries"/lib.get("downloads",{}).get("artifact",{}).get("path",str(maven_path(name)))
            if not path.is_file(): raise FileNotFoundError(path)
            selected[key] = path
    for coord in ["net.neoforged:minecraft-client-patched:26.1.2.122", "net.neoforged:neoforge:26.1.2.122:universal"]:
        path = PRISM/"libraries"/maven_path(coord)
        if not path.is_file(): raise FileNotFoundError(path)
        selected[(coord,)] = path
    return list(selected.values()), native

def prepare(root):
    root = owned(root); root.mkdir(parents=True, exist_ok=True)
    temp = root/"process-temp"; temp.mkdir(exist_ok=True)
    env = os.environ.copy()
    for key in ["TEMP", "TMP", "TMPDIR"]: env[key] = str(temp)
    env.pop("JAVA_TOOL_OPTIONS", None)
    env["PYTHONDONTWRITEBYTECODE"] = "1"
    game = root/"minecraft"; game.mkdir(exist_ok=True)
    # Copy actual enabled pack/config/resources, never user worlds, logs or credentials.
    main = INSTANCE/"minecraft"
    inventory = []
    for folder in ["mods", "config", "defaultconfigs", "resourcepacks", "shaderpacks", "kubejs", "scripts"]:
        source = main/folder
        if source.is_dir() and not (game/folder).exists(): shutil.copytree(source, game/folder)
    for path in sorted((main/"mods").glob("*")):
        if path.is_file(): inventory.append({"name":path.name,"sha256":sha(path),"size":path.stat().st_size})
    (root/"pack-before.json").write_text(json.dumps(inventory, indent=2))
    values = {}
    if (main/"options.txt").is_file():
        for line in (main/"options.txt").read_text().splitlines():
            key, sep, value = line.partition(":")
            if sep: values[key]=value
    values.update({"fullscreen":"false", "soundCategory_master":"0.0", "soundCategory_music":"0.0",
        "soundCategory_ambient":"0.0", "pauseOnLostFocus":"false", "maxFps":"60",
        "renderDistance":"8", "simulationDistance":"6", "overrideWidth":"854", "overrideHeight":"480",
        "onboardAccessibility":"false", "tutorialStep":"none", "joinedFirstServer":"true"})
    (game/"options.txt").write_text("".join(k+":"+v+"\n" for k,v in values.items()),encoding="utf-8")
    # Prevent narration independently of sound channels.
    with (game/"options.txt").open("a") as out: out.write("narrator:0\n")
    libs, native = libraries()
    cp = os.pathsep.join(map(str,libs))
    runtime_libs = [p for p in libs if "minecraft-client-patched" not in str(p) and p.name != "neoforge-26.1.2.122-universal.jar"]
    runtime_libs.append(PRISM/"libraries/com/mojang/minecraft/26.1.2/minecraft-26.1.2-client.jar")
    if not all(p.is_file() for p in runtime_libs): raise RuntimeError("Missing production launch library")
    runtime_cp = os.pathsep.join(map(str,runtime_libs))
    classes = root/("qa-classes-"+uuid.uuid4().hex); classes.mkdir()
    sources = list((PROJECT/"verification/client-smoke").glob("*.java"))
    args = [str(JAVA/"javac.exe"), "-encoding", "UTF-8", "-cp", cp, "-d", str(classes), *map(str,sources)]
    with (root/"compile.log").open("w") as out:
        result = subprocess.run(args,env=env,stdout=out,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    if result.returncode: raise RuntimeError((root/"compile.log").read_text())
    harness = game/"mods/metallurgy-worldload-qa.jar"
    with zipfile.ZipFile(harness,"w",zipfile.ZIP_DEFLATED) as z:
        for path in classes.rglob("*.class"):
            if path.name != "IsolationBootstrap.class": z.write(path,path.relative_to(classes).as_posix())
        z.writestr("META-INF/neoforge.mods.toml",'license="All Rights Reserved"\n[[mods]]\nmodId="metallurgy_worldload_qa"\nversion="1.0"\ndisplayName="Disposable World Load QA"\n[[mixins]]\nconfig="metallurgy-worldload-qa.mixins.json"\n')
        z.writestr("metallurgy-worldload-qa.mixins.json",json.dumps({"required":True,"package":"dev.miles.metallurgy.qa.mixin","compatibilityLevel":"JAVA_25","client":["NoMouseCapture"],"injectors":{"defaultRequire":1}}))
    # Native libraries extract only to the owned H: directory.
    natives = root/"natives"; natives.mkdir(exist_ok=True)
    for path in libs:
        with zipfile.ZipFile(path) as z:
            for entry in z.namelist():
                if entry.endswith(".dll") and "/" not in entry: (natives/entry).write_bytes(z.read(entry))
    command = [str(JAVA/"javaw.exe"), "-Xms512m", "-Xmx4096m", "-XX:ErrorFile="+str(root/"hs_err_pid%p.log"),
        "-Djava.io.tmpdir="+str(temp), "-Djna.tmpdir="+str(temp), "-Dorg.lwjgl.system.SharedLibraryExtractPath="+str(natives),
        "-Djava.library.path="+str(natives), "-Dorg.lwjgl.system.SharedLibraryExtractDirectory=metallurgy-qa",
        "-Dmetallurgy.qa.report="+str(root/"heartbeat.jsonl"),
        *[a.replace("${library_directory}",str(PRISM/"libraries")) for a in native["arguments"]["jvm"]],
        "-cp", str(classes)+os.pathsep+runtime_cp, "dev.miles.metallurgy.qa.IsolationBootstrap",
        "--username", "MetallurgyQA", "--uuid", "e9da93c9ebdb343791892d6b4c7a1327", "--accessToken", "0",
        "--version", "26.1.2", "--versionType", "release", "--gameDir", str(game),
        "--assetsDir", str(PRISM/"assets"), "--assetIndex", "30", "--width", "854", "--height", "480",
        *native["arguments"]["game"]]
    (root/"command.json").write_text(json.dumps(command,indent=2))
    print(json.dumps({"prepared":str(root),"libraries":len(libs),"activePackJars":len(list((main/"mods").glob("*.jar"))),"harnessSeparate":True}),flush=True)

class STARTUPINFO(c.Structure):
    _fields_=[("cb",w.DWORD),("lpReserved",w.LPWSTR),("lpDesktop",w.LPWSTR),("lpTitle",w.LPWSTR),
        ("dwX",w.DWORD),("dwY",w.DWORD),("dwXSize",w.DWORD),("dwYSize",w.DWORD),("dwXCountChars",w.DWORD),
        ("dwYCountChars",w.DWORD),("dwFillAttribute",w.DWORD),("dwFlags",w.DWORD),("wShowWindow",w.WORD),
        ("cbReserved2",w.WORD),("lpReserved2",c.POINTER(c.c_byte)),("hStdInput",w.HANDLE),("hStdOutput",w.HANDLE),("hStdError",w.HANDLE)]
class PROCESS_INFORMATION(c.Structure):
    _fields_=[("hProcess",w.HANDLE),("hThread",w.HANDLE),("dwProcessId",w.DWORD),("dwThreadId",w.DWORD)]

def launch(root, wait_for_slot=False):
    root=owned(root)
    hold=json.loads((PROJECT/"docs/launch-hold.json").read_text(encoding="utf-8-sig"))
    if not hold["gameLaunchesAllowed"] or not hold["testsAllowed"]: raise RuntimeError("Explicit launch/test authorization required")
    # No second game, even if it belongs to the user. Inspect locally without exposing command lines.
    waiting_since=time.time(); notified=False
    while True:
        check=subprocess.check_output(["powershell.exe","-NoProfile","-Command",
            "Get-CimInstance Win32_Process | Where-Object { $_.Name -match '^javaw?\\.exe$' -and $_.CommandLine -match 'net.minecraft|fml.startup.Client|forgewrapper|org.prismlauncher.EntryPoint|IsolationBootstrap' } | ForEach-Object { $_.ProcessId }"],creationflags=subprocess.CREATE_NO_WINDOW,text=True)
        if not check.strip(): break
        if not wait_for_slot or time.time()-waiting_since>900:
            raise RuntimeError("An existing Minecraft process must finish first; PIDs "+check.strip())
        if not notified:
            print("Waiting for the one-Minecraft-window slot; no game launched",flush=True); notified=True
        time.sleep(5)
    user=c.WinDLL("user32",use_last_error=True); kernel=c.WinDLL("kernel32",use_last_error=True)
    for name in ["GetProcessWindowStation","GetThreadDesktop","CreateWindowStationW","CreateDesktopW","OpenInputDesktop"]:
        getattr(user,name).restype=w.HANDLE
    user.GetThreadDesktop.argtypes=[w.DWORD]
    user.CreateWindowStationW.argtypes=[w.LPCWSTR,w.DWORD,w.DWORD,c.c_void_p]
    user.CreateDesktopW.argtypes=[w.LPCWSTR,w.LPCWSTR,c.c_void_p,w.DWORD,w.DWORD,c.c_void_p]
    for name in ["SetProcessWindowStation","SetThreadDesktop","CloseDesktop","CloseWindowStation"]:
        getattr(user,name).argtypes=[w.HANDLE]
    user.GetUserObjectInformationW.argtypes=[w.HANDLE,c.c_int,c.c_void_p,w.DWORD,c.POINTER(w.DWORD)]
    kernel.CreateProcessW.argtypes=[w.LPCWSTR,w.LPWSTR,c.c_void_p,c.c_void_p,w.BOOL,w.DWORD,c.c_void_p,w.LPCWSTR,c.POINTER(STARTUPINFO),c.POINTER(PROCESS_INFORMATION)]
    kernel.WaitForSingleObject.argtypes=[w.HANDLE,w.DWORD]
    kernel.CloseHandle.argtypes=[w.HANDLE]
    kernel.GetExitCodeProcess.argtypes=[w.HANDLE,c.POINTER(w.DWORD)]
    kernel.TerminateProcess.argtypes=[w.HANDLE,w.UINT]
    def objname(handle):
        buf=c.create_unicode_buffer(512); size=w.DWORD()
        if not user.GetUserObjectInformationW(handle,2,buf,c.sizeof(buf),c.byref(size)): raise c.WinError(c.get_last_error())
        return buf.value
    original_station=user.GetProcessWindowStation(); original_desktop=user.GetThreadDesktop(kernel.GetCurrentThreadId())
    station_name=objname(original_station); desktop_name="MetallurgyQA_"+uuid.uuid4().hex
    station=None
    desktop=None; pi=PROCESS_INFORMATION()
    try:
        # Microsoft desktop isolation: only the input desktop is visible/receives input;
        # messages/hooks cannot cross desktops. No SwitchDesktop call exists in this helper.
        # Do not request DESKTOP_SWITCHDESKTOP on our desktop handle.
        desktop=user.CreateDesktopW(desktop_name,None,None,0,0xFF,None)
        if not desktop: raise c.WinError(c.get_last_error())
        if not user.SetThreadDesktop(original_desktop) or not user.SetProcessWindowStation(original_station): raise c.WinError(c.get_last_error())
        command=json.loads((root/"command.json").read_text())
        command[1:1]=["-Dmetallurgy.qa.station="+station_name,"-Dmetallurgy.qa.desktop="+desktop_name]
        env=os.environ.copy()
        for key in ["TEMP","TMP","TMPDIR"]: env[key]=str(root/"process-temp")
        env.pop("JAVA_TOOL_OPTIONS",None)
        block=c.create_unicode_buffer("\0".join(k+"="+v for k,v in sorted(env.items(),key=lambda kv:kv[0].upper()))+"\0\0")
        import msvcrt
        with (root/"client-console.log").open("wb") as log, open(os.devnull,"rb") as null:
            output=msvcrt.get_osfhandle(log.fileno()); input_handle=msvcrt.get_osfhandle(null.fileno())
            os.set_handle_inheritable(output,True); os.set_handle_inheritable(input_handle,True)
            si=STARTUPINFO(); si.cb=c.sizeof(si); si.lpDesktop=station_name+"\\"+desktop_name
            si.dwFlags=0x100|0x80; si.hStdInput=input_handle; si.hStdOutput=output; si.hStdError=output
            if not kernel.CreateProcessW(command[0],c.create_unicode_buffer(subprocess.list2cmdline(command)),None,None,True,0x08000000|0x400,block,str(root/"minecraft"),c.byref(si),c.byref(pi)):
                raise c.WinError(c.get_last_error())
            kernel.CloseHandle(pi.hThread); pi.hThread=None
            start=time.time(); launch_record={"pid":pi.dwProcessId,"station":station_name,"desktop":desktop_name,"inputDesktop":False,"mouseCaptureBlockedByRequiredMixin":True,"started":start}
            (root/"launch.json").write_text(json.dumps(launch_record,indent=2))
            print(json.dumps(launch_record),flush=True)
            while kernel.WaitForSingleObject(pi.hProcess,1000)==258:
                if time.time()-start>900:
                    # Only this exact owned process. Retain diagnostic stack dump on H: before terminating a hung attempt.
                    with (root/"timeout-threads.log").open("w") as out:
                        subprocess.run([str(JAVA/"jcmd.exe"),str(pi.dwProcessId),"Thread.print"],stdout=out,stderr=subprocess.STDOUT,env=env,creationflags=subprocess.CREATE_NO_WINDOW,timeout=30)
                    kernel.TerminateProcess(pi.hProcess,124); kernel.WaitForSingleObject(pi.hProcess,10000)
                    launch_record["timeout"]=True; break
            code=w.DWORD(); kernel.GetExitCodeProcess(pi.hProcess,c.byref(code))
            launch_record.update(exitCode=code.value,elapsedSeconds=round(time.time()-start,2),closed=True)
            (root/"launch.json").write_text(json.dumps(launch_record,indent=2))
            print(json.dumps(launch_record),flush=True)
    finally:
        user.SetThreadDesktop(original_desktop); user.SetProcessWindowStation(original_station)
        if pi.hProcess:
            if kernel.WaitForSingleObject(pi.hProcess,0)==258:
                kernel.TerminateProcess(pi.hProcess,125); kernel.WaitForSingleObject(pi.hProcess,10000)
            kernel.CloseHandle(pi.hProcess)
        if desktop: user.CloseDesktop(desktop)
        if station: user.CloseWindowStation(station)

if __name__=="__main__":
    ap=argparse.ArgumentParser(); ap.add_argument("mode",choices=["prepare","launch"]); ap.add_argument("--scratch",type=Path,required=True); ap.add_argument("--wait-for-slot",action="store_true")
    opts=ap.parse_args()
    if opts.mode=="prepare": prepare(opts.scratch)
    else: launch(opts.scratch,opts.wait_for_slot)
