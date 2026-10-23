package it.hurts.metallurgy_reforged.fluid;
import it.hurts.metallurgy_reforged.material.Materials;
import java.util.*;

/**1.4.1 MetalStats.automaticTemperature uses the source ore tier, with the
 * original Constants tier map for alloys, rather than the tool harvest level. */
public final class FluidCatalog {
    public record Definition(String id,String block,int color,int temperature,int light,boolean tar) {}
    public static final List<Definition> ALL;
    static {
        String[] names={"adamantine","alduorite","amordrine","angmallen","astral_silver","atlarus","black_steel","brass","bronze","carmot","celenegil","ceruclase","copper","damascus_steel","deep_iron","desichalkos","electrum","etherium","eximite","haderoth","hepatizon","ignatius","infuscolium","inolashite","kalendrite","krik","lemurite","lutetium","manganese","meutoite","midasium","mithril","orichalcum","osmium","oureclase","platinum","prometheum","quicksilver","rubracium","sanguinite","shadow_iron","shadow_steel","silver","steel","tartarite","tin","vulcanite","vyroxeres","zinc"};
        int[] tiers={6,6,4,2,3,6,2,1,1,5,4,4,1,1,1,5,2,6,4,5,2,3,2,6,4,5,5,6,2,5,4,5,4,2,3,4,1,3,3,5,3,5,2,2,6,1,4,5,1};
        Map<String,Integer> defaults=new HashMap<>();for(int i=0;i<names.length;i++) defaults.put(names[i],tiers[i]);
        List<Definition> values=new ArrayList<>();
        for(var material:Materials.ALL) {
            int tier=material.oreLevel()>=0?material.oreLevel():defaults.get(material.name());
            values.add(new Definition(material.name(),"molten_"+material.name(),0xff000000|material.color(),1000+tier*200,9,false));
        }
        values.add(new Definition("molten_tar","molten_tar",0xff111419,400,0,true));
        values.add(new Definition("molten_thermite","molten_thermite",0xffc44205,4000,9,false));
        ALL=List.copyOf(values);
    }
    private FluidCatalog() {}
}
