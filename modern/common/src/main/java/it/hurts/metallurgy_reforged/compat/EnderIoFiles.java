package it.hurts.metallurgy_reforged.compat;

import com.google.gson.JsonParser;
import it.hurts.metallurgy_reforged.config.ConfigFiles;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Opt-in, persistent native alloy recipes. Editing/deleting files is authoritative. */
public final class EnderIoFiles {
    public static synchronized Map<String,byte[]> load(String target,String prefix,Map<String,byte[]> defaults) {
        if(!target.matches("[a-z0-9.-]+")) throw new IllegalArgumentException("Invalid Ender IO target");
        Path base=ConfigFiles.folder().resolve("enderio_alloys").resolve(target).normalize();
        try {
            Files.createDirectories(base);
            if(Files.isSymbolicLink(base)) throw new IOException("Ender IO recipe directory cannot be a symbolic link");
            Path marker=base.resolve(".initialized");
            if(!Files.exists(marker)) {
                for(var entry:defaults.entrySet()) {
                    if(!entry.getKey().startsWith(prefix)) throw new IOException("Unexpected bundled alloy recipe path");
                    Path destination=base.resolve(entry.getKey().substring(prefix.length())).normalize();
                    if(!destination.startsWith(base)) throw new IOException("Escaped alloy recipe directory");
                    Files.createDirectories(destination.getParent());
                    try {Files.write(destination,entry.getValue(),StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException ignored) { /* Keep user's file. */ }
                }
                String help="Metallurgy Ender IO native alloy recipes ("+target+")\n"
                    +"These files are used only while enableEnderIOAlloyConfig is true and Ender IO is loaded.\n"
                    +"Edit JSON, add lower-case .json filenames, or delete files to remove recipes; then reload the server data packs.\n"
                    +"The recipe ID is metallurgy:compat/enderio/<relative filename without .json>.\n"
                    +"Defaults are copied only once. Disabling the setting restores bundled recipes without deleting your files.\n"
                    +"Each Minecraft target has its own folder because Ender IO's native formats differ.\n"
                    +"Old1.12 XML is preserved but not parsed by the modern mod; convert it to this target's native JSON.\n";
                try {Files.writeString(base.resolve("README.txt"),help,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException ignored) {}
                Files.writeString(marker,"Default recipe files initialized; deleting a recipe is intentional.\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
                if(Files.exists(ConfigFiles.folder().resolve("metallurgy_enderio_alloys.xml"))) org.slf4j.LoggerFactory.getLogger("metallurgy").warn("Legacy Ender IO XML was preserved. Convert custom alloys to native JSON in {}",base);
            }
            Map<String,byte[]> result=new LinkedHashMap<>();long total=0;Path realBase=base.toRealPath();
            try(var stream=Files.walk(base)) {
                for(Path file:stream.sorted().toList()) {
                    if(Files.isSymbolicLink(file)) throw new IOException("Symbolic link in alloy recipe directory: "+file);
                    if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS) || !file.getFileName().toString().endsWith(".json")) continue;
                    String relative=base.relativize(file).toString().replace('\\','/');
                    if(!relative.matches("[a-z0-9/._-]+\\.json") || !file.toRealPath().startsWith(realBase)) throw new IOException("Invalid alloy recipe path: "+relative);
                    long size=Files.size(file);total+=size;
                    if(size>4*1024*1024 || total>16*1024*1024 || result.size()>=1024) throw new IOException("Ender IO recipes exceed1024 files/4MiB per file/16MiB total");
                    byte[] bytes=Files.readAllBytes(file);var json=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8));
                    if(!json.isJsonObject() || !json.getAsJsonObject().has("type") || !json.getAsJsonObject().get("type").getAsString().equals("enderio:alloy_smelting")) throw new IOException("Expected native Ender IO alloy_smelting JSON: "+relative);
                    result.put(prefix+relative,bytes);
                }
            }
            return Collections.unmodifiableMap(result);
        } catch(IOException|RuntimeException error) {throw new IllegalStateException("Invalid editable Metallurgy Ender IO recipes in "+base,error);}
    }
    private EnderIoFiles() {}
}
