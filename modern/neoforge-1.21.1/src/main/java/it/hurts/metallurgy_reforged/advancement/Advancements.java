package it.hurts.metallurgy_reforged.advancement;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.*;
import java.util.function.Supplier;
public final class Advancements {
    private static final DeferredRegister<CriterionTrigger<?>> TYPES=DeferredRegister.create(Registries.TRIGGER_TYPE,"metallurgy");
    private static final Map<String,Supplier<Trigger>> TRIGGERS=new LinkedHashMap<>();
    static {for(String kind:List.of("harvest_level","jack_of_all_trades","yin_yang","low_temperatures","overclocked_tools","loyal_friends","strong_opponent","advancement_granted")) TRIGGERS.put(kind,TYPES.register(kind,()->new Trigger(kind)));}
    public static void register(net.neoforged.bus.api.IEventBus bus) {TYPES.register(bus);}
    public static void fire(ServerPlayer player,String kind,int level,String advancement) {var trigger=TRIGGERS.get(kind);if(trigger!=null) trigger.get().fire(player,level,advancement);}
    private record Instance(Optional<ContextAwarePredicate> player,int level,String advancement) implements SimpleCriterionTrigger.SimpleInstance {}
    private static final class Trigger extends SimpleCriterionTrigger<Instance> {
        private final String kind;private final Codec<Instance> codec;
        Trigger(String kind) {
            this.kind=kind;
            MapCodec<String> advancement=kind.equals("advancement_granted")?ResourceLocation.CODEC.xmap(ResourceLocation::toString,ResourceLocation::parse).fieldOf("advancement"):Codec.STRING.optionalFieldOf("advancement","");
            codec=RecordCodecBuilder.create(instance->instance.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),Codec.INT.optionalFieldOf("level",100).forGetter(Instance::level),advancement.forGetter(Instance::advancement)).apply(instance,Instance::new));
        }
        @Override public Codec<Instance> codec() {return codec;}
        void fire(ServerPlayer player,int level,String advancement) {trigger(player,instance->kind.equals("harvest_level")?instance.level==level:!kind.equals("advancement_granted") || instance.advancement.equals(advancement));}
    }
    private Advancements() {}
}
