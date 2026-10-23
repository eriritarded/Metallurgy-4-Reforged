package it.hurts.metallurgy_reforged.effect;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.registries.BuiltInRegistries;

public final class EffectEvents {
    @SubscribeEvent public static void track(net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking event) {
        if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player && event.getTarget() instanceof LivingEntity entity) {
            try(State state=VersionBridge.entity(entity)) { int stage=state.integer("corpse_state");if(stage>0) it.hurts.metallurgy_reforged.network.ArmorNetwork.corpse(player,entity,stage); }
        }
    }
    @SubscribeEvent public static void armorJump(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if(!event.getEntity().level().isClientSide() && event.getEntity() instanceof net.minecraft.world.entity.player.Player player) ArmorControls.jumped(player);
    }
    @SubscribeEvent public static void armorFall(net.neoforged.neoforge.event.entity.living.LivingFallEvent event) {
        if(event.getEntity() instanceof net.minecraft.world.entity.player.Player player && ArmorControls.fall(player)) event.setCanceled(true);
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGH)
    public static void armorAttack(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if(!event.getEntity().level().isClientSide() && ArmorLifecycle.attack(event.getEntity(),event.getSource(),event.getAmount())) event.setCanceled(true);
    }
    @SubscribeEvent
    public static void hurt(LivingIncomingDamageEvent event) {
        if(event.getEntity().level().isClientSide()) return;
        if(ArmorLifecycle.hurt(event.getEntity(),event.getSource(),event.getAmount()) || ArmorEffects.incoming(event.getEntity(),event.getSource(),event.getAmount())) { event.setCanceled(true);return; }
        AllEquipmentEffects.hurt(event.getEntity(),event.getAmount());
        float amount=WeaponDamage.beforeArmor(event.getEntity(),event.getSource(),AllEquipmentEffects.damage(event.getSource(),event.getAmount()));
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) { event.setAmount(amount);return; }
        ItemStack weapon = attacker.getMainHandItem();
        String id = BuiltInRegistries.ITEM.getKey(weapon.getItem()).toString();
        boolean weaponType = id.endsWith("_sword") || id.endsWith("_axe");
        boolean amordrine = id.equals("metallurgy:amordrine_sword") || id.equals("metallurgy:amordrine_axe");
        boolean tartarite = (id.equals("metallurgy:tartarite_sword") || id.equals("metallurgy:tartarite_axe"))
            && "amordrine".equals(weapon.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getStringOr("paragon",""));
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
    @SubscribeEvent public static void finalDamage(LivingDamageEvent.Pre event) {
        if(event.getEntity().level().isClientSide()) return;
        ArmorEffects.damaged(event.getEntity(),event.getSource());
        var result=WeaponDamage.afterArmor(event.getEntity(),event.getSource(),event.getNewDamage());
        event.setNewDamage(result.cancelled()?0:result.amount());
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if(event.getEntity().level().isClientSide()) return;
        if(ArmorEffects.preventDeath(event.getEntity())) { event.setCanceled(true);return; }
        CorpseLifecycle.death(event.getEntity(),event.getSource());
        ArmorEffects.death(event.getEntity(),event.getSource());
        WeaponDamage.death(event.getEntity(),event.getSource());
        AllEquipmentEffects.death(event.getEntity(),event.getSource());
        if(event.getEntity() instanceof net.minecraft.world.entity.player.Player player) { WeaponLifecycle.cleanup(player);ArmorLifecycle.cleanup(player);ToolEffects.cleanup(player);it.hurts.metallurgy_reforged.gadget.GadgetLogic.cleanup(player);it.hurts.metallurgy_reforged.gadget.ShieldLogic.cleanup(player); }
    }
    @SubscribeEvent public static void experience(LivingExperienceDropEvent event) { event.setDroppedExperience(WeaponDamage.experience(event.getEntity(),event.getAttackingPlayer(),event.getDroppedExperience())); }
    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) { WeaponLifecycle.tick(event.getEntity());AllEquipmentEffects.tick(event.getEntity());ArmorLifecycle.tick(event.getEntity());ToolEffects.tick(event.getEntity()); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { WeaponLifecycle.cleanup(event.getEntity());ArmorLifecycle.cleanup(event.getEntity());ToolEffects.cleanup(event.getEntity());it.hurts.metallurgy_reforged.gadget.GadgetLogic.cleanup(event.getEntity());it.hurts.metallurgy_reforged.gadget.ShieldLogic.cleanup(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { WeaponLifecycle.cleanup(event.getEntity());ArmorLifecycle.cleanup(event.getEntity());ToolEffects.cleanup(event.getEntity());it.hurts.metallurgy_reforged.gadget.GadgetLogic.cleanup(event.getEntity());it.hurts.metallurgy_reforged.gadget.ShieldLogic.cleanup(event.getEntity()); }
    @SubscribeEvent public static void rightClick(PlayerInteractEvent.RightClickItem event) {
        boolean tool=ToolEffects.rightClick(event.getEntity(),event.getHand());
        if(WeaponLifecycle.rightClick(event.getEntity(),event.getHand()) || tool) { event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true); }
    }
    @SubscribeEvent public static void drops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        WeaponLoot.apply(event.getEntity(),event.getSource(),event.getDrops());
    }
    @SubscribeEvent public static void fuel(net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent event) {
        int ticks=switch(VersionBridge.itemId(event.getItemStack())) {
            case "metallurgy:tar"->800;case "metallurgy:charcoal_block"->16000;
            case "metallurgy:thermite_dust"->it.hurts.metallurgy_reforged.config.GadgetsConfig.ThermiteDust.thermiteFuelValue*200;case "metallurgy:ignatius_fuel"->it.hurts.metallurgy_reforged.config.GadgetsConfig.IgnatiusFuel.ignatiusFuelValue*200;default->-1;
        };
        if(ticks>=0) event.setBurnTime(ticks);
    }
    @SubscribeEvent public static void equipped(net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent event) {
        if(!event.getEntity().level().isClientSide()) {AllEquipmentEffects.equipped(event.getEntity(),event.getSlot(),event.getTo());ToolEffects.equipped(event.getTo());it.hurts.metallurgy_reforged.gadget.GadgetLogic.equipped(event.getEntity(),event.getSlot(),event.getTo());}
    }
    @SubscribeEvent public static void food(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if(!event.getEntity().level().isClientSide()) { AllEquipmentEffects.eat(event.getEntity(),event.getItem());ArmorLifecycle.drank(event.getEntity(),event.getItem()); }
    }
    @SubscribeEvent public static void attack(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) { if(!event.getEntity().level().isClientSide()) {AllEquipmentEffects.use(event.getEntity());it.hurts.metallurgy_reforged.gadget.GadgetLogic.attack(event.getEntity(),event.getTarget());} }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public static void breakBlock(net.neoforged.neoforge.event.level.block.BreakBlockEvent event) {
        if(ToolEffects.breaking(event.getPlayer(),event.getPos(),event.getState())) {event.setCanceled(true);return;}

        AllEquipmentEffects.use(event.getPlayer());
    }
    @SubscribeEvent public static void hoe(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if(!event.getEntity().level().isClientSide() && ToolEffects.crop(event.getEntity(),event.getHand(),event.getPos())) {event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);return;}
        if(!event.getEntity().level().isClientSide() && event.getLevel().getBlockEntity(event.getPos()) instanceof it.hurts.metallurgy_reforged.machine.ChamberEntity chamber
            && chamber.insert(event.getEntity(),event.getHand(),event.getFace())) {
            event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);return;
        }
        if(!event.getEntity().level().isClientSide() && EquipmentEffects.form(event.getItemStack()).equals("hoe")) AllEquipmentEffects.use(event.getEntity());
    }
    @SubscribeEvent public static void speed(PlayerEvent.BreakSpeed event) { event.setNewSpeed(ToolEffects.speed(event.getEntity(),event.getState(),event.getPosition().orElse(null),event.getOriginalSpeed(),AllEquipmentEffects.speed(event.getEntity(),event.getOriginalSpeed()))); }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGH)
    public static void soulbound(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        if(event.getEntity() instanceof net.minecraft.world.entity.player.Player player) AllEquipmentEffects.playerDrops(player,event.getDrops());
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) { AllEquipmentEffects.clone(event.getOriginal(),event.getEntity(),event.isWasDeath()); }
    @SubscribeEvent public static void livingTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) { if(event.getEntity() instanceof LivingEntity entity) {ArmorEffects.tick(entity);it.hurts.metallurgy_reforged.gadget.GadgetLogic.livingTick(entity);} }
    @SubscribeEvent public static void visibility(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingVisibilityEvent event) {
        if(event.getEntity() instanceof net.minecraft.world.entity.player.Player) event.modifyVisibility(it.hurts.metallurgy_reforged.gadget.ShieldLogic.invisible(event.getEntity())?0:ArmorEffects.visibility(event.getEntity()));
    }
    @SubscribeEvent public static void target(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        if(event.getEntity() instanceof net.minecraft.world.entity.Mob mob) {var target=ArmorEffects.target(mob,event.getNewAboutToBeSetTarget());event.setNewAboutToBeSetTarget(target!=null && it.hurts.metallurgy_reforged.gadget.ShieldLogic.invisible(target)?null:target);}
    }
    @SubscribeEvent public static void useTick(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Tick event) { ArmorEffects.useTick(event.getEntity(),event.getItem(),event.getDuration(),event::setDuration); }
    @SubscribeEvent public static void potionAdded(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added event) { ArmorEffects.potions(event.getEntity(),event.getEffectInstance(),event.getOldEffectInstance()); }
    @SubscribeEvent public static void potionRemoved(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove event) { ArmorEffects.potions(event.getEntity(),null,event.getEffectInstance()); }
    @SubscribeEvent public static void potionExpired(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired event) { ArmorEffects.potions(event.getEntity(),null,event.getEffectInstance()); }
    @SubscribeEvent public static void harvestBoost(PlayerEvent.HarvestCheck event) {if(ToolBridge.boostedHarvest(event.getEntity(),event.getTargetBlock())) event.setCanHarvest(true);}
    @SubscribeEvent public static void gadgetTick(PlayerTickEvent.Pre event) {it.hurts.metallurgy_reforged.gadget.GadgetLogic.tick(event.getEntity());it.hurts.metallurgy_reforged.gadget.ShieldLogic.tick(event.getEntity());}
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public static void shield(net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent event) {if(event.getBlocked()) it.hurts.metallurgy_reforged.gadget.ShieldLogic.blocked(event.getEntity(),event.getDamageSource(),event.getBlockedDamage());}
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
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
    private EffectEvents() {}
}
