package it.hurts.metallurgy_reforged.effect;
import it.hurts.metallurgy_reforged.network.ArmorNetwork;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
/** Sanguinite keeps the actual slain mob and its AI, rather than spawning a
 * generic replacement. Native player respawn remains owned by Minecraft. */
public final class CorpseLifecycle {
    public static boolean deathTick(LivingEntity entity) {
        if(!(entity instanceof Mob)) return false;
        int stage;
        try(State state=VersionBridge.entity(entity)) { stage=state.integer("corpse_state"); }
        if(stage==2) return false;
        if(stage==0) {
            if(VersionBridge.client(entity) || entity.deathTime!=16 || !(entity.getKillCredit() instanceof Player owner) || EquipmentEffects.armorLevel(owner,"sanguinite")==0) return false;
            try(State state=VersionBridge.entity(entity)) { state.integer("corpse_state",1); }
            entity.tickCount=0;ArmorNetwork.corpse(entity,1);return true;
        }
        if(entity.tickCount<20) entity.deathTime=18;
        else if(entity.deathTime>=0) entity.deathTime-=2;
        else {
            entity.setHealth(entity.getMaxHealth()*0.4F);entity.deathTime=0;VersionBridge.reviveCorpse(entity);
            try(State state=VersionBridge.entity(entity)) { state.integer("corpse_state",2); }
            if(!VersionBridge.client(entity)) ArmorNetwork.corpse(entity,2);
        }
        return true;
    }
    public static void death(LivingEntity entity,DamageSource source) {
        if(!(entity instanceof Mob)) return;
        try(State state=VersionBridge.entity(entity)) { if(state.integer("corpse_state")==2) VersionBridge.corpseLoot(entity,source); }
    }
    public static void applyClient(Player viewer,int entityId,int stage) {
        if(stage<1 || stage>2) return;
        Entity entity=viewer.level().getEntity(entityId);
        if(entity instanceof LivingEntity living) {
            try(State state=VersionBridge.entity(entity)) { state.integer("corpse_state",stage); }
            entity.tickCount=0;living.deathTime=stage==1?18:0;
            if(stage==2) VersionBridge.reviveCorpse(living);
        }
    }
    private CorpseLifecycle() {}
}
