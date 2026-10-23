package it.hurts.metallurgy_reforged.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.entity.MobType;
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
    public static boolean client(Entity entity) { return entity.level().isClientSide; }
    public static String dimensionId(Level level) {return level.dimension().location().toString();}
    public static boolean knightStrike(Mob actor,Entity target) {
        if(!(target instanceof LivingEntity living)) return false;
        var weapon=actor.getMainHandItem();float damage=(float)actor.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
        damage+=net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(weapon,living.getMobType());
        int knockback=net.minecraft.world.item.enchantment.EnchantmentHelper.getKnockbackBonus(actor),fire=net.minecraft.world.item.enchantment.EnchantmentHelper.getFireAspect(actor);
        if(!hit(living,actor.damageSources().mobAttack(actor),damage)) return false;
        if(knockback>0) { double yaw=actor.getYRot()*Math.PI/180;living.knockback(knockback*0.5,Math.sin(yaw),-Math.cos(yaw));actor.setDeltaMovement(actor.getDeltaMovement().multiply(0.6,1,0.6)); }
        if(fire>0) burn(living,fire*4);
        net.minecraft.world.item.enchantment.EnchantmentHelper.doPostHurtEffects(living,actor);net.minecraft.world.item.enchantment.EnchantmentHelper.doPostDamageEffects(actor,living);actor.setLastHurtMob(living);return true;
    }
    public static void reviveCorpse(LivingEntity entity) { ((it.hurts.metallurgy_reforged.mixin.LivingAccess)entity).metallurgy$dead(false); }
    public static void corpseLoot(LivingEntity entity,DamageSource source) {
        if(entity.level() instanceof ServerLevel level && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT))
            ((it.hurts.metallurgy_reforged.mixin.LivingAccess)entity).metallurgy$loot(source,true);
    }
    public static void step(Player player,boolean active) {
        try(State state=entity(player)) {
            if(active) {
                if(!state.flag("quicksilver_step_owned")) { state.decimal("quicksilver_previous_step",player.maxUpStep());state.flag("quicksilver_step_owned",true); }
                player.setMaxUpStep(1);
            } else if(state.flag("quicksilver_step_owned")) {
                if(Math.abs(player.maxUpStep()-1)<1E-6) player.setMaxUpStep(state.decimal("quicksilver_previous_step"));
                state.remove("quicksilver_step_owned");state.remove("quicksilver_previous_step");
            }
        }
    }
    public static State stack(ItemStack stack) { return new Bag(stack.hasTag()?stack.getTag():new CompoundTag(),tag->stack.setTag(tag)); }
    public static State entity(Entity entity) {
        CompoundTag root=entity.getPersistentData();
        CompoundTag value=root.getCompound("metallurgy_effects");
        // Damage/effect hooks can nest while the first transaction is open.
        // Attach its live compound before callbacks can open another bag.
        root.put("metallurgy_effects",value);
        return new Bag(value,tag->root.put("metallurgy_effects",tag));
    }
    public static String itemId(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    public static String entityId(Entity entity) { return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(); }
    public static boolean hit(LivingEntity target,DamageSource source,float amount) { return target.hurt(source,amount); }
    public static ResourceLocation effectKey(String id) {return ResourceLocation.tryParse(id.indexOf(':')<0?"minecraft:"+id:id);}
    public static boolean registeredEffect(String id) {var key=effectKey(id);return key!=null && BuiltInRegistries.MOB_EFFECT.containsKey(key);}
    public static void buff(LivingEntity entity,String id,int ticks,int amplifier) {
        buff(entity,id,ticks,amplifier,false,true);
    }
    public static void buff(LivingEntity entity,String id,int ticks,int amplifier,boolean ambient,boolean visible) {
        var effect=BuiltInRegistries.MOB_EFFECT.get(effectKey(id));
        if(effect!=null) entity.addEffect(new MobEffectInstance(effect,ticks,Math.max(0,Math.min(255,amplifier)),ambient,visible));
    }
    public static int amplifier(LivingEntity entity,String id) {
        var effect=BuiltInRegistries.MOB_EFFECT.get(effectKey(id));
        var active=effect==null?null:entity.getEffect(effect);
        return active==null?-1:active.getAmplifier();
    }
    public static int duration(LivingEntity entity,String id) {
        var effect=BuiltInRegistries.MOB_EFFECT.get(effectKey(id));
        var active=effect==null?null:entity.getEffect(effect);return active==null?0:active.getDuration();
    }
    public static void removeBuff(LivingEntity entity,String id) {
        var effect=BuiltInRegistries.MOB_EFFECT.get(effectKey(id));
        if(effect!=null) entity.removeEffect(effect);
    }
    public static LivingEntity lastTarget(Player player) { return player.getLastHurtMob(); }
    public static boolean isArmor(ItemStack stack) { return stack.getItem() instanceof ArmorItem; }
    public static boolean undead(LivingEntity entity) { return entity.getMobType()==MobType.UNDEAD; }
    public static void burn(Entity entity,int seconds) { entity.setSecondsOnFire(seconds); }
    public static void drop(LivingEntity entity,ItemStack stack) { entity.spawnAtLocation(stack,entity.getBbHeight()/2); }
    public static float cooldown(Player player,ItemStack stack) { return player.getCooldowns().getCooldownPercent(stack.getItem(),0); }
    public static void cooldown(Player player,ItemStack stack,int ticks) { player.getCooldowns().addCooldown(stack.getItem(),ticks); }
    public static void damageHeld(LivingEntity entity,int damage) { entity.getMainHandItem().hurtAndBreak(damage,entity,owner->owner.broadcastBreakEvent(EquipmentSlot.MAINHAND)); }
    public static void damageStack(LivingEntity entity,ItemStack stack,int damage) { stack.hurtAndBreak(damage,entity,owner->owner.broadcastBreakEvent(EquipmentSlot.MAINHAND)); }
    public static int enchantmentCount(ItemStack stack) { return stack.getEnchantmentTags().size(); }
    public static void unbreakable(ItemStack stack) { if(!stack.getOrCreateTag().contains("Unbreakable")) stack.getOrCreateTag().putBoolean("Unbreakable",true); }
    public static int nutrition(ItemStack stack,LivingEntity entity) { var food=stack.getFoodProperties(entity);return food==null?-1:food.getNutrition(); }
    public static boolean keepInventory(Player player) { return player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY); }
    public static void setMaximumDamage(ItemStack stack,int maximum) { /* Native1.20 item hooks read durability_boost. */ }
    public static String blockId(net.minecraft.world.level.block.state.BlockState state) { return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(); }
    public static net.minecraft.world.level.block.Block registeredBlock(String namespace,String path) {
        var id=new ResourceLocation(namespace,path);return BuiltInRegistries.BLOCK.containsKey(id)?BuiltInRegistries.BLOCK.get(id):null;
    }
    public static String biomeId(net.minecraft.world.level.WorldGenLevel level,BlockPos pos) { return level.getBiome(pos).unwrapKey().map(key->key.location().toString()).orElse(""); }
    public static boolean oreRealm(net.minecraft.world.level.WorldGenLevel level,String realm) {
        var world=level.getLevel();var type=world.dimensionType();
        return it.hurts.metallurgy_reforged.world.OreRealms.realm(dimensionId(world),type.natural(),type.ultraWarm()).equals(realm);
    }
    public static boolean nearFortress(net.minecraft.world.level.WorldGenLevel world,int chunkX,int chunkZ) {
        BlockPos origin=new BlockPos(chunkX*16,world.getSeaLevel(),chunkZ*16);
        // Query prepared structure starts only. Locating/generating distant
        // chunks from a feature can recursively block world generation.
        for(int x=chunkX-6;x<=chunkX+6;x++) for(int z=chunkZ-6;z<=chunkZ+6;z++) {
            var chunk=world.getChunk(x,z,net.minecraft.world.level.chunk.ChunkStatus.STRUCTURE_STARTS,false);
            if(chunk==null) continue;
            for(var start:chunk.getAllStarts().values())
                if(start.isValid() && start.getStructure() instanceof net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure && start.getBoundingBox().getCenter().distSqr(origin)<8100) return true;
        }
        return false;
    }
    public static void useTick(LivingEntity entity,ItemStack stack,int duration) { stack.onUseTick(entity.level(),entity,duration); }
    public static boolean drink(ItemStack stack) { return stack.getUseAnimation()==net.minecraft.world.item.UseAnim.DRINK; }
    public static DamageSource waterDamage(LivingEntity entity) {
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,new ResourceLocation("metallurgy","water_damage"));
        return new DamageSource(entity.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key));
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
        java.util.List<net.minecraftforge.energy.IEnergyStorage> targets=new java.util.ArrayList<>();
        for(int i=0;i<36;i++) {
            var storage=player.getInventory().getItem(i).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).orElse(null);
            if(storage!=null && storage.canReceive()) targets.add(storage);
        }
        if(targets.isEmpty()) return;
        int each=(int)Math.ceil(energy/targets.size());for(var storage:targets) storage.receiveEnergy(each,false);
    }
    public static void privateSound(Player player,Vec3 pos,String id,float volume,float pitch) {
        var sound=BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation("minecraft",id));
        if(sound!=null && player instanceof ServerPlayer server && server.connection!=null)
            server.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(sound),SoundSource.PLAYERS,pos.x,pos.y,pos.z,volume,pitch,player.getRandom().nextLong()));
    }
    public static boolean itemTag(ItemStack stack,String path) { return stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,new ResourceLocation("forge",path))); }
    public static boolean day(Entity entity) { return entity.level().isDay(); }
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
            && net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(player.level(),server.gameMode.getGameModeForPlayer(),server,pos)>=0)
            player.level().destroyBlock(pos,true,player);
    }
    public static void directDamage(LivingEntity target,DamageSource source,float amount) {
        target.getCombatTracker().recordDamage(source,amount);
        target.setHealth(target.getHealth()-amount);target.setAbsorptionAmount(Math.max(0,target.getAbsorptionAmount()-amount));
        // The enclosing native hurt pipeline owns death dispatch after this hook.
    }
    public static int looting(LivingEntity entity) { return net.minecraft.world.item.enchantment.EnchantmentHelper.getMobLooting(entity); }
    public static ItemStack smelt(ServerLevel level,ItemStack input) {
        var inventory=new net.minecraft.world.SimpleContainer(input);
        return level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,inventory,level)
            .map(recipe->recipe.assemble(inventory,level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
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
        var sound=BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation("minecraft",id));
        if(sound!=null) entity.level().playSound(null,entity.blockPosition(),sound,SoundSource.PLAYERS,volume,pitch);
    }
    public static void award(LivingEntity entity,String path) {
        if(entity instanceof ServerPlayer player) it.hurts.metallurgy_reforged.advancement.Advancements.fire(player,path.substring(path.lastIndexOf('/')+1),0,"");
    }
    public static void attribute(LivingEntity entity,String attribute,String key,double amount,int operation) {
        if(attribute.equals("swim_speed")) { swimAttribute(entity,key,amount,false);return; }
        var type=BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation("minecraft","generic."+attribute));
        if(type==null) return;
        var instance=entity.getAttribute(type);
        if(instance==null) return;
        UUID id=UUID.nameUUIDFromBytes(("metallurgy:"+key).getBytes(StandardCharsets.UTF_8));
        var previous=instance.getModifier(id);
        if(previous!=null && previous.getAmount()==amount) return;
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id,"metallurgy:"+key,amount,AttributeModifier.Operation.fromValue(operation)));
    }
    public static void removeAttribute(LivingEntity entity,String attribute,String key) {
        if(attribute.equals("swim_speed")) { swimAttribute(entity,key,0,true);return; }
        var type=BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation("minecraft","generic."+attribute));
        var instance=type==null?null:entity.getAttribute(type);
        if(instance!=null) instance.removeModifier(UUID.nameUUIDFromBytes(("metallurgy:"+key).getBytes(StandardCharsets.UTF_8)));
    }
    private static void swimAttribute(LivingEntity entity,String key,double amount,boolean remove) {
        var instance=entity.getAttribute(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get());if(instance==null) return;
        UUID id=UUID.nameUUIDFromBytes(("metallurgy:"+key).getBytes(StandardCharsets.UTF_8));
        var previous=instance.getModifier(id);
        if(remove) { instance.removeModifier(id);return; }
        if(previous!=null && previous.getAmount()==amount) return;
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id,"metallurgy:"+key,amount,AttributeModifier.Operation.ADDITION));
    }
    private static final class Bag implements State {
        private final CompoundTag tag;
        private final Consumer<CompoundTag> save;
        private boolean dirty;
        Bag(CompoundTag tag,Consumer<CompoundTag> save) { this.tag=tag;this.save=save; }
        public String string(String key) { return tag.getString(key); }
        public int integer(String key) { return tag.getInt(key); }
        public long longValue(String key) { return tag.getLong(key); }
        public float decimal(String key) { return tag.getFloat(key); }
        public double doubleValue(String key) { return tag.getDouble(key); }
        public boolean flag(String key) { return tag.getBoolean(key); }
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
