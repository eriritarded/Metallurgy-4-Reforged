package it.hurts.metallurgy_reforged.advancement;
import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import java.util.*;
public final class Advancements {
    private static final Map<String,Trigger> TRIGGERS=new LinkedHashMap<>();
    static {for(String kind:List.of("harvest_level","jack_of_all_trades","yin_yang","low_temperatures","overclocked_tools","loyal_friends","strong_opponent","advancement_granted")) TRIGGERS.put(kind,new Trigger(kind));}
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->TRIGGERS.values().forEach(CriteriaTriggers::register)));}
    public static void fire(ServerPlayer player,String kind,int level,String advancement) {var trigger=TRIGGERS.get(kind);if(trigger!=null) trigger.fire(player,level,advancement);}
    private static final class Trigger extends SimpleCriterionTrigger<Instance> {
        private final String kind;private final ResourceLocation id;
        Trigger(String kind) {this.kind=kind;id=new ResourceLocation("metallurgy",kind);}
        @Override public ResourceLocation getId() {return id;}
        @Override protected Instance createInstance(JsonObject json,ContextAwarePredicate player,DeserializationContext context) {return new Instance(id,player,GsonHelper.getAsInt(json,"level",100),kind.equals("advancement_granted")?new ResourceLocation(GsonHelper.getAsString(json,"advancement")).toString():"");}
        void fire(ServerPlayer player,int level,String advancement) {trigger(player,instance->kind.equals("harvest_level")?instance.level==level:!kind.equals("advancement_granted") || instance.advancement.equals(advancement));}
    }
    private static final class Instance extends AbstractCriterionTriggerInstance {
        private final int level;private final String advancement;
        Instance(ResourceLocation id,ContextAwarePredicate player,int level,String advancement) {super(id,player);this.level=level;this.advancement=advancement;}
        @Override public JsonObject serializeToJson(SerializationContext context) {var json=super.serializeToJson(context);json.addProperty("level",level);if(!advancement.isEmpty()) json.addProperty("advancement",advancement);return json;}
    }
    private Advancements() {}
}
