"""Explicit1.20.1 ->1.21.1 native adapter; shared behavior remains in common.

Maintained generated files are source deliverables. This script does not build,
extract dependencies, run tests or modify any installation.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'forge-1.20.1/src/main/java'
DEST = ROOT / 'neoforge-1.21.1/src/main/java'
PACKAGE = 'it/hurts/metallurgy_reforged/'


def adapt(text):
    text = text.replace('net.minecraftforge.eventbus', 'net.neoforged.bus')
    text = text.replace('net.minecraftforge.api.distmarker', 'net.neoforged.api.distmarker')
    text = text.replace('net.minecraftforge.fml', 'net.neoforged.fml')
    text = text.replace('net.minecraftforge.', 'net.neoforged.neoforge.')
    text = text.replace('RegistryObject<', 'Supplier<')
    text = re.sub(r'new ResourceLocation\(', 'ResourceLocation.fromNamespaceAndPath(', text)
    text = text.replace('import net.neoforged.neoforge.registries.ForgeRegistries;', 'import net.minecraft.core.registries.BuiltInRegistries;')
    text = text.replace('ForgeRegistries.ITEMS', 'BuiltInRegistries.ITEM').replace('ForgeRegistries.BLOCKS', 'BuiltInRegistries.BLOCK')
    return text


def main():
    for path in SOURCE.rglob('*.java'):
        relative = path.relative_to(SOURCE)
        name = relative.as_posix().removeprefix(PACKAGE)
        if name.startswith(('compat/tconstruct/','compat/spartan/')) or name in ['compat/ModularTiers.java','compat/OptionalArmorGuard.java','mixin/TinkerArmorMenuMixin.java', 'mixin/BlockLootMixin.java','mixin/VulcaniteExplosionMixin.java']:
            # Native NeoForge uses incorrect-block tags, not Forge's sorted tiers.
            continue
        t = adapt(path.read_text(encoding='utf-8'))
        if name == 'Metallurgy.java':
            t = '''package it.hurts.metallurgy_reforged;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.effect.EffectEvents;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
@Mod("metallurgy")
public final class Metallurgy {
    public Metallurgy(IEventBus bus) {
        it.hurts.metallurgy_reforged.config.ConfigBootstrap.initialize();
        it.hurts.metallurgy_reforged.config.ContentCondition.register(bus);
        it.hurts.metallurgy_reforged.config.IntegrationCondition.register(bus);
        it.hurts.metallurgy_reforged.recipe.ProcessingRegistration.register(bus);
        it.hurts.metallurgy_reforged.world.WorldRegistration.register(bus);
        it.hurts.metallurgy_reforged.advancement.Advancements.register(bus);
        it.hurts.metallurgy_reforged.compat.OptionalPacks.register(bus);
        Content.register(bus);
        NeoForge.EVENT_BUS.register(EffectEvents.class);
        NeoForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.gadget.BlockGadgetEvents.class);
        NeoForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.config.GeneralEvents.class);
        NeoForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.world.RetrogenEvents.class);
        NeoForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.advancement.AdvancementEvents.class);
        NeoForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.effect.ItemAttributes.class);
        bus.addListener(this::capabilities);
        bus.addListener(it.hurts.metallurgy_reforged.network.ArmorNetwork::register);
        bus.addListener(it.hurts.metallurgy_reforged.network.VisualNetwork::register);
    }
    private void capabilities(RegisterCapabilitiesEvent event) {
        it.hurts.metallurgy_reforged.compat.ExternalHeat.register(event);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,Content.CHEST_ENTITY.get(),(chest,side)->new InvWrapper(chest));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,Content.MACHINE_ENTITY.get(),(machine,side)->side==null?new InvWrapper(machine):new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(machine,side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,Content.CHAMBER_ENTITY.get(),(machine,side)->side==null?new InvWrapper(machine):new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(machine,side));
    }
}
'''
        elif name == 'compat/JeiPlugin.java':
            t=t.replace('helper.create(fluid,1000)', 'helper.create(fluid.builtInRegistryHolder(),1000)')
        elif name == 'compat/JeiCrafting.java':
            t=t.replace('r instanceof DetectorRecipe:r instanceof ParagonRecipe','r.value() instanceof DetectorRecipe:r.value() instanceof ParagonRecipe')
            t=t.replace('List<CraftingRecipe> recipes=new ArrayList<>();', 'List<RecipeHolder<CraftingRecipe>> recipes=new ArrayList<>();')
            t=t.replace('recipes.add(builder.build());', 'recipes.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("metallurgy","jei_examples/"+name),builder.build()));')
        elif name == 'compat/ConfiguredPack.java':
            t=t.replace('"forge-1.20.1"','"neoforge-1.21.1"').replace('"recipes/compat/enderio/"','"recipe/compat/enderio/"')
            t=t.replace('@Override public boolean isBuiltin() {return true;}', '@Override public net.minecraft.server.packs.PackLocationInfo location() {return delegate.location();}')
        elif name in ['client/ProcessingClient.java','recipe/ProcessingEvents.java']:
            t=t.replace('import net.neoforged.fml.common.Mod;', 'import net.neoforged.fml.common.EventBusSubscriber;').replace('@Mod.EventBusSubscriber','@EventBusSubscriber')
        elif name == 'config/IntegrationCondition.java':
            t='''package it.hurts.metallurgy_reforged.config;
import com.mojang.serialization.*;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.*;
public record IntegrationCondition(String integration) implements ICondition {
    private static final MapCodec<IntegrationCondition> CODEC=Codec.STRING.fieldOf("integration").xmap(IntegrationCondition::new,IntegrationCondition::integration);
    private static final DeferredRegister<MapCodec<? extends ICondition>> SERIALIZERS=DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS,"metallurgy");
    static {SERIALIZERS.register("integration_enabled",()->CODEC);}
    public static void register(net.neoforged.bus.api.IEventBus bus) {SERIALIZERS.register(bus);}
    @Override public boolean test(IContext context) {return IntegrationFlags.enabled(integration);}
    @Override public MapCodec<? extends ICondition> codec() {return CODEC;}
}
'''
        elif name == 'compat/ExternalHeat.java':
            t=t.replace('    private ExternalHeat() {}','''    @SuppressWarnings("unchecked") public static void register(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        var capability=(net.neoforged.neoforge.capabilities.BlockCapability<Object,net.minecraft.core.Direction>)capability();
        if(capability!=null) event.registerBlockEntity(capability,it.hurts.metallurgy_reforged.content.Content.MACHINE_ENTITY.get(),(machine,side)->side==null?machine.externalHeater():null);
    }
    private ExternalHeat() {}''')
        elif name == 'compat/OptionalPacks.java':
            t='''package it.hurts.metallurgy_reforged.compat;
import it.hurts.metallurgy_reforged.config.GeneralConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.*;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.fml.ModList;
public final class OptionalPacks {
    public static void register(net.neoforged.bus.api.IEventBus bus) {bus.addListener(OptionalPacks::packs);}
    private static void packs(AddPackFindersEvent event) {
        if(event.getPackType()==PackType.SERVER_DATA && GeneralConfig.silentGemsIntegration && ModList.get().isLoaded("silentgear"))
            event.addPackFinders(ResourceLocation.fromNamespaceAndPath("metallurgy","resourcepacks/silentgear"),PackType.SERVER_DATA,Component.literal("Metallurgy: Silent Gear"),PackSource.BUILT_IN,true,Pack.Position.BOTTOM);
    }
    private OptionalPacks() {}
}
'''
        elif name == 'advancement/AdvancementEvents.java':
            t=t.replace('event.getAdvancement().getId()', 'event.getAdvancement().id()')
        elif name == 'advancement/Advancements.java':
            t='''package it.hurts.metallurgy_reforged.advancement;
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
'''
        elif name == 'world/RetrogenEvents.java':
            t='''package it.hurts.metallurgy_reforged.world;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.*;
import java.util.function.Supplier;
public final class RetrogenEvents {
    private static final DeferredRegister<AttachmentType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,"metallurgy");
    private static final Supplier<AttachmentType<Boolean>> GENERATED=TYPES.register("ore_generated",()->AttachmentType.builder(()->false).serialize(Codec.BOOL).build());
    public static void register(net.neoforged.bus.api.IEventBus bus) {TYPES.register(bus);}
    @SubscribeEvent public static void load(ChunkEvent.Load event) {
        if(!(event.getLevel() instanceof ServerLevel level)) return;
        if(event.isNewChunk()) generated(event.getChunk());
        else if(!event.getChunk().getData(GENERATED.get())) Retrogen.loaded(level,event.getChunk().getPos());
    }
    @SubscribeEvent public static void unload(ChunkEvent.Unload event) {if(event.getLevel() instanceof ServerLevel level) Retrogen.unloaded(level,event.getChunk().getPos());}
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {if(event.getLevel() instanceof ServerLevel level) Retrogen.tick(level);}
    public static void generated(ChunkAccess chunk) {chunk.setData(GENERATED.get(),true);chunk.setUnsaved(true);}
    private RetrogenEvents() {}
}
'''
        elif name == 'config/ContentCondition.java':
            t='''package it.hurts.metallurgy_reforged.config;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.*;
public record ContentCondition(String content) implements ICondition {
    private static final MapCodec<ContentCondition> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(Codec.STRING.fieldOf("content").forGetter(ContentCondition::content)).apply(instance,ContentCondition::new));
    private static final DeferredRegister<MapCodec<? extends ICondition>> SERIALIZERS=DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS,"metallurgy");
    static {SERIALIZERS.register("content_enabled",()->CODEC);}
    public static void register(net.neoforged.bus.api.IEventBus bus) {SERIALIZERS.register(bus);}
    @Override public boolean test(IContext context) {return ContentAvailability.enabled(content);}
    @Override public MapCodec<? extends ICondition> codec() {return CODEC;}
}
'''
        elif name in ('world/TarBiomeModifier.java','world/OreBiomeModifier.java'):
            t=t.replace('import com.mojang.serialization.Codec;','import com.mojang.serialization.MapCodec;').replace('Codec<','MapCodec<').replace('RecordCodecBuilder.create(', 'RecordCodecBuilder.mapCodec(')
        elif name == 'world/WorldRegistration.java':
            t=t.replace('import com.mojang.serialization.Codec;','import com.mojang.serialization.MapCodec;').replace('Codec<','MapCodec<').replace('ForgeRegistries.Keys','net.neoforged.neoforge.registries.NeoForgeRegistries.Keys')
        elif name == 'mixin/CorpseRenderMixin.java':
            t=t.replace('IIFFFF)V','III)V')
            start=t.index('    private void metallurgy$tint(')
            t=t[:start]+'''    private void metallurgy$tint(Args args) {if(metallurgy$corpse) args.set(4,CorpseRenderData.tint(args.<Integer>get(4)));}
}
'''
        elif name == 'client/PierKnightRenderer.java':
            t=t.replace('int overlay,float red,float green,float blue,float alpha) { super.renderToBuffer(pose,vertices,light,overlay,red,green,blue,alpha*fade); }', 'int overlay,int color) { int alpha=(int)((color>>>24)*fade);super.renderToBuffer(pose,vertices,light,overlay,(color&0xffffff)|(alpha<<24)); }')
        elif name == 'entity/PierKnight.java':
            t=t.replace('BlockPathTypes','PathType').replace('canBeLeashed(Player player)', 'canBeLeashed()')
            t=t.replace('defineSynchedData() { super.defineSynchedData();entityData.define(THICKNESS,1);entityData.define(WIDE,false);entityData.define(VANISH,40); }','defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder);builder.define(THICKNESS,1);builder.define(WIDE,false);builder.define(VANISH,40); }')
            t=t.replace('dropAllDeathLoot(DamageSource source)', 'dropAllDeathLoot(ServerLevel level,DamageSource source)')
        elif name == 'entity/VulcanitePrimedTnt.java':
            t=t.replace('super(type,level);', 'super(type,level);setBlockState(Content.BLOCK_MAP.get("vulcanite_tnt").get().defaultBlockState());')
            t=t.replace('    @Override protected void explode() {level().explode(this,getX(),getY(),getZ(),6,false,Level.ExplosionInteraction.TNT);}', '''    @Override protected void explode() {
        var calculator=new EntityBasedExplosionDamageCalculator(this) {
            @Override public float getEntityDamageAmount(Explosion explosion,Entity entity) {return (float)Math.floor((super.getEntityDamageAmount(explosion,entity)+1)/2);}
        };
        level().explode(this,Explosion.getDefaultDamageSource(level(),this),calculator,getX(),getY(),getZ(),6,false,Level.ExplosionInteraction.TNT);
    }''')
        elif name == 'gadget/VulcaniteTntBlock.java':
            t=t.replace('    public VulcaniteTntBlock(Properties properties)', '    private static final com.mojang.serialization.MapCodec<TntBlock> CODEC=simpleCodec(VulcaniteTntBlock::new);\n    @Override public com.mojang.serialization.MapCodec<TntBlock> codec() {return CODEC;}\n    public VulcaniteTntBlock(Properties properties)')
        elif name == 'client/VulcaniteTntRenderer.java':
            t='''package it.hurts.metallurgy_reforged.client;
import net.minecraft.client.renderer.entity.*;
public final class VulcaniteTntRenderer extends TntRenderer {
    public VulcaniteTntRenderer(EntityRendererProvider.Context context) {super(context);}
}
'''
        elif name == 'gadget/GadgetItem.java':
            start=t.index('    @Override public boolean canApplyAtEnchantingTable(');end=t.index('    @Override public boolean isFoil(',start)
            t=t[:start]+'''    @Override public boolean supportsEnchantment(ItemStack stack,net.minecraft.core.Holder<Enchantment> enchantment) {
        return (id.equals("brass_knuckles") || id.equals("rubracium_gauntlet")) && java.util.List.of("bane_of_arthropods","smite","mending","vanishing_curse","unbreaking","sharpness").stream().anyMatch(name->enchantment.is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,ResourceLocation.fromNamespaceAndPath("minecraft",name))));
    }
'''+t[end:]
            t=t.replace('import net.minecraft.world.item.enchantment.*;', 'import net.minecraft.world.item.enchantment.*;\nimport net.minecraft.resources.ResourceLocation;')
        elif name == 'gadget/GogglesItem.java':
            t=t.replace('EquipmentSlot slot,Entity entity)', 'EquipmentSlot slot,LivingEntity entity)')
        elif name == 'gadget/BlockGadgetEvents.java':
            t=t.replace('import net.neoforged.bus.api.Event;','')
            t=t.replace('Event.Result.DENY','MobSpawnEvent.PositionCheck.Result.FAIL')
        elif name in ['gadget/RoadBlock.java','gadget/PhosphorusLampBlock.java']:
            cls=path.stem;at=t.index('    @Override protected void createBlockStateDefinition')
            t=t[:at]+f'    private static final com.mojang.serialization.MapCodec<{cls}> CODEC=simpleCodec({cls}::new);\n    @Override protected com.mojang.serialization.MapCodec<{cls}> codec() {{return CODEC;}}\n'+t[at:]
        elif name == 'gadget/MetallurgyShieldItem.java':
            t=t.replace('getUseDuration(ItemStack stack)', 'getUseDuration(ItemStack stack,LivingEntity user)')
        elif name == 'gadget/IceShieldBlock.java':
            t=t.replace('extends GlassBlock','extends TransparentBlock')
            at=t.index('    @Override protected void createBlockStateDefinition')
            t=t[:at]+'''    private static final com.mojang.serialization.MapCodec<IceShieldBlock> CODEC=simpleCodec(IceShieldBlock::new);
    @Override protected com.mojang.serialization.MapCodec<IceShieldBlock> codec() {return CODEC;}
'''+t[at:]
        elif name == 'gadget/GadgetBridge.java':
            t=t.replace('snapshot.restore(true,false)', 'snapshot.restore(3)')
            t=t.replace('net.neoforged.neoforge.event.ForgeEventFactory.onBlockPlace','net.neoforged.neoforge.event.EventHooks.onBlockPlace')
            t=t.replace('stack.hurtAndBreak(amount,entity,e->e.broadcastBreakEvent(slot))','stack.hurtAndBreak(amount,entity,slot)')
            t=t.replace('net.neoforged.neoforge.common.MinecraftForge.EVENT_BUS.post(event)', 'net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled()')
        elif name == 'network/VisualNetwork.java':
            t='''package it.hurts.metallurgy_reforged.network;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;
public final class VisualNetwork {
    private static final Map<ServerPlayer,Integer> SENT=Collections.synchronizedMap(new WeakHashMap<>());
    public record Debris(it.hurts.metallurgy_reforged.visual.ExplosionVisual data) implements CustomPacketPayload {
        public static final Type<Debris> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("metallurgy","explosion_particles"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Debris> CODEC=StreamCodec.of((buffer,message)->message.data.write(buffer),buffer->new Debris(it.hurts.metallurgy_reforged.visual.ExplosionVisual.read(buffer)));
        @Override public Type<Debris> type() {return TYPE;}
    }
    public record Activation(int entity,net.minecraft.world.item.ItemStack stack) implements CustomPacketPayload {
        public static final Type<Activation> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("metallurgy","armor_activation"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Activation> CODEC=StreamCodec.of((buffer,message)->{buffer.writeVarInt(message.entity);net.minecraft.world.item.ItemStack.STREAM_CODEC.encode(buffer,message.stack);},buffer->new Activation(buffer.readVarInt(),net.minecraft.world.item.ItemStack.STREAM_CODEC.decode(buffer)));
        @Override public Type<Activation> type() {return TYPE;}
    }
    public record Height(int height) implements CustomPacketPayload {
        public static final Type<Height> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("metallurgy","krik_height"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Height> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Height::height,Height::new);
        @Override public Type<Height> type() {return TYPE;}
    }
    public record Particles(it.hurts.metallurgy_reforged.visual.ParticleBurst data) implements CustomPacketPayload {
        public static final Type<Particles> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("metallurgy","ore_particles"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Particles> CODEC=StreamCodec.of((buffer,message)->message.data.write(buffer),buffer->new Particles(it.hurts.metallurgy_reforged.visual.ParticleBurst.read(buffer)));
        @Override public Type<Particles> type() {return TYPE;}
    }
    public record Sound(it.hurts.metallurgy_reforged.visual.SoundCue data) implements CustomPacketPayload {
        public static final Type<Sound> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("metallurgy","machine_sound"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Sound> CODEC=StreamCodec.of((buffer,message)->message.data.write(buffer),buffer->new Sound(it.hurts.metallurgy_reforged.visual.SoundCue.read(buffer)));
        @Override public Type<Sound> type() {return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("4").playToClient(Activation.TYPE,Activation.CODEC,(message,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.client.ArmorActivation.show(message.entity,message.stack)));
        event.registrar("4").playToClient(Debris.TYPE,Debris.CODEC,(message,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.client.ExplosionParticles.burst(message.data)));
        event.registrar("4").playToClient(Height.TYPE,Height.CODEC,(message,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.client.HudRenderer.height(message.height)));
        event.registrar("4").playToClient(Particles.TYPE,Particles.CODEC,(message,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.client.OreParticles.burst(message.data)));
        event.registrar("4").playToClient(Sound.TYPE,Sound.CODEC,(message,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.client.MachineAmbience.cue(message.data)));
    }
    public static void height(ServerPlayer player,int height) {int value=Math.max(0,Math.min(27,height));Integer previous=SENT.put(player,value);if(previous==null || previous!=value) PacketDistributor.sendToPlayer(player,new Height(value));}
    public static void explosion(net.minecraft.server.level.ServerLevel level,it.hurts.metallurgy_reforged.visual.ExplosionVisual data) {if(data.valid()) for(var player:level.players()) if(player.distanceToSqr(data.x(),data.y(),data.z())<=4096) PacketDistributor.sendToPlayer(player,new Debris(data));}
    public static void activation(net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.item.ItemStack stack) {if(entity instanceof ServerPlayer player) PacketDistributor.sendToPlayer(player,new Activation(entity.getId(),stack.copy()));}
    public static void clear(ServerPlayer player) {SENT.remove(player);}
    public static void burst(net.minecraft.server.level.ServerLevel level,it.hurts.metallurgy_reforged.visual.ParticleBurst data) {if(data.valid()) for(var player:level.players()) if(player.distanceToSqr(data.x(),data.y(),data.z())<=4096) PacketDistributor.sendToPlayer(player,new Particles(data));}
    public static void sound(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos,int kind) {var data=new it.hurts.metallurgy_reforged.visual.SoundCue(it.hurts.metallurgy_reforged.effect.VersionBridge.dimensionId(level),pos,kind);if(data.valid()) for(var player:level.players()) if(player.distanceToSqr(pos.getX(),pos.getY(),pos.getZ())<=4096) PacketDistributor.sendToPlayer(player,new Sound(data));}
    private VisualNetwork() {}
}
'''
        elif name == 'network/ArmorNetwork.java':
            t='''package it.hurts.metallurgy_reforged.network;
import it.hurts.metallurgy_reforged.effect.ArmorControls;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
public final class ArmorNetwork {
    public record Action(int id) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("metallurgy","armor_actions"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Action::id,Action::new);
        @Override public Type<Action> type() { return TYPE; }
    }
    public record Phase(boolean active) implements CustomPacketPayload {
        public static final Type<Phase> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("metallurgy","etherium_phase"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Phase> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,Phase::active,Phase::new);
        @Override public Type<Phase> type() { return TYPE; }
    }
    public record Corpse(int entityId,int stage) implements CustomPacketPayload {
        public static final Type<Corpse> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("metallurgy","corpse_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Corpse> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Corpse::entityId,ByteBufCodecs.VAR_INT,Corpse::stage,Corpse::new);
        @Override public Type<Corpse> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("3").playToServer(Action.TYPE,Action.CODEC,(message,context)->context.enqueueWork(()->{if(context.player() instanceof ServerPlayer player) ArmorControls.action(player,message.id());}));
        event.registrar("3").playToClient(Phase.TYPE,Phase.CODEC,(message,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.effect.ArmorPhasing.clientFlag(context.player(),message.active())));
        event.registrar("3").playToClient(Corpse.TYPE,Corpse.CODEC,(message,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.effect.CorpseLifecycle.applyClient(context.player(),message.entityId(),message.stage())));
    }
    public static void send(int action) { PacketDistributor.sendToServer(new Action(action)); }
    public static void phase(ServerPlayer player,boolean active) { PacketDistributor.sendToPlayer(player,new Phase(active)); }
    public static void corpse(net.minecraft.world.entity.LivingEntity entity,int stage) { PacketDistributor.sendToPlayersTrackingEntity(entity,new Corpse(entity.getId(),stage)); }
    public static void corpse(ServerPlayer player,net.minecraft.world.entity.LivingEntity entity,int stage) { PacketDistributor.sendToPlayer(player,new Corpse(entity.getId(),stage)); }
    private ArmorNetwork() {}
}
'''
        elif name == 'client/ChamberParticles.java':
            t=t.replace('level.addParticle(ParticleTypes.ENTITY_EFFECT,x,y,z,(color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F)', 'level.addParticle(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,0xff000000|color),x,y,z,0,0,0)')
        elif name == 'client/GadgetTooltips.java':
            t=t.replace('var data=BlockItem.getBlockEntityData(stack);if(data==null) return;', 'var component=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);if(component==null) return;var data=component.copyTag();')
        elif name == 'client/KnucklesRenderer.java':
            t=t.replace('player.getModelName().equals("slim")', 'player.getSkin().model()==net.minecraft.client.resources.PlayerSkin.Model.SLIM').replace('player.getSkinTextureLocation()', 'player.getSkin().texture()').replace('mc.getFrameTime()', 'mc.getTimer().getGameTimeDeltaPartialTick(false)')
        elif name in ['client/GadgetProperties.java','client/GadgetVisuals.java','client/ItemVisuals.java','client/HudRenderer.java','client/GadgetOverlay.java','client/KnucklesModel.java']:
            t=t.replace('import net.neoforged.fml.common.Mod;', 'import net.neoforged.fml.common.EventBusSubscriber;').replace('@Mod.EventBusSubscriber','@EventBusSubscriber').replace('Mod.EventBusSubscriber.Bus.MOD','EventBusSubscriber.Bus.MOD')
            if name=='client/GadgetVisuals.java':
                t=t.replace('    @SubscribeEvent public static void attack(InputEvent event) {', '''    // Both callbacks run after native key mappings receive input; the grouping type is abstract.
    @SubscribeEvent public static void attackMouse(InputEvent.MouseButton.Post event) { attack(); }
    @SubscribeEvent public static void attackKey(InputEvent.Key event) { attack(); }
    private static void attack() {''')
            if name=='client/GadgetOverlay.java': t=t.replace('event.getPartialTick()', 'event.getPartialTick().getGameTimeDeltaPartialTick(false)')
        elif name in ['client/ArmorInput.java','client/ArmorKeys.java']:
            t=t.replace('import net.neoforged.fml.common.Mod;', 'import net.neoforged.fml.common.EventBusSubscriber;').replace('@Mod.EventBusSubscriber','@EventBusSubscriber').replace('Mod.EventBusSubscriber.Bus.MOD','EventBusSubscriber.Bus.MOD')
            t=t.replace('import net.neoforged.neoforge.event.TickEvent;', 'import net.neoforged.neoforge.client.event.ClientTickEvent;')
            t=t.replace('tick(TickEvent.ClientTickEvent event) { if(event.phase==TickEvent.Phase.END) update(); }','tick(ClientTickEvent.Post event) { update(); }')
        elif name == 'content/Content.java':
            t=t.replace(' { @Override public int getMaxDamage(ItemStack stack) { return it.hurts.metallurgy_reforged.effect.Paragon.maximum(stack,super.getMaxDamage(stack)); } }','')
            t = t.replace('new GlassBlock(', 'new TransparentBlock(')
            t = t.replace('IForgeMenuType', 'IMenuTypeExtension')
            t = t.replace('public static final DeferredRegister<MenuType<?>> MENUS', 'public static final DeferredRegister<ArmorMaterial> ARMOR = DeferredRegister.create(Registries.ARMOR_MATERIAL, "metallurgy");\n    public static final DeferredRegister<MenuType<?>> MENUS')
            for kind, damage, speed in [('Sword', '3', '-2.4F'), ('Axe', 'material.tool("damage",0)*0.25F+3', '-2.5F-material.tool("damage",0)/10'), ('Pickaxe', '1', '-2.8F'), ('Shovel', '1.5F', '-3'), ('Hoe', '-(int)material.tool("damage", 0)', 'material.tool("damage",0)-3')]:
                t = t.replace(f'new {kind}Item(tier, {damage}, {speed}, new Item.Properties())', f'new {kind}Item(tier, new Item.Properties().attributes({kind}Item.createAttributes(tier, {damage}, {speed})))')
            start = t.index('                ArmorMaterial armor = new MetalArmor(material);')
            end = t.index('\n            }', start)
            t = t[:start] + '''                net.neoforged.neoforge.registries.DeferredHolder<ArmorMaterial,ArmorMaterial> armor = ARMOR.register(name, () -> new ArmorMaterial(
                    Map.of(ArmorItem.Type.BOOTS,material.defense(0),ArmorItem.Type.LEGGINGS,material.defense(1),ArmorItem.Type.CHESTPLATE,material.defense(2),ArmorItem.Type.HELMET,material.defense(3)),
                    (int)material.armor("enchantability",10),SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ITEM_MAP.get(name + "_ingot").get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("metallurgy",name))),
                    material.armor("toughness",0),0));
                for(ArmorItem.Type type:List.of(ArmorItem.Type.BOOTS,ArmorItem.Type.LEGGINGS,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.HELMET)) {
                    item(name+"_"+type.getName(), () -> new ArmorItem(armor,type,new Item.Properties().durability(type.getDurability((int)material.armor("durability",15)))));
                }''' + t[end:]
            t = t.replace('TABS.register(bus);', 'TABS.register(bus); ARMOR.register(bus);')
            t = t.replace('public int getLevel() { return (int)data.tool("harvest_level", 2); }', 'public net.minecraft.tags.TagKey<Block> getIncorrectBlocksForDrops() { return net.minecraft.tags.TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("metallurgy","incorrect_for_level_"+(int)data.tool("harvest_level",2))); }')
            start = t.index('    private record MetalArmor(')
            end = t.index('    private static final class MetalOre', start)
            t = t[:start] + t[end:]
            start = t.index('        @Override public boolean canHarvestBlock(')
            end = t.index('\n        }', start) + len('\n        }')
            t = t[:start] + '        // Native Tool component/tag tier checks determine harvest eligibility.' + t[end:]
        elif name == 'inventory/MetalChestEntity.java':
            t = t.replace('import net.neoforged.neoforge.common.capabilities.*;', '')
            t = t.replace('import net.neoforged.neoforge.common.util.LazyOptional;', '')
            t = re.sub(r'^    private LazyOptional<.*\n', '', t, flags=re.M)
            start = t.index('    @Override public <T> LazyOptional<T>')
            end = t.index('    @Override public void startOpen', start)
            t = t[:start] + t[end:]
            t = t.replace('@Override public void load(CompoundTag tag)', '@Override protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries)')
            t = t.replace('super.load(tag);', 'super.loadAdditional(tag,registries);')
            t = t.replace('ContainerHelper.loadAllItems(tag, items);', 'ContainerHelper.loadAllItems(tag, items, registries);')
            t = t.replace('saveAdditional(CompoundTag tag) { super.saveAdditional(tag); if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items);', 'saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) { super.saveAdditional(tag,registries); if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items, registries);')
        elif name == 'inventory/MetalChestBlock.java':
            t = t.replace('import net.neoforged.neoforge.network.NetworkHooks;', '')
            t = t.replace('    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)', '    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)')
            t = t.replace('NetworkHooks.openScreen(server, chest,', 'server.openMenu(chest,')
            at = t.index('    @Override protected void createBlockStateDefinition')
            t = t[:at] + '''    private static final com.mojang.serialization.MapCodec<MetalChestBlock> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
        com.mojang.serialization.Codec.INT.fieldOf("capacity").forGetter(block -> block.capacity), propertiesCodec()).apply(instance,MetalChestBlock::new));
    @Override protected com.mojang.serialization.MapCodec<MetalChestBlock> codec() { return CODEC; }
''' + t[at:]
        elif name == 'effect/EffectEvents.java':
            t=t.replace('gadgetTick(TickEvent.PlayerTickEvent event) { if(event.phase==TickEvent.Phase.START) {it.hurts.metallurgy_reforged.gadget.GadgetLogic.tick(event.player);it.hurts.metallurgy_reforged.gadget.ShieldLogic.tick(event.player);} }','gadgetTick(PlayerTickEvent.Pre event) {it.hurts.metallurgy_reforged.gadget.GadgetLogic.tick(event.getEntity());it.hurts.metallurgy_reforged.gadget.ShieldLogic.tick(event.getEntity());}')
            t=t.replace('net.neoforged.neoforge.event.entity.living.ShieldBlockEvent','net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent')
            t=t.replace('{it.hurts.metallurgy_reforged.gadget.ShieldLogic.blocked(event.getEntity(),event.getDamageSource(),event.getBlockedDamage());}', '{if(event.getBlocked()) it.hurts.metallurgy_reforged.gadget.ShieldLogic.blocked(event.getEntity(),event.getDamageSource(),event.getBlockedDamage());}')
            t=t.replace('livingTick(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingTickEvent event) { ArmorEffects.tick(event.getEntity());it.hurts.metallurgy_reforged.gadget.GadgetLogic.livingTick(event.getEntity()); }','livingTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) { if(event.getEntity() instanceof LivingEntity entity) {ArmorEffects.tick(entity);it.hurts.metallurgy_reforged.gadget.GadgetLogic.livingTick(entity);} }')
            t=t.replace('        event.setExpToDrop(ToolEffects.experience(event.getPlayer(),event.getState(),event.getExpToDrop()));','')
            t=t.replace('    private EffectEvents() {}','''    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void toolDrops(net.neoforged.neoforge.event.level.BlockDropsEvent event) {
        if(!(event.getBreaker() instanceof net.minecraft.world.entity.player.Player player)) return;
        event.setDroppedExperience(ToolEffects.experience(player,event.getState(),event.getDroppedExperience()));
        var entities=event.getDrops();java.util.List<ItemStack> stacks=new java.util.ArrayList<>();
        for(var entity:entities) stacks.add(entity.getItem());
        var changed=ToolEffects.drops(event.getLevel(),event.getPos(),event.getState(),event.getBlockEntity(),player,event.getTool(),stacks);
        if(changed==stacks) return;
        int existing=entities.size();
        for(int i=0;i<changed.size();i++) {
            if(i<existing) entities.get(i).setItem(changed.get(i));
            else entities.add(new net.minecraft.world.entity.item.ItemEntity(event.getLevel(),event.getPos().getX()+0.5,event.getPos().getY()+0.5,event.getPos().getZ()+0.5,changed.get(i)));
        }
        while(entities.size()>changed.size()) entities.remove(entities.size()-1);
    }
    private EffectEvents() {}''')
            t=t.replace('ToolEffects.tick(event.player);','ToolEffects.tick(event.getEntity());')
            t=t.replace('tick(TickEvent.PlayerTickEvent event) { if(event.phase==TickEvent.Phase.START) { WeaponLifecycle.tick(event.player);AllEquipmentEffects.tick(event.player);ArmorLifecycle.tick(event.player);ToolEffects.tick(event.getEntity()); } }','tick(PlayerTickEvent.Pre event) { WeaponLifecycle.tick(event.getEntity());AllEquipmentEffects.tick(event.getEntity());ArmorLifecycle.tick(event.getEntity());ToolEffects.tick(event.getEntity()); }')
            t=t.replace('net.neoforged.neoforge.event.entity.living.LivingAttackEvent', 'net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent')
            t=t.replace('tick(TickEvent.PlayerTickEvent event) { if(event.phase==TickEvent.Phase.START) { WeaponLifecycle.tick(event.player);AllEquipmentEffects.tick(event.player);ArmorLifecycle.tick(event.player); } }', 'tick(PlayerTickEvent.Pre event) { WeaponLifecycle.tick(event.getEntity());AllEquipmentEffects.tick(event.getEntity());ArmorLifecycle.tick(event.getEntity()); }')
            t=t.replace('livingTick(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingTickEvent event) { ArmorEffects.tick(event.getEntity()); }', 'livingTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) { if(event.getEntity() instanceof LivingEntity entity) ArmorEffects.tick(entity); }')
            t=t.replace('event.setNewTarget(', 'event.setNewAboutToBeSetTarget(').replace('event.getNewTarget()', 'event.getNewAboutToBeSetTarget()')
            t = t.replace('import net.neoforged.neoforge.event.TickEvent;', 'import net.neoforged.neoforge.event.tick.PlayerTickEvent;')
            t = t.replace('tick(TickEvent.PlayerTickEvent event) { if(event.phase==TickEvent.Phase.START) WeaponLifecycle.tick(event.player); }', 'tick(PlayerTickEvent.Pre event) { WeaponLifecycle.tick(event.getEntity()); }')
            t = t.replace('tick(TickEvent.PlayerTickEvent event) { if(event.phase==TickEvent.Phase.START) { WeaponLifecycle.tick(event.player);AllEquipmentEffects.tick(event.player); } }', 'tick(PlayerTickEvent.Pre event) { WeaponLifecycle.tick(event.getEntity());AllEquipmentEffects.tick(event.getEntity()); }')
            t = t.replace('LivingHurtEvent', 'LivingIncomingDamageEvent')
            t = t.replace('weapon.hasTag() && "amordrine".equals(weapon.getTag().getString("paragon"))', '"amordrine".equals(weapon.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getString("paragon"))')
            t = t.replace('finalDamage(LivingDamageEvent event)', 'finalDamage(LivingDamageEvent.Pre event)')
            t = t.replace('WeaponDamage.afterArmor(event.getEntity(),event.getSource(),event.getAmount())', 'WeaponDamage.afterArmor(event.getEntity(),event.getSource(),event.getNewDamage())')
            t = t.replace('event.setAmount(result.amount());if(result.cancelled()) event.setCanceled(true);', 'event.setNewDamage(result.cancelled()?0:result.amount());')
        elif name == 'fluid/FluidContent.java':
            t=t.replace('ForgeFlowingFluid','BaseFlowingFluid')
            t=t.replace('ForgeRegistries.Keys.FLUID_TYPES','net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES')
            start=t.index('                @Override public void initializeClient')
            end=t.index('\n                }',start)+len('\n                }')
            t=t[:start]+t[end:]
        elif name == 'fluid/MetallurgyLiquidBlock.java':
            t=t.replace('super(entry.still,properties)','super(entry.still.get(),properties)')
        elif name == 'client/FluidRendering.java':
            t=t.replace('public final class FluidRendering', '@net.neoforged.fml.common.EventBusSubscriber(modid="metallurgy",value=net.neoforged.api.distmarker.Dist.CLIENT,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)\npublic final class FluidRendering')
            t=t.replace('    private FluidRendering() {}','''    @net.neoforged.bus.api.SubscribeEvent
    public static void register(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        for(var entry:it.hurts.metallurgy_reforged.fluid.FluidContent.ENTRIES.values()) event.registerFluidType(extensions(entry.definition),entry.type.get());
    }
    private FluidRendering() {}''')
        elif name == 'effect/ToolBridge.java':
            t=t.replace('net.neoforged.neoforge.common.MinecraftForge','net.neoforged.neoforge.common.NeoForge').replace('MinecraftForge.EVENT_BUS.post(event)','NeoForge.EVENT_BUS.post(event).isCanceled()')
            t=t.replace('ItemStack.isSameItemSameTags','ItemStack.isSameItemSameComponents')
            t=t.replace('Enchantments.BLOCK_FORTUNE','player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE)')
            t=t.replace('state.canHarvestBlock(player.level(),pos,player)','player.getMainHandItem().isCorrectToolForDrops(state)')
            t=t.replace('Blocks.GRASS','Blocks.SHORT_GRASS')
        elif name == 'effect/ItemAttributes.java':
            start=t.index('            if(entry.slot()!=event.getSlotType())')
            end=t.index('\n        }',start)
            t=t[:start]+'''            net.minecraft.core.Holder<Attribute> attribute=entry.attribute().equals("reach_distance")?Attributes.BLOCK_INTERACTION_RANGE:
                BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.fromNamespaceAndPath("minecraft","generic."+entry.attribute())).orElse(null);
            if(attribute==null) continue;
            var id=ResourceLocation.fromNamespaceAndPath("metallurgy",entry.id());
            event.addModifier(attribute,new AttributeModifier(id,entry.amount(),AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.bySlot(entry.slot()));'''+t[end:]
        elif name == 'effect/VersionBridge.java':
            start=t.index('    public static boolean knightStrike(');end=t.index('    public static void reviveCorpse(',start)
            t=t[:start]+'''    public static boolean knightStrike(Mob actor,Entity target) {
        if(!(target instanceof LivingEntity living) || !(actor.level() instanceof ServerLevel world)) return false;
        var weapon=actor.getMainHandItem();var source=actor.damageSources().mobAttack(actor);
        float damage=net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(world,weapon,living,source,(float)actor.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
        float knockback=net.minecraft.world.item.enchantment.EnchantmentHelper.modifyKnockback(world,weapon,living,source,0);
        if(!hit(living,source,damage)) return false;
        if(knockback>0) { double yaw=actor.getYRot()*Math.PI/180;living.knockback(knockback*0.5,Math.sin(yaw),-Math.cos(yaw));actor.setDeltaMovement(actor.getDeltaMovement().multiply(0.6,1,0.6)); }
        net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(world,living,source);actor.setLastHurtMob(living);return true;
    }
'''+t[end:]
            start=t.index('    public static void step(');end=t.index('    public static State stack(',start)
            t=t[:start]+'''    public static void step(Player player,boolean active) {
        if(active) { attribute(player,"step_height","quicksilver_step",0.4,0);try(State state=entity(player)) { state.flag("quicksilver_step_owned",true); } }
        else { removeAttribute(player,"step_height","quicksilver_step");try(State state=entity(player)) { state.remove("quicksilver_step_owned"); } }
    }
'''+t[end:]
            t=t.replace('registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key)', 'lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(key)')
            t=t.replace('net.minecraft.world.level.chunk.ChunkStatus', 'net.minecraft.world.level.chunk.status.ChunkStatus')
            t=t.replace('net.neoforged.neoforge.common.ForgeMod.SWIM_SPEED.get()', 'net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED')
            t=t.replace('getCapability(net.neoforged.neoforge.common.capabilities.ForgeCapabilities.ENERGY).orElse(null)', 'getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM)')
            t=t.replace('new AttributeModifier(id,"metallurgy:"+key,amount,AttributeModifier.Operation.ADDITION)', 'new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_VALUE)')
            t=t.replace('stack.getEnchantmentTags().size()', 'stack.getEnchantments().size()')
            t=t.replace('if(!stack.getOrCreateTag().contains("Unbreakable")) stack.getOrCreateTag().putBoolean("Unbreakable",true);', 'if(!stack.has(net.minecraft.core.component.DataComponents.UNBREAKABLE)) stack.set(net.minecraft.core.component.DataComponents.UNBREAKABLE,new net.minecraft.world.item.component.Unbreakable(true));')
            t=t.replace('stack.getFoodProperties(entity)', 'stack.get(net.minecraft.core.component.DataComponents.FOOD)').replace('food.getNutrition()', 'food.nutrition()')
            t=t.replace('/* Native1.20 item hooks read durability_boost. */', 'stack.set(net.minecraft.core.component.DataComponents.MAX_DAMAGE,maximum);')
            t = t.replace('ResourceLocation.fromNamespaceAndPath("forge",path)', 'ResourceLocation.fromNamespaceAndPath("c",path)')
            t = t.replace('entity.setSecondsOnFire(seconds)', 'entity.igniteForSeconds(seconds)')
            t = t.replace('.getAdvancements().getAdvancement(', '.getAdvancements().get(')
            t = t.replace('net.neoforged.neoforge.common.ForgeHooks.onBlockBreakEvent(player.level(),server.gameMode.getGameModeForPlayer(),server,pos)>=0', '!net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(player.level(),pos,player.level().getBlockState(pos),player)).isCanceled()')
            t = t.replace('net.minecraft.world.item.enchantment.EnchantmentHelper.getMobLooting(entity)', 'net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(entity.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),entity)')
            t = t.replace('new net.minecraft.world.SimpleContainer(input)', 'new net.minecraft.world.item.crafting.SingleRecipeInput(input)')
            t = t.replace('recipe.assemble(inventory,level.registryAccess())', 'recipe.value().assemble(inventory,level.registryAccess())')
            t = t.replace('import net.minecraft.world.entity.MobType;', '')
            t = t.replace('entity.getMobType()==MobType.UNDEAD', 'entity.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)')
            t = t.replace('owner->owner.broadcastBreakEvent(EquipmentSlot.MAINHAND)', 'EquipmentSlot.MAINHAND')
            t = t.replace('advancement.getCriteria().keySet()', 'advancement.value().criteria().keySet()')
            t = t.replace('return new Bag(stack.hasTag()?stack.getTag():new CompoundTag(),tag->stack.setTag(tag));', 'return new Bag(stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag(),tag->stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag)));')
            t = t.replace('BuiltInRegistries.MOB_EFFECT.get(', 'BuiltInRegistries.MOB_EFFECT.getHolder(')
            t = t.replace('var effect=BuiltInRegistries.MOB_EFFECT.getHolder(effectKey(id));', 'var effect=BuiltInRegistries.MOB_EFFECT.getHolder(effectKey(id)).orElse(null);')
            t = t.replace('var type=BuiltInRegistries.ATTRIBUTE.get(', 'var type=BuiltInRegistries.ATTRIBUTE.getHolder(')
            t = t.replace('ResourceLocation.fromNamespaceAndPath("minecraft","generic."+attribute));', 'ResourceLocation.fromNamespaceAndPath("minecraft","generic."+attribute)).orElse(null);')
            t = t.replace('UUID id=UUID.nameUUIDFromBytes(("metallurgy:"+key).getBytes(StandardCharsets.UTF_8));', 'ResourceLocation id=ResourceLocation.fromNamespaceAndPath("metallurgy",key);')
            t = t.replace('previous.getAmount()', 'previous.amount()')
            t = t.replace('new AttributeModifier(id,"metallurgy:"+key,amount,AttributeModifier.Operation.fromValue(operation))', 'new AttributeModifier(id,amount,switch(operation) {case 0->AttributeModifier.Operation.ADD_VALUE;case 1->AttributeModifier.Operation.ADD_MULTIPLIED_BASE;case 2->AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;default->throw new IllegalArgumentException("Attribute operation");})')
            t = t.replace('instance.removeModifier(UUID.nameUUIDFromBytes(("metallurgy:"+key).getBytes(StandardCharsets.UTF_8)))', 'instance.removeModifier(ResourceLocation.fromNamespaceAndPath("metallurgy",key))')
        elif name in ['machine/MachineEntity.java','machine/ChamberEntity.java']:
            t=t.replace('ItemStack.isSameItemSameTags', 'ItemStack.isSameItemSameComponents')
            t=re.sub(r'^import net.neoforged.neoforge.(common.capabilities.\*|common.util.LazyOptional|items.\*|items.wrapper.SidedInvWrapper);\n', '', t,flags=re.M)
            t=re.sub(r'^    private final LazyOptional<.*\n','',t,flags=re.M)
            start=t.index('    @Override public <T> LazyOptional<T>')
            t=t[:start]+'}\n'
            t=t.replace('@Override public void load(CompoundTag tag)', '@Override protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries)')
            t=t.replace('super.load(tag);','super.loadAdditional(tag,registries);').replace('ContainerHelper.loadAllItems(tag,items);','ContainerHelper.loadAllItems(tag,items,registries);')
            t=t.replace('saveAdditional(CompoundTag tag)', 'saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries)').replace('super.saveAdditional(tag);','super.saveAdditional(tag,registries);').replace('ContainerHelper.saveAllItems(tag,items);','ContainerHelper.saveAllItems(tag,items,registries);')
            t=t.replace('getUpdateTag() { return saveWithoutMetadata(); }', 'getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }')
        elif name == 'machine/ChamberBlock.java':
            at=t.index('    @Override protected void createBlockStateDefinition')
            t=t[:at]+'''    private static final com.mojang.serialization.MapCodec<ChamberBlock> CODEC=simpleCodec(ChamberBlock::new);
    @Override protected com.mojang.serialization.MapCodec<ChamberBlock> codec() { return CODEC; }
'''+t[at:]
        elif name == 'machine/ChamberBridge.java':
            t=t.replace('chamber.saveWithoutMetadata()', 'chamber.saveWithoutMetadata(chamber.getLevel().registryAccess())')
        elif name == 'machine/MachineBlock.java':
            t=t.replace('import net.neoforged.neoforge.network.NetworkHooks;','')
            t=t.replace('public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit)', 'protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit)')
            t=t.replace('NetworkHooks.openScreen(server,machine,','server.openMenu(machine,')
            at=t.index('    @Override protected void createBlockStateDefinition')
            t=t[:at]+'''    private static final com.mojang.serialization.MapCodec<MachineBlock> CODEC=com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance->instance.group(
        com.mojang.serialization.Codec.BOOL.fieldOf("alloy").forGetter(block->block.alloy),propertiesCodec()).apply(instance,MachineBlock::new));
    @Override protected com.mojang.serialization.MapCodec<MachineBlock> codec() { return CODEC; }
'''+t[at:]
        elif name == 'machine/MachineBridge.java':
            t=t.replace('net.neoforged.neoforge.common.ForgeHooks.getBurnTime(stack,net.minecraft.world.item.crafting.RecipeType.SMELTING)', 'stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING)')
            t=t.replace('net.neoforged.neoforge.event.ForgeEventFactory.firePlayerSmeltedEvent', 'net.neoforged.neoforge.event.EventHooks.firePlayerSmeltedEvent')
        elif name in ['recipe/ParagonRecipe.java','recipe/DetectorRecipe.java']:
            t=t.replace('public DetectorRecipe(ResourceLocation id,CraftingBookCategory category) { super(id,category); }', 'public DetectorRecipe(CraftingBookCategory category) { super(category); }')
            t=t.replace('public ParagonRecipe(ResourceLocation id,CraftingBookCategory category) { super(id,category); }', 'public ParagonRecipe(CraftingBookCategory category) { super(category); }')
            t=t.replace('import net.minecraft.world.inventory.CraftingContainer;', 'import net.minecraft.world.item.crafting.CraftingInput;')
            t=t.replace('CraftingContainer','CraftingInput').replace('RegistryAccess registries','net.minecraft.core.HolderLookup.Provider registries')
            t=t.replace('input.getContainerSize()', 'input.size()').replace('input.getWidth()', 'input.width()').replace('input.getHeight()', 'input.height()')
        elif name in ['client/ChestScreen.java','client/MachineScreen.java']:
            t = t.replace('renderBackground(graphics);super.render', 'renderBackground(graphics,x,y,partial);super.render')
        elif name == 'client/ClientSetup.java':
            t = t.replace('import net.neoforged.fml.common.Mod;', 'import net.neoforged.fml.common.EventBusSubscriber;')
            t = t.replace('@Mod.EventBusSubscriber', '@EventBusSubscriber').replace('Mod.EventBusSubscriber.Bus.MOD', 'EventBusSubscriber.Bus.MOD')
            t = t.replace('import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;', 'import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;')
            t = t.replace('setup(FMLClientSetupEvent event) { event.enqueueWork(() -> MenuScreens.register(Content.CHEST_MENU.get(),ChestScreen::new)); }', 'setup(RegisterMenuScreensEvent event) { event.register(Content.CHEST_MENU.get(),ChestScreen::new); }')
            t = t.replace('setup(FMLClientSetupEvent event) { event.enqueueWork(() -> { MenuScreens.register(Content.CHEST_MENU.get(),ChestScreen::new);MenuScreens.register(Content.MACHINE_MENU.get(),MachineScreen::new); }); }', 'setup(RegisterMenuScreensEvent event) { event.register(Content.CHEST_MENU.get(),ChestScreen::new);event.register(Content.MACHINE_MENU.get(),MachineScreen::new); }')
        out = DEST / relative
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(t, encoding='utf-8')


if __name__ == '__main__':
    main()
    from generate_gauntlet_tools import apply
    apply('neoforge-1.21.1')
    from generate_processing_adapters import generate121
    generate121()
    from generate_optional_pack_adapters import generate
    generate('neoforge-1.21.1')
