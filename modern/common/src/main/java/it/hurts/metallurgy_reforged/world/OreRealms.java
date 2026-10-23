package it.hurts.metallurgy_reforged.world;
import it.hurts.metallurgy_reforged.config.WorldGenerationConfig;
/** Native dimension types replace removed1.12 provider flags; explicit IDs win. */
public final class OreRealms {
    public static String realm(String id,boolean natural,boolean ultraWarm) {
        for(String entry:WorldGenerationConfig.oreDimensionOverrides) {
            int split=entry.lastIndexOf('=');
            if(split>0 && entry.substring(0,split).equals(id)) {String value=entry.substring(split+1);return value.equals("none")?"":value;}
        }
        return natural?"overworld":ultraWarm?"nether":"end";
    }
    private OreRealms() {}
}
