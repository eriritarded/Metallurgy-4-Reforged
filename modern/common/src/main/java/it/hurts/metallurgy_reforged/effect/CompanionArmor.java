package it.hurts.metallurgy_reforged.effect;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.entity.PierKnight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import java.util.*;
public final class CompanionArmor {
    public static void damaged(LivingEntity owner,DamageSource source) {
        int level=EquipmentEffects.armorLevel(owner,"damascus_steel");
        if(level==0 || !(source.getEntity() instanceof LivingEntity attacker) || !(owner.level() instanceof ServerLevel world)) return;
        var randomPiece=owner.getItemBySlot(EquipmentEffects.ARMOR_SLOTS.get(owner.getRandom().nextInt(4)));
        if(owner instanceof Player player && VersionBridge.cooldown(player,randomPiece)!=0) return;
        try(State state=VersionBridge.entity(owner)) {
            try { if(world.getEntity(UUID.fromString(state.string("pier_uuid"))) instanceof PierKnight existing && existing.isAlive()) return; }catch(IllegalArgumentException ignored) {}
        }
        var knight=new PierKnight(Content.PIER_KNIGHT.get(),world);knight.summon(owner,attacker,level);knight.setPos(owner.position());
        if(world.addFreshEntity(knight)) {
            try(State state=VersionBridge.entity(owner)) { state.string("pier_uuid",knight.getUUID().toString()); }
            VersionBridge.award(owner,"challenges/loyal_friends");
        }
    }
    public static void released(PierKnight knight,LivingEntity owner) {
        if(owner==null) return;boolean owned=false;
        try(State state=VersionBridge.entity(owner)) { owned=state.string("pier_uuid").equals(knight.getUUID().toString());if(owned) state.remove("pier_uuid"); }
        if(owned && owner instanceof Player player) for(String form:List.of("helmet","chestplate","leggings","boots")) VersionBridge.cooldown(player,Content.stack("damascus_steel_"+form),200);
    }
    public static void cleanup(Player owner) {
        if(!(owner.level() instanceof ServerLevel world)) return;
        try(State state=VersionBridge.entity(owner)) {
            try { if(world.getEntity(UUID.fromString(state.string("pier_uuid"))) instanceof PierKnight knight) { knight.discard(); } }catch(IllegalArgumentException ignored) {}
            state.remove("pier_uuid");
        }
    }
    private CompanionArmor() {}
}
