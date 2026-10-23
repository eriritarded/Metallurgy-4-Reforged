package it.hurts.metallurgy_reforged.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.component.DataComponents;

import it.hurts.metallurgy_reforged.material.Materials;
import org.joml.Vector3f;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Target-native boundary: no client classes and no reflective Minecraft calls. */
public final class VersionBridge {
    public static boolean client(Entity entity) { return entity.level().isClientSide(); }
    public static String dimensionId(Level level) {return level.dimension().identifier().toString();}
    public static boolean knightStrike(Mob actor,Entity target) {
        if(!(target instanceof LivingEntity living) || !(actor.level() instanceof ServerLevel world)) return false;
        var weapon=actor.getMainHandItem();var source=actor.damageSources().mobAttack(actor);
        float damage=net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(world,weapon,living,source,(float)actor.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
        float knockback=net.minecraft.world.item.enchantment.EnchantmentHelper.modifyKnockback(world,weapon,living,source,0);
        if(!hit(living,source,damage)) return false;
        if(knockback>0) { double yaw=actor.getYRot()*Math.PI/180;living.knockback(knockback*0.5,Math.sin(yaw),-Math.cos(yaw));actor.setDeltaMovement(actor.getDeltaMovement().multiply(0.6,1,0.6)); }
        net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(world,living,source);actor.setLastHurtMob(living);return true;
    }
    public static void reviveCorpse(LivingEntity entity) { ((it.hurts.metallurgy_reforged.mixin.LivingAccess)entity).metallurgy$dead(false); }
    public static void corpseLoot(LivingEntity entity,DamageSource source) {
        if(entity.level() instanceof ServerLevel level && level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS))
            ((it.hurts.metallurgy_reforged.mixin.LivingAccess)entity).metallurgy$loot(level,source,true);
    }
    public static void step(Player player,boolean active) {
        if(active) { attribute(player,"step_height","quicksilver_step",0.4,0);try(State state=entity(player)) { state.flag("quicksilver_step_owned",true); } }
        else { removeAttribute(player,"step_height","quicksilver_step");try(State state=entity(player)) { state.remove("quicksilver_step_owned"); } }
    }
    public static State stack(ItemStack stack) { return new Bag(stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag(),tag->stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag))); }
    public static State entity(Entity entity) {
        CompoundTag root=entity.getPersistentData();
        CompoundTag value=root.getCompoundOrEmpty("metallurgy_effects");
        // Damage/effect hooks can nest while the first transaction is open.
        // Attach its live compound before callbacks can open another bag.
        root.put("metallurgy_effects",value);
        return new Bag(value,tag->root.put("metallurgy_effects",tag));
    }
    public static String itemId(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    public static String entityId(Entity entity) { return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(); }
    public static boolean hit(LivingEntity target,DamageSource source,float amount) { return target.level() instanceof net.minecraft.server.level.ServerLevel level && target.hurtServer(level,source,amount); }
    public static Identifier effectKey(String id) {return Identifier.tryParse(id.indexOf(':')<0?"minecraft:"+id:id);}
    public static boolean registeredEffect(String id) {var key=effectKey(id);return key!=null && BuiltInRegistries.MOB_EFFECT.containsKey(key);}
    public static void buff(LivingEntity entity,String id,int ticks,int amplifier) {
        buff(entity,id,ticks,amplifier,false,true);
    }
    public static void buff(LivingEntity entity,String id,int ticks,int amplifier,boolean ambient,boolean visible) {
        var effect=BuiltInRegistries.MOB_EFFECT.get(effectKey(id)).orElse(null);
        if(effect!=null) entity.addEffect(new MobEffectInstance(effect,ticks,Math.max(0,Math.min(255,amplifier)),ambient,visible));
    }
    public static int amplifier(LivingEntity entity,String id) {
        var effect=BuiltInRegistries.MOB_EFFECT.get(effectKey(id)).orElse(null);
        var active=effect==null?null:entity.getEffect(effect);
        return active==null?-1:active.getAmplifier();
    }
    public static int duration(LivingEntity entity,String id) {
        var effect=BuiltInRegistries.MOB_EFFECT.get(effectKey(id)).orElse(null);
        var active=effect==null?null:entity.getEffect(effect);return active==null?0:active.getDuration();
    }
    public static void removeBuff(LivingEntity entity,String id) {
        var effect=BuiltInRegistries.MOB_EFFECT.get(effectKey(id)).orElse(null);
        if(effect!=null) entity.removeEffect(effect);
    }
    public static LivingEntity lastTarget(Player player) { return player.getLastHurtMob(); }
    public static boolean isArmor(ItemStack stack) { return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).modifiers().stream().anyMatch(entry->entry.attribute().equals(Attributes.ARMOR)); }
    public static boolean undead(LivingEntity entity) { return entity.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.UNDEAD); }
    public static void burn(Entity entity,int seconds) { entity.igniteForSeconds(seconds); }
    public static void drop(LivingEntity entity,ItemStack stack) { entity.spawnAtLocation((net.minecraft.server.level.ServerLevel)entity.level(),stack,entity.getBbHeight()/2); }
    public static float cooldown(Player player,ItemStack stack) { return player.getCooldowns().getCooldownPercent(stack,0); }
    public static void cooldown(Player player,ItemStack stack,int ticks) { player.getCooldowns().addCooldown(stack,ticks); }
    public static void damageHeld(LivingEntity entity,int damage) { entity.getMainHandItem().hurtAndBreak(damage,entity,EquipmentSlot.MAINHAND); }
    public static void damageStack(LivingEntity entity,ItemStack stack,int damage) { stack.hurtAndBreak(damage,entity,EquipmentSlot.MAINHAND); }
    public static int enchantmentCount(ItemStack stack) { return stack.getEnchantments().size(); }
    public static void unbreakable(ItemStack stack) { if(!stack.has(net.minecraft.core.component.DataComponents.UNBREAKABLE)) stack.set(net.minecraft.core.component.DataComponents.UNBREAKABLE,net.minecraft.util.Unit.INSTANCE); }
    public static int nutrition(ItemStack stack,LivingEntity entity) { var food=stack.get(net.minecraft.core.component.DataComponents.FOOD);return food==null?-1:food.nutrition(); }
    public static boolean keepInventory(Player player) { return player.level() instanceof net.minecraft.server.level.ServerLevel level && level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY); }
    public static void setMaximumDamage(ItemStack stack,int maximum) { stack.set(net.minecraft.core.component.DataComponents.MAX_DAMAGE,maximum); }
    public static String blockId(net.minecraft.world.level.block.state.BlockState state) { return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(); }
    public static net.minecraft.world.level.block.Block registeredBlock(String namespace,String path) {
        var id=Identifier.fromNamespaceAndPath(namespace,path);return BuiltInRegistries.BLOCK.containsKey(id)?BuiltInRegistries.BLOCK.getValue(id):null;
    }
    public static String biomeId(net.minecraft.world.level.WorldGenLevel level,BlockPos pos) { return level.getBiome(pos).unwrapKey().map(key->key.identifier().toString()).orElse(""); }
    public static boolean oreRealm(net.minecraft.world.level.WorldGenLevel level,String realm) {
        var world=level.getLevel();var type=world.dimensionType();
        return it.hurts.metallurgy_reforged.world.OreRealms.realm(dimensionId(world),type.hasSkyLight() && type.skybox()==net.minecraft.world.level.dimension.DimensionType.Skybox.OVERWORLD,type.attributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES,net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES.defaultValue())).equals(realm);
    }
    public static boolean nearFortress(net.minecraft.world.level.WorldGenLevel world,int chunkX,int chunkZ) {
        BlockPos origin=new BlockPos(chunkX*16,world.getSeaLevel(),chunkZ*16);
        // Query prepared structure starts only. Locating/generating distant
        // chunks from a feature can recursively block world generation.
        for(int x=chunkX-6;x<=chunkX+6;x++) for(int z=chunkZ-6;z<=chunkZ+6;z++) {
            var chunk=world.getChunk(x,z,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS,false);
            if(chunk==null) continue;
            for(var start:chunk.getAllStarts().values())
                if(start.isValid() && start.getStructure() instanceof net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure && start.getBoundingBox().getCenter().distSqr(origin)<8100) return true;
        }
        return false;
    }
    public static void useTick(LivingEntity entity,ItemStack stack,int duration) { stack.onUseTick(entity.level(),entity,duration); }
    public static boolean drink(ItemStack stack) { return stack.getUseAnimation()==net.minecraft.world.item.ItemUseAnimation.DRINK; }
    public static DamageSource waterDamage(LivingEntity entity) {
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,Identifier.fromNamespaceAndPath("metallurgy","water_damage"));
        return new DamageSource(entity.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(key));
    }
    public static boolean bonemeal(Player player,BlockPos pos) {
        return player.mayBuild() && player.level().mayInteract(player,pos) && net.minecraft.world.item.BoneMealItem.applyBonemeal(new ItemStack(net.minecraft.world.item.Items.BONE_MEAL),player.level(),pos,player);
    }
    public static void armorExplosion(Player player,Vec3 pos,float power,boolean fire) {
        player.level().explode(player,pos.x,pos.y,pos.z,power,fire,Level.ExplosionInteraction.NONE);
    }
    public static void modSound(Entity entity,String path,float volume,float pitch) {
        var sound=it.hurts.metallurgy_reforged.content.Content.SOUND_MAP.get(path);
        if(sound!=null) entity.level().playSound(null,entity.blockPosition(),sound.get(),SoundSource.PLAYERS,volume,pitch);
    }
    public static void totem(LivingEntity entity,ItemStack piece) {
        if(entity.level() instanceof ServerLevel level) it.hurts.metallurgy_reforged.visual.EffectParticles.box(level,entity.getBoundingBox(),"adamantine",16,1,true,it.hurts.metallurgy_reforged.material.Materials.BY_NAME.get("adamantine").oreLevel()-2,null,40,true);
        it.hurts.metallurgy_reforged.network.VisualNetwork.activation(entity,piece);
        if(entity instanceof ServerPlayer player) net.minecraft.advancements.CriteriaTriggers.USED_TOTEM.trigger(player,piece);
    }
    public static void chargeInventory(Player player,float energy) {
        var targets=new java.util.ArrayList<net.neoforged.neoforge.transfer.energy.EnergyHandler>();
        for(int i=0;i<36;i++) {
            var access=net.neoforged.neoforge.transfer.access.ItemAccess.forPlayerSlot(player,i);
            var storage=access.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.ITEM);
            if(storage!=null) try(var transaction=net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                if(storage.insert(1,transaction)>0) targets.add(storage);
            }
        }
        if(targets.isEmpty()) return;
        int each=Math.max(0,(int)Math.ceil(energy/targets.size()));
        try(var transaction=net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            for(var storage:targets) storage.insert(each,transaction);transaction.commit();
        }
    }
    public static void privateSound(Player player,Vec3 pos,String id,float volume,float pitch) {
        var sound=BuiltInRegistries.SOUND_EVENT.getValue(Identifier.fromNamespaceAndPath("minecraft",id));
        if(sound!=null && player instanceof ServerPlayer server && server.connection!=null)
            server.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(sound),SoundSource.PLAYERS,pos.x,pos.y,pos.z,volume,pitch,player.getRandom().nextLong()));
    }
    public static boolean itemTag(ItemStack stack,String path) { return stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,Identifier.fromNamespaceAndPath("c",path))); }
    public static boolean day(Entity entity) { return entity.level().isBrightOutside(); }
    public static void velocity(Entity entity) {
        entity.hurtMarked=true;
        if(entity instanceof ServerPlayer player && player.connection!=null) player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(entity));
    }
    public static void recallFood(Player player,int foodDelta,float saturationDelta) {
        int food=Math.min(player.getFoodData().getFoodLevel()+foodDelta,20);
        float saturation=Math.max(0,Math.min(player.getFoodData().getSaturationLevel()+foodDelta*saturationDelta*2,food));
        player.getFoodData().setFoodLevel(food);player.getFoodData().setSaturation(saturation);
    }
    public static void explode(Entity source,Vec3 pos,float power) { explode(source,source,pos,power); }
    public static void explode(Entity source,Entity location,Vec3 pos,float power) {
        if(location!=null && location.level() instanceof ServerLevel level && power>0)
            level.explode(source,pos.x,pos.y,pos.z,power,Level.ExplosionInteraction.NONE);
    }
    public static void breakBlock(Player player,BlockPos pos) {
        if(player instanceof ServerPlayer server && player.mayBuild() && player.level().mayInteract(player,pos)
            && !net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(player.level(),server.gameMode.getGameModeForPlayer(),server,pos,player.level().getBlockState(pos)).isCanceled())
            player.level().destroyBlock(pos,true,player);
    }
    public static void directDamage(LivingEntity target,DamageSource source,float amount) {
        target.getCombatTracker().recordDamage(source,amount);
        target.setHealth(target.getHealth()-amount);target.setAbsorptionAmount(Math.max(0,target.getAbsorptionAmount()-amount));
        // The enclosing native hurt pipeline owns death dispatch after this hook.
    }
    public static int looting(LivingEntity entity) { return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(entity.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),entity); }
    public static ItemStack smelt(ServerLevel level,ItemStack input) {
        var inventory=new net.minecraft.world.item.crafting.SingleRecipeInput(input);
        return level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,inventory,level)
            .map(recipe->recipe.value().assemble(inventory).copy()).orElse(ItemStack.EMPTY);
    }
    public static void particleAt(ServerLevel level,Vec3 pos,String metal,float scale) {
        var material=Materials.BY_NAME.get(metal);if(material==null) return;
        it.hurts.metallurgy_reforged.network.VisualNetwork.burst(level,new it.hurts.metallurgy_reforged.visual.ParticleBurst(pos.x,pos.y,pos.z,0,0,0,material.color(),scale,1,5,true,java.util.concurrent.ThreadLocalRandom.current().nextLong()));
    }
    public static void particles(Entity entity,String metal,int count,float scale) {
        if(!(entity.level() instanceof ServerLevel level) || scale<=0) return;
        Materials.Definition material=Materials.BY_NAME.get(metal);
        if(material==null) return;
        it.hurts.metallurgy_reforged.network.VisualNetwork.burst(level,new it.hurts.metallurgy_reforged.visual.ParticleBurst(entity.getX(),entity.getY()+entity.getBbHeight()/2,entity.getZ(),entity.getBbWidth()/2,entity.getBbHeight()/2,entity.getBbWidth()/2,material.color(),scale,count,5,true,java.util.concurrent.ThreadLocalRandom.current().nextLong()));
    }
    public static void sound(Entity entity,String id,float volume,float pitch) {
        var sound=BuiltInRegistries.SOUND_EVENT.getValue(Identifier.fromNamespaceAndPath("minecraft",id));
        if(sound!=null) entity.level().playSound(null,entity.blockPosition(),sound,SoundSource.PLAYERS,volume,pitch);
    }
    public static void award(LivingEntity entity,String path) {
        if(entity instanceof ServerPlayer player) it.hurts.metallurgy_reforged.advancement.Advancements.fire(player,path.substring(path.lastIndexOf('/')+1),0,"");
    }
    public static void attribute(LivingEntity entity,String attribute,String key,double amount,int operation) {
        if(attribute.equals("swim_speed")) { swimAttribute(entity,key,amount,false);return; }
        var type=BuiltInRegistries.ATTRIBUTE.get(Identifier.fromNamespaceAndPath("minecraft",attribute)).orElse(null);
        if(type==null) return;
        var instance=entity.getAttribute(type);
        if(instance==null) return;
        Identifier id=Identifier.fromNamespaceAndPath("metallurgy",key);
        var previous=instance.getModifier(id);
        if(previous!=null && previous.amount()==amount) return;
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id,amount,switch(operation) {case 0->AttributeModifier.Operation.ADD_VALUE;case 1->AttributeModifier.Operation.ADD_MULTIPLIED_BASE;case 2->AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;default->throw new IllegalArgumentException("Attribute operation");}));
    }
    public static void removeAttribute(LivingEntity entity,String attribute,String key) {
        if(attribute.equals("swim_speed")) { swimAttribute(entity,key,0,true);return; }
        var type=BuiltInRegistries.ATTRIBUTE.get(Identifier.fromNamespaceAndPath("minecraft",attribute)).orElse(null);
        var instance=type==null?null:entity.getAttribute(type);
        if(instance!=null) instance.removeModifier(Identifier.fromNamespaceAndPath("metallurgy",key));
    }
    private static void swimAttribute(LivingEntity entity,String key,double amount,boolean remove) {
        var instance=entity.getAttribute(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED);if(instance==null) return;
        Identifier id=Identifier.fromNamespaceAndPath("metallurgy",key);
        var previous=instance.getModifier(id);
        if(remove) { instance.removeModifier(id);return; }
        if(previous!=null && previous.amount()==amount) return;
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_VALUE));
    }
    private static final class Bag implements State {
        private final CompoundTag tag;
        private final Consumer<CompoundTag> save;
        private boolean dirty;
        Bag(CompoundTag tag,Consumer<CompoundTag> save) { this.tag=tag;this.save=save; }
        public String string(String key) { return tag.getStringOr(key,""); }
        public int integer(String key) { return tag.getIntOr(key,0); }
        public long longValue(String key) { return tag.getLongOr(key,0); }
        public float decimal(String key) { return tag.getFloatOr(key,0); }
        public double doubleValue(String key) { return tag.getDoubleOr(key,0); }
        public boolean flag(String key) { return tag.getBooleanOr(key,false); }
        public boolean has(String key) { return tag.contains(key); }
        public void string(String key,String value) { tag.putString(key,value);dirty=true; }
        public void integer(String key,int value) { tag.putInt(key,value);dirty=true; }
        public void longValue(String key,long value) { tag.putLong(key,value);dirty=true; }
        public void decimal(String key,float value) { tag.putFloat(key,value);dirty=true; }
        public void doubleValue(String key,double value) { tag.putDouble(key,value);dirty=true; }
        public void flag(String key,boolean value) { tag.putBoolean(key,value);dirty=true; }
        public void remove(String key) { tag.remove(key);dirty=true; }
        public void close() { if(dirty) save.accept(tag); }
    }
    private VersionBridge() {}
}
