package it.hurts.metallurgy_reforged.effect;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.registries.ForgeRegistries;

public final class EffectEvents {
    @SubscribeEvent public static void track(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player && event.getTarget() instanceof LivingEntity entity) {
            try(State state=VersionBridge.entity(entity)) { int stage=state.integer("corpse_state");if(stage>0) it.hurts.metallurgy_reforged.network.ArmorNetwork.corpse(player,entity,stage); }
        }
    }
    @SubscribeEvent public static void armorJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if(!event.getEntity().level().isClientSide && event.getEntity() instanceof net.minecraft.world.entity.player.Player player) ArmorControls.jumped(player);
    }
    @SubscribeEvent public static void armorFall(net.minecraftforge.event.entity.living.LivingFallEvent event) {
        if(event.getEntity() instanceof net.minecraft.world.entity.player.Player player && ArmorControls.fall(player)) event.setCanceled(true);
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGH)
    public static void armorAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if(!event.getEntity().level().isClientSide && ArmorLifecycle.attack(event.getEntity(),event.getSource(),event.getAmount())) event.setCanceled(true);
    }
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if(event.getEntity().level().isClientSide) return;
        if(ArmorLifecycle.hurt(event.getEntity(),event.getSource(),event.getAmount()) || ArmorEffects.incoming(event.getEntity(),event.getSource(),event.getAmount())) { event.setCanceled(true);return; }
        AllEquipmentEffects.hurt(event.getEntity(),event.getAmount());
        float amount=WeaponDamage.beforeArmor(event.getEntity(),event.getSource(),AllEquipmentEffects.damage(event.getSource(),event.getAmount()));
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) { event.setAmount(amount);return; }
        ItemStack weapon = attacker.getMainHandItem();
        String id = ForgeRegistries.ITEMS.getKey(weapon.getItem()).toString();
        boolean weaponType = weapon.getItem() instanceof SwordItem || weapon.getItem() instanceof AxeItem;
        boolean amordrine = id.equals("metallurgy:amordrine_sword") || id.equals("metallurgy:amordrine_axe");
        boolean tartarite = (id.equals("metallurgy:tartarite_sword") || id.equals("metallurgy:tartarite_axe"))
            && weapon.hasTag() && "amordrine".equals(weapon.getTag().getString("paragon"));
        // True source alone also identifies an arrow's shooter. Requiring the
        // direct source and a native melee damage type closes issue388.
        boolean melee = event.getSource().getDirectEntity() == attacker
            && (event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK));
        event.setAmount(DivineGrace.damage(amount, event.getEntity().getHealth(),
            event.getEntity().getMaxHealth(), weaponType && (amordrine || tartarite) && EquipmentEffects.weapon(attacker).equals("amordrine"), melee));
        if(melee && weaponType && (amordrine || tartarite) && EquipmentEffects.weapon(attacker).equals("amordrine")) {
            float percentage=1-event.getEntity().getHealth()/event.getEntity().getMaxHealth();
            it.hurts.metallurgy_reforged.visual.EffectParticles.entity(event.getEntity(),"amordrine",25,1+percentage*2,true,(int)Math.floor(percentage*9));
        }
    }
    @SubscribeEvent public static void finalDamage(LivingDamageEvent event) {
        if(event.getEntity().level().isClientSide) return;
        ArmorEffects.damaged(event.getEntity(),event.getSource());
        var result=WeaponDamage.afterArmor(event.getEntity(),event.getSource(),event.getAmount());
        event.setAmount(result.amount());if(result.cancelled()) event.setCanceled(true);
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if(event.getEntity().level().isClientSide) return;
        if(ArmorEffects.preventDeath(event.getEntity())) { event.setCanceled(true);return; }
        CorpseLifecycle.death(event.getEntity(),event.getSource());
        ArmorEffects.death(event.getEntity(),event.getSource());
        WeaponDamage.death(event.getEntity(),event.getSource());
        AllEquipmentEffects.death(event.getEntity(),event.getSource());
        if(event.getEntity() instanceof net.minecraft.world.entity.player.Player player) { WeaponLifecycle.cleanup(player);ArmorLifecycle.cleanup(player);ToolEffects.cleanup(player);it.hurts.metallurgy_reforged.gadget.GadgetLogic.cleanup(player);it.hurts.metallurgy_reforged.gadget.ShieldLogic.cleanup(player); }
    }
    @SubscribeEvent public static void experience(LivingExperienceDropEvent event) { event.setDroppedExperience(WeaponDamage.experience(event.getEntity(),event.getAttackingPlayer(),event.getDroppedExperience())); }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) { if(event.phase==TickEvent.Phase.START) { WeaponLifecycle.tick(event.player);AllEquipmentEffects.tick(event.player);ArmorLifecycle.tick(event.player);ToolEffects.tick(event.player); } }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { WeaponLifecycle.cleanup(event.getEntity());ArmorLifecycle.cleanup(event.getEntity());ToolEffects.cleanup(event.getEntity());it.hurts.metallurgy_reforged.gadget.GadgetLogic.cleanup(event.getEntity());it.hurts.metallurgy_reforged.gadget.ShieldLogic.cleanup(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { WeaponLifecycle.cleanup(event.getEntity());ArmorLifecycle.cleanup(event.getEntity());ToolEffects.cleanup(event.getEntity());it.hurts.metallurgy_reforged.gadget.GadgetLogic.cleanup(event.getEntity());it.hurts.metallurgy_reforged.gadget.ShieldLogic.cleanup(event.getEntity()); }
    @SubscribeEvent public static void rightClick(PlayerInteractEvent.RightClickItem event) {
        boolean tool=ToolEffects.rightClick(event.getEntity(),event.getHand());
        if(WeaponLifecycle.rightClick(event.getEntity(),event.getHand()) || tool) { event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true); }
    }
    @SubscribeEvent public static void drops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        WeaponLoot.apply(event.getEntity(),event.getSource(),event.getDrops());
    }
    @SubscribeEvent public static void fuel(net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent event) {
        int ticks=switch(VersionBridge.itemId(event.getItemStack())) {
            case "metallurgy:tar"->800;case "metallurgy:charcoal_block"->16000;
            case "metallurgy:thermite_dust"->it.hurts.metallurgy_reforged.config.GadgetsConfig.ThermiteDust.thermiteFuelValue*200;case "metallurgy:ignatius_fuel"->it.hurts.metallurgy_reforged.config.GadgetsConfig.IgnatiusFuel.ignatiusFuelValue*200;default->-1;
        };
        if(ticks>=0) event.setBurnTime(ticks);
    }
    @SubscribeEvent public static void equipped(net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent event) {
        if(!event.getEntity().level().isClientSide) {AllEquipmentEffects.equipped(event.getEntity(),event.getSlot(),event.getTo());ToolEffects.equipped(event.getTo());it.hurts.metallurgy_reforged.gadget.GadgetLogic.equipped(event.getEntity(),event.getSlot(),event.getTo());}
    }
    @SubscribeEvent public static void food(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if(!event.getEntity().level().isClientSide) { AllEquipmentEffects.eat(event.getEntity(),event.getItem());ArmorLifecycle.drank(event.getEntity(),event.getItem()); }
    }
    @SubscribeEvent public static void attack(net.minecraftforge.event.entity.player.AttackEntityEvent event) { if(!event.getEntity().level().isClientSide) {AllEquipmentEffects.use(event.getEntity());it.hurts.metallurgy_reforged.gadget.GadgetLogic.attack(event.getEntity(),event.getTarget());} }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void breakBlock(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if(ToolEffects.breaking(event.getPlayer(),event.getPos(),event.getState())) {event.setCanceled(true);return;}
        event.setExpToDrop(ToolEffects.experience(event.getPlayer(),event.getState(),event.getExpToDrop()));
        AllEquipmentEffects.use(event.getPlayer());
    }
    @SubscribeEvent public static void hoe(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if(!event.getEntity().level().isClientSide && ToolEffects.crop(event.getEntity(),event.getHand(),event.getPos())) {event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);return;}
        if(!event.getEntity().level().isClientSide && event.getLevel().getBlockEntity(event.getPos()) instanceof it.hurts.metallurgy_reforged.machine.ChamberEntity chamber
            && chamber.insert(event.getEntity(),event.getHand(),event.getFace())) {
            event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);return;
        }
        if(!event.getEntity().level().isClientSide && EquipmentEffects.form(event.getItemStack()).equals("hoe")) AllEquipmentEffects.use(event.getEntity());
    }
    @SubscribeEvent public static void speed(PlayerEvent.BreakSpeed event) { event.setNewSpeed(ToolEffects.speed(event.getEntity(),event.getState(),event.getPosition().orElse(null),event.getOriginalSpeed(),AllEquipmentEffects.speed(event.getEntity(),event.getOriginalSpeed()))); }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGH)
    public static void soulbound(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if(event.getEntity() instanceof net.minecraft.world.entity.player.Player player) AllEquipmentEffects.playerDrops(player,event.getDrops());
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) { AllEquipmentEffects.clone(event.getOriginal(),event.getEntity(),event.isWasDeath()); }
    @SubscribeEvent public static void livingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) { ArmorEffects.tick(event.getEntity());it.hurts.metallurgy_reforged.gadget.GadgetLogic.livingTick(event.getEntity()); }
    @SubscribeEvent public static void visibility(net.minecraftforge.event.entity.living.LivingEvent.LivingVisibilityEvent event) {
        if(event.getEntity() instanceof net.minecraft.world.entity.player.Player) event.modifyVisibility(it.hurts.metallurgy_reforged.gadget.ShieldLogic.invisible(event.getEntity())?0:ArmorEffects.visibility(event.getEntity()));
    }
    @SubscribeEvent public static void target(net.minecraftforge.event.entity.living.LivingChangeTargetEvent event) {
        if(event.getEntity() instanceof net.minecraft.world.entity.Mob mob) {var target=ArmorEffects.target(mob,event.getNewTarget());event.setNewTarget(target!=null && it.hurts.metallurgy_reforged.gadget.ShieldLogic.invisible(target)?null:target);}
    }
    @SubscribeEvent public static void useTick(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Tick event) { ArmorEffects.useTick(event.getEntity(),event.getItem(),event.getDuration(),event::setDuration); }
    @SubscribeEvent public static void potionAdded(net.minecraftforge.event.entity.living.MobEffectEvent.Added event) { ArmorEffects.potions(event.getEntity(),event.getEffectInstance(),event.getOldEffectInstance()); }
    @SubscribeEvent public static void potionRemoved(net.minecraftforge.event.entity.living.MobEffectEvent.Remove event) { ArmorEffects.potions(event.getEntity(),null,event.getEffectInstance()); }
    @SubscribeEvent public static void potionExpired(net.minecraftforge.event.entity.living.MobEffectEvent.Expired event) { ArmorEffects.potions(event.getEntity(),null,event.getEffectInstance()); }
    @SubscribeEvent public static void harvestBoost(PlayerEvent.HarvestCheck event) {if(ToolBridge.boostedHarvest(event.getEntity(),event.getTargetBlock())) event.setCanHarvest(true);}
    @SubscribeEvent public static void gadgetTick(TickEvent.PlayerTickEvent event) { if(event.phase==TickEvent.Phase.START) {it.hurts.metallurgy_reforged.gadget.GadgetLogic.tick(event.player);it.hurts.metallurgy_reforged.gadget.ShieldLogic.tick(event.player);} }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void shield(net.minecraftforge.event.entity.living.ShieldBlockEvent event) {it.hurts.metallurgy_reforged.gadget.ShieldLogic.blocked(event.getEntity(),event.getDamageSource(),event.getBlockedDamage());}
    private EffectEvents() {}
}
