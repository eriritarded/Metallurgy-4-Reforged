package it.hurts.metallurgy_reforged.world;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class OreProfiles {
    public record Profile(String name,String block,String dimension,int min_y,int max_y,int vein_size,int attempts,List<String> biomes,String special) {}
    public static final List<Profile> ALL;
    static {
        try(var stream=OreProfiles.class.getResourceAsStream("/assets/metallurgy/legacy_worldgen.json")) {
            if(stream==null) throw new IOException("Missing source worldgen profiles");
            var data=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            List<Profile> profiles=new ArrayList<>();
            for(Profile profile:new Gson().fromJson(data.getAsJsonArray("ores"),Profile[].class)) {
                profiles.add(new Profile(profile.name,profile.block,profile.dimension,
                    it.hurts.metallurgy_reforged.config.ConfigFiles.oreInt(profile.name,"minY",profile.min_y),it.hurts.metallurgy_reforged.config.ConfigFiles.oreInt(profile.name,"maxY",profile.max_y),
                    it.hurts.metallurgy_reforged.config.ConfigFiles.oreInt(profile.name,"veinSize",profile.vein_size),it.hurts.metallurgy_reforged.config.ConfigFiles.rarity(profile.attempts),
                    it.hurts.metallurgy_reforged.config.ConfigFiles.oreBiomes(profile.name),profile.special));
            }
            ALL=List.copyOf(profiles);
            if(ALL.size()!=34) throw new IOException("Expected34 original default ore profiles");
        } catch(IOException|RuntimeException exception) { throw new ExceptionInInitializerError(exception); }
    }
    private OreProfiles() {}
}
